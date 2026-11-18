package com.eventbooking;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

/**
 * Classe de base pour l'initialisation de l'EntityManager JPA dans les tests.
 */
public abstract class BaseJpaTest {

    protected static EntityManagerFactory emf;
    protected EntityManager em;

    @BeforeAll
    public static void initEmf() {
        emf = Persistence.createEntityManagerFactory("eventBookingTestPU");
    }

    @AfterAll
    public static void closeEmf() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    public void initEm() {
        em = emf.createEntityManager();
    }

    @AfterEach
    public void closeEm() {
        if (em != null && em.isOpen()) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    protected void doInTransaction(Runnable action) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            action.run();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    protected void injectEntityManager(Object target, EntityManager entityManager) {
        try {
            Field emField = target.getClass().getDeclaredField("em");
            emField.setAccessible(true);
            emField.set(target, entityManager);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'injection de l'EntityManager sur " + target, e);
        }
    }
}
