package ua.readmania.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.TestControllerConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    // =========================================================================
    // Test DTOs and Controller
    // =========================================================================

    public record TestValidationDto(
            @NotBlank(message = "Поле обов'язкове. Приклад: Тарас Шевченко")
            String name,

            @Email(message = "Невірний формат email. Приклад: name@example.com")
            String email,

            @Min(value = 1, message = "Кількість має бути не менше 1. Рекомендація: вкажіть додатне число")
            int quantity
    ) {
    }

    @TestConfiguration
    static class TestControllerConfig {

        @RestController
        @Validated
        @RequestMapping("/api/catalog/test-exceptions")
        static class TestExceptionController {

            @GetMapping("/not-found")
            public void throwNotFound() {
                throw new NotFoundException("Книгу з ідентифікатором 999 не знайдено");
            }

            @GetMapping("/conflict")
            public void throwConflict() {
                throw new ConflictException("Замовлення вже обробляється іншим оператором");
            }

            @GetMapping("/business-rule-default")
            public void throwBusinessRuleDefault() {
                throw new BusinessRuleException("Неможливо скасувати замовлення у статусі DELIVERED");
            }

            @GetMapping("/business-rule-conflict")
            public void throwBusinessRuleConflict() {
                throw new BusinessRuleException("Товар закінчився на складі під час оформлення", HttpStatus.CONFLICT);
            }

            @GetMapping("/optimistic-lock")
            public void throwOptimisticLock() {
                throw new ObjectOptimisticLockingFailureException("Book", 123);
            }

            @GetMapping("/data-integrity")
            public void throwDataIntegrity() {
                throw new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint 'uk_books_isbn'");
            }

            @PostMapping("/validation")
            public String validateDto(@Valid @RequestBody TestValidationDto dto) {
                return "ok";
            }

            @GetMapping("/param-validation/{id}")
            public String validateParam(@PathVariable("id") @Min(value = 1, message = "ID має бути додатним числом") Long id) {
                return "id: " + id;
            }

            @GetMapping("/type-mismatch")
            public String typeMismatch(@RequestParam("number") Long number) {
                return "number: " + number;
            }

            @GetMapping("/missing-param")
            public String missingParam(@RequestParam("requiredParam") String requiredParam) {
                return "param: " + requiredParam;
            }

            @GetMapping("/access-denied")
            public void throwAccessDenied() {
                throw new AccessDeniedException("Недостатньо прав для виконання операції");
            }

            @GetMapping("/unhandled-server-error")
            public void throwUnhandled() {
                throw new NullPointerException("Simulated unexpected null pointer exception with internal details");
            }

            @GetMapping("/get-only")
            public String getOnlyEndpoint() {
                return "get-success";
            }
        }
    }

    // =========================================================================
    // Tests: Domain Exceptions
    // =========================================================================

    @Nested
    @DisplayName("Domain Exceptions (NotFoundException, ConflictException, BusinessRuleException)")
    class DomainExceptionsTests {

        @Test
        @DisplayName("NotFoundException returns 404 with Problem Details")
        void notFoundExceptionReturns404() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Ресурс не знайдено")))
                    .andExpect(jsonPath("$.detail", is("Книгу з ідентифікатором 999 не знайдено")))
                    .andExpect(jsonPath("$.instance", is("/api/catalog/test-exceptions/not-found")));
        }

        @Test
        @DisplayName("ConflictException returns 409 with Problem Details")
        void conflictExceptionReturns409() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.title", is("Конфлікт даних")))
                    .andExpect(jsonPath("$.detail", is("Замовлення вже обробляється іншим оператором")))
                    .andExpect(jsonPath("$.instance", is("/api/catalog/test-exceptions/conflict")));
        }

        @Test
        @DisplayName("BusinessRuleException (default) returns 400 Bad Request")
        void businessRuleExceptionReturns400() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/business-rule-default"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Порушення бізнес-правил")))
                    .andExpect(jsonPath("$.detail", is("Неможливо скасувати замовлення у статусі DELIVERED")))
                    .andExpect(jsonPath("$.instance", is("/api/catalog/test-exceptions/business-rule-default")));
        }

        @Test
        @DisplayName("BusinessRuleException (with CONFLICT status) returns 409 Conflict")
        void businessRuleExceptionReturns409WhenConfigured() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/business-rule-conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.title", is("Конфлікт бізнес-правил")))
                    .andExpect(jsonPath("$.detail", is("Товар закінчився на складі під час оформлення")))
                    .andExpect(jsonPath("$.instance", is("/api/catalog/test-exceptions/business-rule-conflict")));
        }
    }

    // =========================================================================
    // Tests: Concurrency & Data Integrity
    // =========================================================================

    @Nested
    @DisplayName("Concurrency and Data Integrity (409 Conflict)")
    class ConcurrencyAndDataIntegrityTests {

        @Test
        @DisplayName("ObjectOptimisticLockingFailureException returns 409 and does not leak internal class details")
        void optimisticLockReturns409WithoutLeak() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/optimistic-lock"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.title", is("Конфлікт одночасного редагування")))
                    .andExpect(jsonPath("$.detail", containsString("Дані були змінені іншим користувачем")))
                    .andExpect(jsonPath("$.detail", not(containsString("ObjectOptimisticLockingFailureException"))));
        }

        @Test
        @DisplayName("DataIntegrityViolationException returns 409 without leaking SQL/table names")
        void dataIntegrityReturns409WithoutSqlLeak() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/data-integrity"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.title", is("Конфлікт цілісності даних")))
                    .andExpect(jsonPath("$.detail", is("Операція не може бути виконана через порушення унікальності або цілісності даних.")))
                    .andExpect(jsonPath("$.detail", not(containsString("uk_books_isbn"))))
                    .andExpect(jsonPath("$.detail", not(containsString("duplicate key"))));
        }
    }

    // =========================================================================
    // Tests: Validation & Parameter Mismatches (400 Bad Request)
    // =========================================================================

    @Nested
    @DisplayName("Validation and Parameter Errors (400 Bad Request)")
    class ValidationAndParameterErrorsTests {

        @Test
        @DisplayName("MethodArgumentNotValidException returns 400 with errors[{ field, message }]")
        void dtoValidationReturns400WithErrorsList() throws Exception {
            String invalidJson = """
                    {
                        "name": "",
                        "email": "invalid-email",
                        "quantity": 0
                    }
                    """;

            mockMvc.perform(post("/api/catalog/test-exceptions/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Помилка валідації")))
                    .andExpect(jsonPath("$.errors", hasSize(3)))
                    .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value(
                            org.hamcrest.Matchers.hasItem(containsString("Невірний формат email. Приклад: name@example.com"))
                    ))
                    .andExpect(jsonPath("$.errors[?(@.field == 'quantity')].message").exists());
        }

        @Test
        @DisplayName("ConstraintViolationException (path/query) returns 400 with errors array")
        void constraintViolationReturns400WithErrors() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/param-validation/-5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Помилка валідації")))
                    .andExpect(jsonPath("$.errors[0].message", containsString("ID має бути додатним числом")));
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException returns 400 with parameter name and expected type")
        void methodArgumentTypeMismatchReturns400() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/type-mismatch").param("number", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Некоректний тип параметра")))
                    .andExpect(jsonPath("$.detail", containsString("Параметр 'number' має некоректний тип")));
        }

        @Test
        @DisplayName("MissingServletRequestParameterException returns 400 with missing parameter name")
        void missingServletRequestParameterReturns400() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/missing-param"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Відсутній обов'язковий параметр")))
                    .andExpect(jsonPath("$.detail", containsString("requiredParam")));
        }

        @Test
        @DisplayName("HttpMessageNotReadableException (malformed JSON) returns 400")
        void malformedJsonReturns400() throws Exception {
            mockMvc.perform(post("/api/catalog/test-exceptions/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not-a-valid-json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Некоректне тіло запиту")));
        }
    }

    // =========================================================================
    // Tests: Standard HTTP Error Statuses (405, 415)
    // =========================================================================

    @Nested
    @DisplayName("Standard Spring MVC HTTP Errors (405, 415 are not 500)")
    class StandardHttpErrorsTests {

        @Test
        @DisplayName("HttpRequestMethodNotSupportedException returns 405 (does not turn into 500)")
        void methodNotAllowedReturns405() throws Exception {
            mockMvc.perform(post("/api/catalog/test-exceptions/get-only"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(405)))
                    .andExpect(jsonPath("$.title", is("Метод не підтримується")))
                    .andExpect(jsonPath("$.detail", containsString("POST")));
        }

        @Test
        @DisplayName("HttpMediaTypeNotSupportedException returns 415")
        void mediaTypeNotSupportedReturns415() throws Exception {
            mockMvc.perform(post("/api/catalog/test-exceptions/validation")
                            .contentType(MediaType.APPLICATION_XML)
                            .content("<xml></xml>"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(415)))
                    .andExpect(jsonPath("$.title", is("Непідтримуваний тип медіа")));
        }
    }

    // =========================================================================
    // Tests: Security Exception Propagation (401, 403 are not 500)
    // =========================================================================

    @Nested
    @DisplayName("Security Exception Handling (401, 403 are not turned into 500)")
    class SecurityExceptionHandlingTests {

        @Test
        @DisplayName("Anonymous access to protected /api/orders returns 401 (not 500)")
        void anonymousAccessReturns401Not500() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.title", is("Unauthorized")));
        }

        @Test
        @DisplayName("USER role access to /api/manager/books returns 403 (not 500)")
        @WithMockUser(roles = "USER")
        void userAccessToManagerReturns403Not500() throws Exception {
            mockMvc.perform(get("/api/manager/books"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.title", is("Forbidden")));
        }

        @Test
        @DisplayName("AccessDeniedException thrown within controller propagation produces 403 (not 500)")
        @WithMockUser(roles = "USER")
        void accessDeniedExceptionInsideControllerProduces403() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/access-denied"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.title", is("Forbidden")));
        }
    }

    // =========================================================================
    // Tests: Safety Net (500 Internal Server Error)
    // =========================================================================

    @Nested
    @DisplayName("Safety Net Exception Handling (500 with errorId)")
    class SafetyNetTests {

        @Test
        @DisplayName("Unhandled Exception returns 500 with unique errorId and without stacktrace leak")
        void unhandledExceptionReturns500WithErrorId() throws Exception {
            mockMvc.perform(get("/api/catalog/test-exceptions/unhandled-server-error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(500)))
                    .andExpect(jsonPath("$.title", is("Внутрішня помилка сервера")))
                    .andExpect(jsonPath("$.errorId").isString())
                    .andExpect(jsonPath("$.detail", containsString("Виникла непередбачена помилка")))
                    .andExpect(jsonPath("$.detail", not(containsString("NullPointerException"))))
                    .andExpect(jsonPath("$.detail", not(containsString("Simulated unexpected"))));
        }
    }

    // =========================================================================
    // Tests: SPA Compatibility and 404 Routing
    // =========================================================================

    @Nested
    @DisplayName("SPA Compatibility and 404 Routing")
    class SpaRoutingAndNotFoundTests {

        @Test
        @DisplayName("Unknown /api/** route returns 404 Problem Details JSON")
        void unknownApiRouteReturns404Json() throws Exception {
            mockMvc.perform(get("/api/catalog/non-existent-endpoint"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Ресурс не знайдено")));
        }

        @Test
        @DisplayName("Unknown non-API route forwards to /index.html for SPA semantic URLs")
        void unknownNonApiRouteForwardsToIndexHtml() throws Exception {
            mockMvc.perform(get("/catalog/frontend-page"))
                    .andExpect(forwardedUrl("/index.html"));
        }
    }
}
