package com.eventbooking.security;

import com.eventbooking.model.Role;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class AuthSession {
    public static final String IDENTITY = "authUser";
    public static final String CSRF = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private AuthSession() {
    }

    public static Identity identity(HttpSession session) {
        return session != null && session.getAttribute(IDENTITY) instanceof Identity identity
                ? identity : null;
    }

    public static String csrfToken(HttpSession session) {
        synchronized (session) {
            if (session.getAttribute(CSRF) instanceof String token) {
                return token;
            }
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF, token);
            return token;
        }
    }

    public static boolean validCsrf(HttpSession session, String supplied) {
        return session != null && supplied != null && supplied.length() == 43
                && session.getAttribute(CSRF) instanceof String expected
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }

    public record Identity(long id, Role role) implements Serializable {
        public long getId() {
            return id;
        }

        public Role getRole() {
            return role;
        }
    }
}
