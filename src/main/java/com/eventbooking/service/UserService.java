package com.eventbooking.service;

import com.eventbooking.model.Role;
import com.eventbooking.model.User;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.PasswordUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

@ApplicationScoped
public class UserService {
    private static final Pattern EMAIL = Pattern.compile(
            "[a-z0-9_+%-]+(?:\\.[a-z0-9_+%-]+)*@"
            + "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
            + "(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+");

    private UserRepository users;

    protected UserService() {
    }

    @Inject
    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public Profile register(String name, String email, String password) {
        String validName = validateName(name);
        String validEmail = validateEmail(email);
        if (!PasswordUtils.canVerify(password) || password.codePointCount(0, password.length()) < 12
                || password.isBlank()) {
            throw new Failure(Problem.INVALID_PASSWORD);
        }
        requireAvailableEmail(validEmail, null);
        User user = new User(validName, validEmail, PasswordUtils.hash(password), Role.CLIENT);
        try {
            users.save(user);
        } catch (PersistenceException failure) {
            throw translateWriteFailure(failure);
        }
        return Profile.from(user);
    }

    public Profile authenticate(String email, String password) {
        String normalized = normalizeEmail(email);
        if (normalized == null || !PasswordUtils.canVerify(password)) {
            throw new Failure(Problem.INVALID_CREDENTIALS);
        }
        User user = users.findByEmail(normalized);
        if (!PasswordUtils.matches(password, user == null ? null : user.getPasswordHash())) {
            throw new Failure(Problem.INVALID_CREDENTIALS);
        }
        return Profile.from(user);
    }

    public Profile profile(long id) {
        return Profile.from(requireUser(users.findById(id)));
    }

    @Transactional
    public Profile updateProfile(long id, String name, String email, String currentPassword) {
        String validName = validateName(name);
        String validEmail = validateEmail(email);
        User user = requireUser(users.findByIdForUpdate(id));
        if (!PasswordUtils.matches(currentPassword, user.getPasswordHash())) {
            throw new Failure(Problem.INVALID_CREDENTIALS);
        }
        requireAvailableEmail(validEmail, id);
        user.setNom(validName);
        user.setEmail(validEmail);
        try {
            users.flush();
        } catch (PersistenceException failure) {
            throw translateWriteFailure(failure);
        }
        return Profile.from(user);
    }

    private void requireAvailableEmail(String email, Long ownerId) {
        User owner = users.findByEmail(email);
        if (owner != null && !owner.getId().equals(ownerId)) {
            throw new Failure(Problem.EMAIL_TAKEN);
        }
    }

    private static String validateName(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > 100
                || trimmed.codePoints().anyMatch(Character::isISOControl)) {
            throw new Failure(Problem.INVALID_NAME);
        }
        return trimmed;
    }

    private static String validateEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            throw new Failure(Problem.INVALID_EMAIL);
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        return normalized.length() <= 150 && at > 0 && at <= 64 && EMAIL.matcher(normalized).matches()
                ? normalized : null;
    }

    private static User requireUser(User user) {
        if (user == null) {
            throw new Failure(Problem.ACCOUNT_UNAVAILABLE);
        }
        return user;
    }

    private static RuntimeException translateWriteFailure(PersistenceException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) {
                return new Failure(Problem.EMAIL_TAKEN);
            }
        }
        return failure;
    }

    public record Profile(long id, String nom, String email, Role role) {
        private static Profile from(User user) {
            return new Profile(user.getId(), user.getNom(), user.getEmail(), user.getRole());
        }
    }

    public enum Problem {
        INVALID_NAME, INVALID_EMAIL, INVALID_PASSWORD, INVALID_CREDENTIALS, EMAIL_TAKEN, ACCOUNT_UNAVAILABLE
    }

    public static final class Failure extends RuntimeException {
        private final Problem problem;

        private Failure(Problem problem) {
            super(problem.name());
            this.problem = problem;
        }

        public Problem problem() {
            return problem;
        }
    }
}
