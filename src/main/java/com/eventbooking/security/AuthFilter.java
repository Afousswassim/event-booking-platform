
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

@WebFilter(urlPatterns = {
    "/client/*",
    "/admin/*",
    "/agent/*",
    "/my-reservations",
    "/my-tickets"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request =
            (HttpServletRequest) servletRequest;

        HttpServletResponse response =
            (HttpServletResponse) servletResponse;

        HttpSession session =
            request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null) {

            response.sendRedirect(
                request.getContextPath() + "/login"
            );
            return;
        }

        Object roleObject =
            session.getAttribute("userRole");

        if (!(roleObject instanceof Role)) {
            response.sendError(
                HttpServletResponse.SC_FORBIDDEN
            );
            return;
        }

        Role role = (Role) roleObject;

        String path = request.getServletPath();

        if (path.startsWith("/admin/")
                && role != Role.ADMIN) {

            response.sendError(
                HttpServletResponse.SC_FORBIDDEN
            );
            return;
        }

        if (path.startsWith("/agent/")
                && role != Role.AGENT) {

            response.sendError(
                HttpServletResponse.SC_FORBIDDEN
            );
            return;
        }

        if (path.startsWith("/client/")
                && role != Role.CLIENT) {

            response.sendError(
                HttpServletResponse.SC_FORBIDDEN
            );
            return;
        }

        if ((path.equals("/my-reservations")
                || path.equals("/my-tickets"))
                && role != Role.CLIENT) {

            response.sendError(
                HttpServletResponse.SC_FORBIDDEN
            );
            return;
        }

        chain.doFilter(request, response);
    }
}
