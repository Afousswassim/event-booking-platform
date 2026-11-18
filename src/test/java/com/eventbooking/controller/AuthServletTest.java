package com.eventbooking.controller;

import com.eventbooking.model.Role;
import com.eventbooking.security.AuthSession;
import com.eventbooking.service.UserService;
import com.eventbooking.service.UserService.Profile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServletTest {
    @Mock private UserService users;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession previous;
    @Mock private HttpSession current;
    @InjectMocks private AuthServlet servlet;
    private AutoCloseable mocks;
    private StringWriter body;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(request.getSession(false)).thenReturn(previous);
        when(request.getSession(true)).thenReturn(current);
        when(request.getContentType()).thenReturn("application/x-www-form-urlencoded; charset=UTF-8");
    }

    @AfterEach
    void closeMocks() throws Exception {
        mocks.close();
    }

    @Test
    void successfulLoginInvalidatesTheOldSessionAndCreatesAnIdentityAndToken() throws Exception {
        String password = UUID.randomUUID().toString();
        when(request.getPathInfo()).thenReturn("/login");
        when(request.getParameter("email")).thenReturn("sam@example.test");
        when(request.getParameter("password")).thenReturn(password);
        when(users.authenticate("sam@example.test", password))
                .thenReturn(new Profile(7L, "Sam", "sam@example.test", Role.CLIENT));

        servlet.doPost(request, response);

        var order = inOrder(previous, request, current);
        order.verify(previous).invalidate();
        order.verify(request).getSession(true);
        order.verify(current).setAttribute(eq(AuthSession.CSRF), matches("[A-Za-z0-9_-]{43}"));
        order.verify(current).setAttribute(AuthSession.IDENTITY, new AuthSession.Identity(7L, Role.CLIENT));
        verify(response).setStatus(200);
        assertTrue(body.toString().contains("\"csrfToken\""));
        assertFalse(body.toString().contains(password));
    }

    @Test
    void profileUpdateTakesItsUserIdFromTheSessionAndIgnoresRoleAndIdParameters() throws Exception {
        String password = UUID.randomUUID().toString();
        when(request.getPathInfo()).thenReturn("/profile");
        when(previous.getAttribute(AuthSession.IDENTITY)).thenReturn(new AuthSession.Identity(7L, Role.CLIENT));
        when(request.getParameter("nom")).thenReturn("Sam");
        when(request.getParameter("email")).thenReturn("sam@example.test");
        when(request.getParameter("currentPassword")).thenReturn(password);
        when(users.updateProfile(7L, "Sam", "sam@example.test", password))
                .thenReturn(new Profile(7L, "Sam", "sam@example.test", Role.CLIENT));

        servlet.doPost(request, response);

        verify(users).updateProfile(7L, "Sam", "sam@example.test", password);
        verify(request, never()).getParameter("userId");
        verify(request, never()).getParameter("role");
        assertTrue(body.toString().contains("\"role\":\"CLIENT\""));
        assertFalse(body.toString().contains(password));
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        when(request.getPathInfo()).thenReturn("/logout");
        servlet.doPost(request, response);
        verify(previous).invalidate();
        verify(response).setStatus(204);
        verifyNoInteractions(users);
    }
}
