
package com.eventbooking.controller;

import com.eventbooking.model.User;
import com.eventbooking.service.AuthService;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(urlPatterns = {
    "/login",
    "/register",
    "/logout"
})
public class AuthServlet extends HttpServlet {

    @Inject
    private AuthService authService;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        switch (path) {

            case "/login":
                request.getRequestDispatcher(
                    "/WEB-INF/views/auth/login.jsp"
                ).forward(request, response);
                break;

            case "/register":
                request.getRequestDispatcher(
                    "/WEB-INF/views/auth/register.jsp"
                ).forward(request, response);
                break;

            case "/logout":
                logout(request, response);
                break;

            default:
                response.sendError(
                    HttpServletResponse.SC_NOT_FOUND
                );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();

        switch (path) {

            case "/login":
                login(request, response);
                break;

            case "/register":
                register(request, response);
                break;

            case "/logout":
                logout(request, response);
                break;

            default:
                response.sendError(
                    HttpServletResponse.SC_NOT_FOUND
                );
        }
    }

    private void register(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String nom = request.getParameter("nom");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            authService.register(nom, email, password);

            response.sendRedirect(
                request.getContextPath() + "/login?registered=1"
            );

        } catch (IllegalArgumentException e) {

            request.setAttribute(
                "error",
                e.getMessage()
            );

            request.getRequestDispatcher(
                "/WEB-INF/views/auth/register.jsp"
            ).forward(request, response);
        }
    }

    private void login(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            User user = authService.login(email, password);

            HttpSession oldSession = request.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);

            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getNom());
            session.setAttribute("userRole", user.getRole());

            session.setMaxInactiveInterval(30 * 60);

            response.sendRedirect(
                request.getContextPath() + "/"
            );

        } catch (IllegalArgumentException e) {

            request.setAttribute(
                "error",
                "Email ou mot de passe incorrect"
            );

            request.getRequestDispatcher(
                "/WEB-INF/views/auth/login.jsp"
            ).forward(request, response);
        }
    }

    private void logout(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(
            request.getContextPath() + "/login"
        );
    }
}
