
package com.eventbooking.repository;

import com.eventbooking.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext(unitName = "eventBookingPU")
    private EntityManager em;

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public User findByEmail(String email) {
        return em.createQuery(
            "SELECT u FROM User u WHERE lower(u.email) = :email",
            User.class
        )
        .setParameter("email", email)
        .setMaxResults(1)
        .getResultList()
        .stream()
        .findFirst()
        .orElse(null);
    }

    public User findByIdForUpdate(Long id) {
        return em.find(User.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    @Transactional
    public void save(User user) {
        em.persist(user);
        em.flush();
    }

    public void flush() {
        em.flush();
    }
}
