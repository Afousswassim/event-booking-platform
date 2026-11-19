
package com.eventbooking.service;

import com.eventbooking.model.User;
import com.eventbooking.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Provides the original service API using the current authentication rules. */
@ApplicationScoped
public class AuthService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserService users;

    @Transactional
    public User register(
            String nom,
            String email,
            String password) {
        try {
            return userRepository.findById(users.register(nom, email, password).id());
        } catch (UserService.Failure failure) {
            throw new IllegalArgumentException(failure.getMessage(), failure);
        }
    }

    public User login(String email, String password) {
        try {
            return userRepository.findById(users.authenticate(email, password).id());
        } catch (UserService.Failure failure) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect", failure);
        }
    }
}
