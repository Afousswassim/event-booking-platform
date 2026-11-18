package com.eventbooking.security;

import com.eventbooking.model.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AuthFilterTest {
    private static final String TOKEN = "A".repeat(43);
    private final AuthFilter filter = new AuthFilter();
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private HttpSession session;
    private StringWriter body;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        session = mock(HttpSession.class);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(request.getServletPath()).thenReturn("/auth");
        when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void profileRequiresAuthenticationEvenWithAnAnonymousSession() throws Exception {
        when(request.getPathInfo()).thenReturn("/profile");
        when(request.getSession(false)).thenReturn(session);

        filter.doFilter(request, response, chain);

        denied(401, "authentication_required");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void eachRoleCanReachOnlyItsOwnProtectedRoutes(Role actual) throws Exception {
        authenticate(actual);
        for (Role required : Role.values()) {
            reset(chain);
            var name = required.name().toLowerCase(Locale.ROOT);
            when(request.getPathInfo()).thenReturn("/access/" + name);
            filter.doFilter(request, response, chain);
            if (actual == required) {
                verify(chain).doFilter(request, response);
            } else {
                denied(403, "forbidden");
            }
        }
    }

    @Test
    void protectsRolePrefixesOutsideTheAuthServlet() throws Exception {
        authenticate(Role.CLIENT);
        when(request.getServletPath()).thenReturn("/admin/reports");
        filter.doFilter(request, response, chain);
        denied(403, "forbidden");
    }

    @Test
    void postLoginRejectsMissingTokenEvenForAPublicEndpoint() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getPathInfo()).thenReturn("/login");
        filter.doFilter(request, response, chain);
        denied(403, "invalid_csrf");
    }

    @Test
    void profilePostAcceptsSessionBoundHeaderAndChecksTheToken() throws Exception {
        authenticate(Role.CLIENT);
        when(request.getMethod()).thenReturn("POST");
        when(request.getPathInfo()).thenReturn("/profile");
        when(session.getAttribute(AuthSession.CSRF)).thenReturn(TOKEN);
        when(request.getHeader(AuthFilter.CSRF_HEADER)).thenReturn("B".repeat(43));
        filter.doFilter(request, response, chain);
        denied(403, "invalid_csrf");

        when(request.getHeader(AuthFilter.CSRF_HEADER)).thenReturn(TOKEN);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void registrationAcceptsAFormTokenButRejectsParametersInTheUrl() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getPathInfo()).thenReturn("/register");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.CSRF)).thenReturn(TOKEN);
        when(request.getParameter(AuthFilter.CSRF_FIELD)).thenReturn(TOKEN);

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        reset(chain);
        when(request.getQueryString()).thenReturn("csrfToken=" + TOKEN);
        filter.doFilter(request, response, chain);
        denied(400, "unexpected_query");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/register", "/logout"})
    void browserFormPostsRequireCsrf(String path) throws Exception {
        authenticate(Role.CLIENT);
        when(request.getServletPath()).thenReturn(path);
        when(request.getMethod()).thenReturn("POST");
        when(session.getAttribute(AuthSession.CSRF)).thenReturn(TOKEN);

        filter.doFilter(request, response, chain);
        denied(403, "invalid_csrf");

        when(request.getParameter(AuthFilter.CSRF_FIELD)).thenReturn(TOKEN);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/my-reservations", "/my-tickets"})
    void browserClientPagesRedirectGuestsAndRejectOtherRoles(String path) throws Exception {
        when(request.getServletPath()).thenReturn(path);
        when(request.getContextPath()).thenReturn("/event-booking-platform");

        filter.doFilter(request, response, chain);
        verify(response).sendRedirect("/event-booking-platform/login");
        verifyNoInteractions(chain);

        authenticate(Role.ADMIN);
        filter.doFilter(request, response, chain);
        denied(403, "forbidden");

        authenticate(Role.CLIENT);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    private void authenticate(Role role) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.IDENTITY)).thenReturn(new AuthSession.Identity(7L, role));
    }

    private void denied(int status, String code) throws Exception {
        verify(response, atLeastOnce()).setStatus(status);
        assertTrue(body.toString().contains(code));
        verifyNoInteractions(chain);
    }
}
