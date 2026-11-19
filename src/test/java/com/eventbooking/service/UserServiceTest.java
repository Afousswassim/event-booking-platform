package com.eventbooking.service;

import com.eventbooking.model.Role;
import com.eventbooking.model.User;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.PasswordUtils;
import com.eventbooking.service.UserService.Failure;
import com.eventbooking.service.UserService.Problem;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String HASH = PasswordUtils.hash(PASSWORD);
    private UserRepository repository;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        service = new UserService(repository);
    }

    @Test
    void registrationNormalizesInputAndPersistsOnlyAClientWithABcryptHash() {
        var saved = ArgumentCaptor.forClass(User.class);
        doAnswer(call -> {
            User user = call.getArgument(0);
            var id = User.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(user, 7L);
            return null;
        }).when(repository).save(saved.capture());

        var profile = service.register("  Sam  ", " SAM@Example.test ", PASSWORD);

        assertEquals(new UserService.Profile(7L, "Sam", "sam@example.test", Role.CLIENT), profile);
        assertTrue(saved.getValue().getPasswordHash().startsWith("$2b$12$"));
        assertNotEquals(PASSWORD, saved.getValue().getPasswordHash());
        assertTrue(PasswordUtils.matches(PASSWORD, saved.getValue().getPasswordHash()));
    }

    @Test
    void registrationRejectsInvalidFieldsIncludingOversizedUtf8Passwords() {
        assertProblem(Problem.INVALID_NAME, () -> service.register(" ", "sam@example.test", PASSWORD));
        assertProblem(Problem.INVALID_EMAIL, () -> service.register("Sam", "sam..test@example.test", PASSWORD));
        assertProblem(Problem.INVALID_PASSWORD, () -> service.register("Sam", "sam@example.test", "x".repeat(11)));
        assertProblem(Problem.INVALID_PASSWORD, () -> service.register("Sam", "sam@example.test", "é".repeat(37)));
        verifyNoInteractions(repository);
    }

    @Test
    void bothAnExistingAccountAndAConcurrentDuplicateReturnTheSameConflict() {
        when(repository.findByEmail("taken@example.test")).thenReturn(account(1L, Role.CLIENT));
        assertProblem(Problem.EMAIL_TAKEN, () -> service.register("Sam", "TAKEN@example.test", PASSWORD));

        doThrow(new PersistenceException(new SQLException("Duplicate key", "23000", 1062)))
                .when(repository).save(any(User.class));
        assertProblem(Problem.EMAIL_TAKEN, () -> service.register("Sam", "race@example.test", PASSWORD));
    }

    @Test
    void loginUsesStoredRoleAndRejectsBothWrongPasswordAndUnknownAccount() {
        when(repository.findByEmail("sam@example.test")).thenReturn(account(7L, Role.AGENT));

        assertEquals(Role.AGENT, service.authenticate("SAM@example.test", PASSWORD).role());
        assertProblem(Problem.INVALID_CREDENTIALS, () -> service.authenticate("sam@example.test", PASSWORD + "x"));
        assertProblem(Problem.INVALID_CREDENTIALS, () -> service.authenticate("unknown@example.test", PASSWORD));
    }

    @Test
    void profileUpdateRequiresCurrentPasswordAndPreservesRoleAndHash() {
        User user = account(7L, Role.ADMIN);
        when(repository.findById(7L)).thenReturn(user);
        when(repository.findByIdForUpdate(7L)).thenReturn(user);
        when(repository.findByEmail("taken@example.test")).thenReturn(account(8L, Role.CLIENT));
        when(repository.findByEmail("sam@example.test")).thenReturn(user);

        assertEquals("Sam", service.profile(7L).nom());
        assertProblem(Problem.ACCOUNT_UNAVAILABLE, () -> service.profile(8L));
        assertProblem(Problem.INVALID_CREDENTIALS,
                () -> service.updateProfile(7L, "New", "new@example.test", PASSWORD + "x"));
        assertProblem(Problem.EMAIL_TAKEN,
                () -> service.updateProfile(7L, "New", "taken@example.test", PASSWORD));
        assertEquals("Sam", user.getNom());
        verify(repository, never()).flush();

        assertEquals("New", service.updateProfile(7L, "New", "SAM@example.test", PASSWORD).nom());
        assertEquals("new@example.test", service.updateProfile(7L, "Sam", "NEW@example.test", PASSWORD).email());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(HASH, user.getPasswordHash());
        verify(repository, times(2)).flush();
    }

    private static User account(long id, Role role) {
        return new User("Sam", "sam@example.test", HASH, role) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    private static void assertProblem(Problem problem, Runnable useCase) {
        assertEquals(problem, assertThrows(Failure.class, useCase::run).problem());
    }
}
