package ua.readmania.common;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global REST exception handler producing RFC 9457 Problem Details responses.
 *
 * Implements requirements:
 * - R3: Informative and structured error responses across all endpoints.
 * - U3: Validation errors formatted as errors[{ field, message }].
 * - Status codes: 400, 401, 403, 404, 409, 500.
 * - Custom domain exceptions: NotFoundException (404), ConflictException (409), BusinessRuleException (400/409).
 * - Data conflicts: ObjectOptimisticLockingFailureException & DataIntegrityViolationException (409).
 * - Parameter validation: ConstraintViolationException & MethodArgumentTypeMismatchException (400).
 * - Safety net (500) with unique errorId in logs and response without sensitive data leakage.
 * - Forwarding Spring Security exceptions (AccessDeniedException / AuthenticationException) to security layer.
 * - SPA compatibility: non-API unknown routes forward to index.html; JSON-404 only for /api/**.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // Custom domain exceptions
    // =========================================================================

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Ресурс не знайдено");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Конфлікт даних");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(status == HttpStatus.CONFLICT ? "Конфлікт бізнес-правил" : "Порушення бізнес-правил");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    // =========================================================================
    // Concurrency and Data Integrity conflicts (409)
    // =========================================================================

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail handleOptimisticLockingFailure(Exception ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Дані були змінені іншим користувачем. Будь ласка, оновіть сторінку та спробуйте знову."
        );
        problemDetail.setTitle("Конфлікт одночасного редагування");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Операція не може бути виконана через порушення унікальності або цілісності даних."
        );
        problemDetail.setTitle("Конфлікт цілісності даних");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    // =========================================================================
    // Parameter validation (400)
    // =========================================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();
                    int lastDot = field.lastIndexOf('.');
                    if (lastDot >= 0 && lastDot < field.length() - 1) {
                        field = field.substring(lastDot + 1);
                    }
                    return new ValidationError(field, violation.getMessage());
                })
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Параметри запиту не пройшли валідацію"
        );
        problemDetail.setTitle("Помилка валідації");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "невідомий тип";
        String detail = String.format("Параметр '%s' має некоректний тип. Очікується: %s", ex.getName(), requiredType);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Некоректний тип параметра");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    // =========================================================================
    // Overrides for Spring MVC ResponseEntityExceptionHandler
    // =========================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Перевірте правильність заповнення полів"
        );
        problemDetail.setTitle("Помилка валідації");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        problemDetail.setProperty("errors", errors);

        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        ex.getValueResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                String message = error.getDefaultMessage();
                errors.add(new ValidationError(paramName != null ? paramName : "parameter", message));
            });
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Некоректні параметри запиту"
        );
        problemDetail.setTitle("Помилка валідації");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        problemDetail.setProperty("errors", errors);

        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            String uri = httpRequest.getRequestURI();
            if (!uri.startsWith("/api/")) {
                try {
                    HttpServletResponse httpResponse = servletWebRequest.getResponse();
                    if (httpResponse != null && !httpResponse.isCommitted()) {
                        httpRequest.getRequestDispatcher("/index.html").forward(httpRequest, httpResponse);
                        return null;
                    }
                } catch (Exception ignored) {
                    // Fall back to ProblemDetail 404
                }
            }
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Запитуваний ресурс не знайдено"
        );
        problemDetail.setTitle("Ресурс не знайдено");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.NOT_FOUND, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                String.format("HTTP-метод '%s' не підтримується для цього ресурсу", ex.getMethod())
        );
        problemDetail.setTitle("Метод не підтримується");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Тіло запиту відсутнє або містить некоректний JSON"
        );
        problemDetail.setTitle("Некоректне тіло запиту");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("Відсутній обов'язковий параметр запиту: '%s'", ex.getParameterName())
        );
        problemDetail.setTitle("Відсутній обов'язковий параметр");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                String.format("Тип контенту '%s' не підтримується", ex.getContentType())
        );
        problemDetail.setTitle("Непідтримуваний тип медіа");
        if (request instanceof ServletWebRequest servletWebRequest) {
            problemDetail.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return handleExceptionInternal(ex, problemDetail, headers, HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    // =========================================================================
    // Safety net (500 Internal Server Error)
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex, HttpServletRequest request) throws Exception {
        // Pass AccessDeniedException and AuthenticationException to Spring Security
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof AccessDeniedException accessDeniedException) {
                throw accessDeniedException;
            }
            if (cause instanceof AuthenticationException authenticationException) {
                throw authenticationException;
            }
            cause = cause.getCause();
        }

        String errorId = UUID.randomUUID().toString();
        log.error("Internal Server Error [errorId={}] on {}: {}", errorId, request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Виникла непередбачена помилка. Зверніться до адміністратора та вкажіть ідентифікатор помилки: " + errorId
        );
        problemDetail.setTitle("Внутрішня помилка сервера");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("errorId", errorId);

        return problemDetail;
    }
}
