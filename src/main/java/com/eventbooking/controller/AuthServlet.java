package com.eventbooking.controller;

import com.eventbooking.security.AuthResponse;
import com.eventbooking.security.AuthSession;
import com.eventbooking.model.Role;
import com.eventbooking.service.UserService;
import com.eventbooking.service.UserService.Profile;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;

@WebServlet({"/auth/*", "/login", "/register", "/logout", "/admin", "/agent"})
public class AuthServlet extends HttpServlet {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    static final String USER_NAME = "userName";
    private static final String REGISTRATION_COMPLETE = "registrationComplete";

    @Inject
    private UserService users;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (isLegacyForm(request)) {
            if (!forwardLegacyForm(request, response, null)) {
                AuthResponse.error(response, 404, "not_found");
            }
            return;
        }
        try {
            switch (path(request)) {
                case "/csrf" -> AuthResponse.json(response, 200, Json.createObjectBuilder()
                        .add("csrfToken", AuthSession.csrfToken(request.getSession(true))).build());
                case "/profile" -> {
                    var identity = AuthSession.identity(request.getSession(false));
                    AuthResponse.json(response, 200, userBody(users.profile(identity.id())));
                }
                case "/access/client", "/access/admin", "/access/agent" -> response.setStatus(204);
                case "/admin", "/agent" -> {
                    request.setAttribute(AuthSession.CSRF, AuthSession.csrfToken(request.getSession(false)));
                    request.getRequestDispatcher("/WEB-INF/views/auth/account.jsp").forward(request, response);
                }
                case "/register", "/login", "/logout" -> methodNotAllowed(response, "POST");
                default -> AuthResponse.error(response, 404, "not_found");
            }
        } catch (UserService.Failure failure) {
            respondToFailure(request, response, failure);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            switch (path(request)) {
                case "/register", "/login", "/profile" -> {
                    if (!isForm(request.getContentType())) {
                        AuthResponse.error(response, 415, "unsupported_content_type");
                        return;
                    }
                    handleForm(request, response);
                }
                case "/logout" -> {
                    var session = request.getSession(false);
                    var identity = AuthSession.identity(session);
                    session.invalidate();
                    if ("/logout".equals(request.getServletPath())) {
                        String destination = identity != null && identity.role() == Role.CLIENT ? "/" : "/login";
                        response.sendRedirect(request.getContextPath() + destination);
                    } else {
                        response.setStatus(204);
                    }
                }
                case "/csrf", "/access/client", "/access/admin", "/access/agent", "/admin", "/agent" ->
                    methodNotAllowed(response, "GET, HEAD");
                default -> AuthResponse.error(response, 404, "not_found");
            }
        } catch (UserService.Failure failure) {
            respondToFailure(request, response, failure);
        }
    }

    private void handleForm(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String email = request.getParameter("email");
        switch (path(request)) {
            case "/register" -> {
                String password = request.getParameter("password");
                if (isLegacyForm(request)) {
                    if (!Objects.equals(password, request.getParameter("passwordConfirmation"))) {
                        rejectRegistrationForm(request, response, "password_mismatch", "Les mots de passe ne correspondent pas.");
                        return;
                    }
                    if (!"accepted".equals(request.getParameter("terms"))) {
                        rejectRegistrationForm(request, response, "terms_required", "Acceptez les conditions d’utilisation pour continuer.");
                        return;
                    }
                }
                Profile profile = users.register(request.getParameter("nom"), email, password);
                if (isLegacyForm(request)) {
                    request.getSession(false).setAttribute(REGISTRATION_COMPLETE, true);
                    String destination = request.getContextPath() + "/login";
                    if (wantsJson(request)) {
                        AuthResponse.json(response, 201, Json.createObjectBuilder()
                                .add("user", profileJson(profile)).add("redirectUrl", destination).build());
                    } else {
                        response.sendRedirect(destination);
                    }
                } else {
                    AuthResponse.json(response, 201, userBody(profile));
                }
            }
            case "/login" -> {
                Profile profile = users.authenticate(email, request.getParameter("password"));
                HttpSession previous = request.getSession(false);
                previous.invalidate();
                HttpSession session = request.getSession(true);
                String csrf = AuthSession.csrfToken(session);
                session.setAttribute(AuthSession.IDENTITY, new AuthSession.Identity(profile.id(), profile.role()));
                session.setAttribute("userId", profile.id());
                session.setAttribute(USER_NAME, profile.nom());
                session.setAttribute("userRole", profile.role());
                Long eventId = ReservationServlet.positiveId(request.getParameter("eventId"));
                String destination = request.getContextPath() + profile.role().getHomePath();
                if (profile.role() == Role.CLIENT && eventId != null) {
                    destination = request.getContextPath() + "/my-reservations?eventId=" + eventId;
                }
                if (isLegacyForm(request)) {
                    response.sendRedirect(destination);
                } else {
                    AuthResponse.json(response, 200, Json.createObjectBuilder()
                            .add("user", profileJson(profile)).add("csrfToken", csrf)
                            .add("redirectUrl", destination).build());
                }
            }
            case "/profile" -> {
                HttpSession session = request.getSession(false);
                var identity = AuthSession.identity(session);
                Profile profile = users.updateProfile(identity.id(), request.getParameter("nom"), email,
                        request.getParameter("currentPassword"));
                session.setAttribute(USER_NAME, profile.nom());
                AuthResponse.json(response, 200, userBody(profile));
            }
            default -> throw new IllegalStateException("Unexpected authentication form route");
        }
    }

    private static void respondToFailure(HttpServletRequest request, HttpServletResponse response,
            UserService.Failure failure) throws IOException, ServletException {
        if (isLegacyForm(request) && !wantsJson(request) && forwardLegacyForm(request, response, switch (failure.problem()) {
            case INVALID_NAME -> "Nom invalide";
            case INVALID_EMAIL -> "Adresse e-mail invalide";
            case INVALID_PASSWORD -> "Mot de passe invalide";
            case EMAIL_TAKEN -> "Cette adresse e-mail est déjà utilisée";
            case INVALID_CREDENTIALS, ACCOUNT_UNAVAILABLE -> "Email ou mot de passe incorrect";
        })) {
            return;
        }
        switch (failure.problem()) {
            case INVALID_NAME -> AuthResponse.error(response, 400, "invalid_name");
            case INVALID_EMAIL -> AuthResponse.error(response, 400, "invalid_email");
            case INVALID_PASSWORD -> AuthResponse.error(response, 400, "invalid_password");
            case INVALID_CREDENTIALS -> AuthResponse.error(response, 401, "invalid_credentials");
            case EMAIL_TAKEN -> AuthResponse.error(response, 409, "email_taken");
            case ACCOUNT_UNAVAILABLE -> {
                request.getSession(false).invalidate();
                AuthResponse.error(response, 401, "authentication_required");
            }
        }
    }

    private static String path(HttpServletRequest request) {
        return Objects.toString(request.getPathInfo(), Objects.toString(request.getServletPath(), ""));
    }

    private static boolean isLegacyForm(HttpServletRequest request) {
        return "/login".equals(request.getServletPath()) || "/register".equals(request.getServletPath());
    }

    private static boolean forwardLegacyForm(HttpServletRequest request, HttpServletResponse response,
            String error) throws IOException, ServletException {
        String view = "/WEB-INF/views/auth" + request.getServletPath() + ".jsp";
        if (request.getServletContext().getResource(view) == null) {
            return false;
        }
        HttpSession session = request.getSession(true);
        request.setAttribute(AuthSession.CSRF, AuthSession.csrfToken(session));
        if ("/login".equals(request.getServletPath())) {
            request.setAttribute("bookingEventId", ReservationServlet.positiveId(request.getParameter("eventId")));
        }
        if (error != null) {
            request.setAttribute("error", error);
        } else if ("/login".equals(request.getServletPath()) && Boolean.TRUE.equals(session.getAttribute(REGISTRATION_COMPLETE))) {
            session.removeAttribute(REGISTRATION_COMPLETE);
            request.setAttribute(REGISTRATION_COMPLETE, true);
        }
        request.getRequestDispatcher(view).forward(request, response);
        return true;
    }

    private static void rejectRegistrationForm(HttpServletRequest request, HttpServletResponse response,
            String code, String message) throws IOException, ServletException {
        response.setStatus(400);
        if (wantsJson(request) || !forwardLegacyForm(request, response, message)) {
            AuthResponse.error(response, 400, code);
        }
    }

    private static boolean wantsJson(HttpServletRequest request) {
        return Objects.toString(request.getHeader("Accept"), "").contains("application/json");
    }

    private static boolean isForm(String contentType) {
        return contentType != null && FORM_CONTENT_TYPE.equalsIgnoreCase(contentType.split(";", 2)[0].trim());
    }

    private static void methodNotAllowed(HttpServletResponse response, String allowed) throws IOException {
        response.setHeader("Allow", allowed);
        AuthResponse.error(response, 405, "method_not_allowed");
    }

    private static JsonObject userBody(Profile profile) {
        return Json.createObjectBuilder().add("user", profileJson(profile)).build();
    }

    private static JsonObject profileJson(Profile profile) {
        return Json.createObjectBuilder().add("id", profile.id()).add("nom", profile.nom())
                .add("email", profile.email()).add("role", profile.role().name()).build();
    }
}
