
package com.eventbooking.service;

import com.eventbooking.model.User;
import com.eventbooking.model.Role;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.PasswordUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Locale;

@ApplicationScoped
public class AuthService {

    @Inject
    private UserRepository userRepository;

    @Transactional
    public User register(
            String nom,
            String email,
            String password) {

        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException(
                "Le nom est obligatoire"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "L'email est obligatoire"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                "Le mot de passe est obligatoire"
            );
        }

        String cleanNom = nom.trim();
        String cleanEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        if (cleanNom.length() > 100 ||
            cleanEmail.length() > 150 ||
            !cleanEmail.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            )) {

            throw new IllegalArgumentException(
                "Nom ou email invalide"
            );
        }

        if (userRepository.findByEmail(cleanEmail) != null) {
            throw new IllegalArgumentException(
                "Cet email est deja utilise"
            );
        }

        String hash = PasswordUtils.hashPassword(password);

        User user = new User(
            cleanNom,
            cleanEmail,
            hash,
            Role.CLIENT
        );

        userRepository.save(user);

        return user;
    }

    public User login(String email, String password) {

        if (email == null || password == null) {
            throw new IllegalArgumentException(
                "Email ou mot de passe incorrect"
            );
        }

        String cleanEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(cleanEmail);

        if (user == null ||
            !PasswordUtils.verifyPassword(
                password,
                user.getPasswordHash()
            )) {

            throw new IllegalArgumentException(
                "Email ou mot de passe incorrect"
            );
        }

        return user;
    }
}
