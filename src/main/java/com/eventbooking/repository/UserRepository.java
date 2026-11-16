
package com.eventbooking.repository;

import com.eventbooking.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext
    private EntityManager em;

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public User findByEmail(String email) {
        return em.createQuery(
            "SELECT u FROM User u WHERE u.email = :email",
            User.class
        )
        .setParameter("email", email)
        .getResultStream()
        .findFirst()
        .orElse(null);
    }

    @Transactional
    public void save(User user) {
        em.persist(user);
    }
}
