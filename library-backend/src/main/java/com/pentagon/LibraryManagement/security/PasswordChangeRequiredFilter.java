package com.pentagon.LibraryManagement.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pentagon.LibraryManagement.DAO.UsersDAO;
import com.pentagon.LibraryManagement.entity.Users;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Blocks normal protected endpoints while a member still has a staff-issued
 * temporary password. The only protected endpoint allowed in that state is the
 * authenticated password change itself.
 *
 * Runs after the JWT filter, so the SecurityContext is already populated.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH = "/change-password";

    @Autowired
    private UsersDAO usersDAO;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // CORS preflight requests carry no credentials and must pass through.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !isChangePasswordRequest(request)) {

            Users user = usersDAO.findByEmail(authentication.getName());

            if (user != null && user.isPasswordChangeRequired()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"statuscode\":403,\"message\":"
                        + "\"Password change required before using other endpoints\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isChangePasswordRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return CHANGE_PASSWORD_PATH.equals(path);
    }
}
