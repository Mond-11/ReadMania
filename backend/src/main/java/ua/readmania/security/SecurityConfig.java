package ua.readmania.security;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Spring Security configuration for ReadMania.
 *
 * Implements requirements:
 * - D2: Protection against unauthorized access via Spring Security.
 * - D3: Password encoding with BCrypt.
 * - U1: Server-side role-based access control (/api/manager/** for MANAGER role only).
 * - M6: Active session account status verification hook (AccountStatusFilter).
 * - Problem Details (RFC 7807 / RFC 9457) JSON for 401 & 403 errors.
 * - Default deny for /api/** (only explicitly allowed endpoints are open).
 * - Swagger open only in "local" profile, denied in prod/other profiles.
 * - CSRF disabled on Stage 0 with TODO for XSRF token repository.
 * - CORS not enabled (handled via proxy / same-origin).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;
    private final ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailsAccessDeniedHandler accessDeniedHandler;
    private final AccountStatusFilter accountStatusFilter;

    public SecurityConfig(Environment environment,
                          ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
                          ProblemDetailsAccessDeniedHandler accessDeniedHandler,
                          AccountStatusFilter accountStatusFilter) {
        this.environment = environment;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.accountStatusFilter = accountStatusFilter;
    }

    /**
     * D3: Passwords must be encoded using BCrypt only, never stored or logged in plain text.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO: Stage 0: CSRF is disabled temporarily.
        // Implement CookieCsrfTokenRepository (cookie XSRF-TOKEN + header X-XSRF-TOKEN) before acceptance testing (separate issue).
        http.csrf(AbstractHttpConfigurer::disable);

        // CORS is not enabled. The frontend works via reverse proxy or from the same origin.
        http.cors(AbstractHttpConfigurer::disable);

        // Problem Details format for 401 and 403 errors
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );

        http.authorizeHttpRequests(auth -> {
            // Swagger / OpenAPI documentation is open only in "local" profile, closed in production
            if (environment.matchesProfiles("local")) {
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
            } else {
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").denyAll();
            }

            auth
                    // Public endpoints
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/api/catalog/**").permitAll()
                    // Actuator health check (without sensitive details)
                    .requestMatchers("/actuator/health").permitAll()
                    // U1: Manager endpoints restricted to MANAGER role
                    .requestMatchers("/api/manager/**").hasRole("MANAGER")
                    // Default deny for API: all other /api/** endpoints require authentication
                    .requestMatchers("/api/**").authenticated()
                    // Static resources and frontend pages remain open
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .anyRequest().permitAll();
        });

        // M6: Architecture hook to check account block status on each request during active session
        http.addFilterAfter(accountStatusFilter, SecurityContextHolderFilter.class);

        return http.build();
    }
}
