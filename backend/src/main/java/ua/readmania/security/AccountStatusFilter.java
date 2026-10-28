package ua.readmania.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for active session account status verification (M6).
 * Checks if the authenticated user's account has been blocked during an ongoing session.
 *
 * Spring Security verifies account status (enabled/non-locked) only at login time by default.
 * This filter establishes the architectural hook to verify account status on each request.
 * Full persistence/service lookup logic will be implemented in Stage 2.
 */
@Component
public class AccountStatusFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Stage 2 Architecture Hook:
            // 1. Fetch user account state from UserDetailsService / UserRepository.
            // 2. If status is BLOCKED / LOCKED / DISABLED:
            //    SecurityContextHolder.clearContext();
            //    HttpSession session = request.getSession(false);
            //    if (session != null) { session.invalidate(); }
            //    Write 403 Problem Details response and return immediately without continuing filter chain.
        }

        filterChain.doFilter(request, response);
    }
}
