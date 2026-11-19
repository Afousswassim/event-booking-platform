package com.eventbooking.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PasswordUtils {
    private static final int COST = 12;
    private static final int MAX_BYTES = 72;
    private static final String DUMMY_HASH = hash(UUID.randomUUID().toString());

    private PasswordUtils() {
    }

    public static boolean canVerify(String password) {
        return password != null && !password.isEmpty() && password.indexOf('\0') < 0
                && password.length() <= MAX_BYTES
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    public static String hash(String password) {
        if (!canVerify(password)) {
            throw new IllegalArgumentException("Invalid BCrypt password length or characters");
        }
        return BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(COST, password.toCharArray());
    }

    public static boolean matches(String password, String hash) {
        if (!canVerify(password)) {
            return false;
        }
        var result = BCrypt.verifyer().verify(password.toCharArray(), hash == null ? DUMMY_HASH : hash);
        return hash != null && result.verified;
    }
}
