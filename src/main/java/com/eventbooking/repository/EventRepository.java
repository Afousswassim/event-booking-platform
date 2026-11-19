package com.eventbooking.repository;

import com.eventbooking.model.Event;
import com.eventbooking.model.EventStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Repository JPA pour la gestion des événements.
 */
@ApplicationScoped
public class EventRepository {

    @PersistenceContext(unitName = "eventBookingPU")
    private EntityManager em;

    public Event findById(Long id) {
        if (id == null) return null;
        return em.find(Event.class, id);
    }

    /**
     * Recherche d'un événement avec verrouillage pessimiste d'écriture (PESSIMISTIC_WRITE)
     * pour garantir l'atomicité et l'intégrité lors des réservations concurrentes.
     */
    public Event findByIdWithLock(Long id) {
        if (id == null) return null;
        return em.find(Event.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<Event> findAll() {
        return em.createQuery("SELECT e FROM Event e JOIN FETCH e.category ORDER BY e.dateDebut ASC", Event.class)
                 .getResultList();
    }

    public List<Event> findByCategory(Long categoryId) {
        return em.createQuery(
                "SELECT e FROM Event e JOIN FETCH e.category WHERE e.category.id = :categoryId ORDER BY e.dateDebut ASC",
                Event.class)
                 .setParameter("categoryId", categoryId)
                 .getResultList();
    }

    public List<Event> findPublishedEvents() {
        return em.createQuery(
                "SELECT e FROM Event e JOIN FETCH e.category WHERE e.statut = :statut ORDER BY e.dateDebut ASC",
                Event.class)
                 .setParameter("statut", EventStatus.PUBLIE)
                 .getResultList();
    }

    public Event findPublishedById(long id) {
        return em.createQuery("SELECT e FROM Event e JOIN FETCH e.category WHERE e.id = :id AND e.statut = :statut", Event.class)
                .setParameter("id", id)
                .setParameter("statut", EventStatus.PUBLIE)
                .getSingleResultOrNull();
    }

    @Transactional
    public Event save(Event event) {
        em.persist(event);
        return event;
    }

    @Transactional
    public Event update(Event event) {
        return em.merge(event);
    }

    @Transactional
    public boolean delete(Long id) {
        Event event = findById(id);
        if (event != null) {
            em.remove(event);
            return true;
        }
        return false;
    }
}
