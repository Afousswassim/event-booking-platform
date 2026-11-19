package com.eventbooking.security;

import com.eventbooking.model.Role;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@WebFilter(urlPatterns = {"/auth/*", "/login", "/register", "/logout",
        "/client/*", "/admin/*", "/agent/*", "/my-reservations", "/my-tickets"})
public class AuthFilter implements Filter {
    public static final String CSRF_HEADER = "X-CSRF-Token";
    public static final String CSRF_FIELD = "csrfToken";

    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
            throws IOException, ServletException {
        var request = (HttpServletRequest) input;
        var response = (HttpServletResponse) output;
        String path = request.getServletPath() + Objects.toString(request.getPathInfo(), "");
        boolean authRoute = path.equals("/auth") || path.startsWith("/auth/")
                || path.equals("/login") || path.equals("/register") || path.equals("/logout");
        String method = request.getMethod();
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        request.setCharacterEncoding(StandardCharsets.UTF_8);

        HttpSession session = request.getSession(false);
        AuthSession.Identity identity = AuthSession.identity(session);
        if (!isPublic(path)) {
            if (identity == null) {
                if ((isClientPage(path) || isAccountPage(path)) && (method.equals("GET") || method.equals("HEAD"))) {
                    response.sendRedirect(request.getContextPath() + "/login");
                } else {
                    AuthResponse.error(response, 401, "authentication_required");
                }
                return;
            }
            Role required = requiredRole(path);
            if (required != null && identity.role() != required) {
                AuthResponse.error(response, 403, "forbidden");
                return;
            }
        }

        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("OPTIONS")) {
            if (authRoute && request.getQueryString() != null) {
                AuthResponse.error(response, 400, "unexpected_query");
                return;
            }
            String token = request.getHeader(CSRF_HEADER);
            if (token == null && request.getQueryString() == null && method.equals("POST")) {
                token = request.getParameter(CSRF_FIELD);
            }
            if (!AuthSession.validCsrf(session, token)) {
                AuthResponse.error(response, 403, "invalid_csrf");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static boolean isPublic(String path) {
        return switch (path) {
            case "/auth/csrf", "/auth/register", "/auth/login", "/register", "/login" -> true;
            default -> false;
        };
    }

    private static Role requiredRole(String path) {
        if (path.equals("/auth/access/client") || path.equals("/client") || path.startsWith("/client/")
                || isClientPage(path)) {
            return Role.CLIENT;
        }
        if (path.equals("/auth/access/admin") || path.equals("/admin") || path.startsWith("/admin/")) {
            return Role.ADMIN;
        }
        if (path.equals("/auth/access/agent") || path.equals("/agent") || path.startsWith("/agent/")) {
            return Role.AGENT;
        }
        return null;
    }

    private static boolean isClientPage(String path) {
        return path.equals("/my-reservations") || path.equals("/my-tickets");
    }

    private static boolean isAccountPage(String path) {
        return path.equals(Role.CLIENT.getHomePath()) || path.equals(Role.ADMIN.getHomePath())
                || path.equals(Role.AGENT.getHomePath());
    }
}
