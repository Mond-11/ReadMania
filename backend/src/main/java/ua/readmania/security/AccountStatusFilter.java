package ua.readmania.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import ua.readmania.domain.user.User;
import ua.readmania.domain.user.UserRepository;
import ua.readmania.domain.user.UserStatus;

/**
 * Filter for active session account status verification (M6).
 * Checks if the authenticated user's account has been blocked during an ongoing session.
 *
 * If account is BLOCKED, immediately clears security context, invalidates session,
 * and writes 403 Forbidden with RFC 9457 Problem Details.
 */
@Component
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AccountStatusFilter(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

            if (userOpt.isPresent() && userOpt.get().getStatus() == UserStatus.BLOCKED) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                String blockReason = userOpt.map(User::getBlockReason)
                        .filter(r -> r != null && !r.isBlank())
                        .orElse("Обліковий запис заблоковано");

                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, blockReason);
                problemDetail.setTitle("Forbidden");
                problemDetail.setInstance(URI.create(request.getRequestURI()));

                objectMapper.writeValue(response.getWriter(), problemDetail);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
