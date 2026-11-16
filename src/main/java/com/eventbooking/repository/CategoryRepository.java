package com.eventbooking.repository;

import com.eventbooking.model.Category;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Repository JPA pour la gestion des catégories d'événements.
 */
@ApplicationScoped
public class CategoryRepository {

    @PersistenceContext(unitName = "eventBookingPU")
    private EntityManager em;

    public Category findById(Long id) {
        if (id == null) return null;
        return em.find(Category.class, id);
    }

    public List<Category> findAll() {
        return em.createQuery("SELECT c FROM Category c ORDER BY c.nom ASC", Category.class)
                 .getResultList();
    }

    @Transactional
    public Category save(Category category) {
        em.persist(category);
        return category;
    }

    @Transactional
    public Category update(Category category) {
        return em.merge(category);
    }

    @Transactional
    public boolean delete(Long id) {
        Category category = findById(id);
        if (category != null) {
            em.remove(category);
            return true;
        }
        return false;
    }
}
