package com.eventbooking.repository;

import com.eventbooking.model.Event;
import com.eventbooking.model.EventStatus;
import com.eventbooking.model.Reservation;
import com.eventbooking.model.ReservationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Repository JPA pour la gestion des réservations.
 */
@ApplicationScoped
public class ReservationRepository {

    @PersistenceContext(unitName = "eventBookingPU")
    private EntityManager em;

    public Reservation findById(Long id) {
        if (id == null) return null;
        return em.find(Reservation.class, id);
    }

    public List<Reservation> findByUser(Long userId) {
        var graph = em.createEntityGraph(Reservation.class);
        graph.addSubgraph("event", Event.class).addAttributeNodes("category");
        return em.createQuery(
                "SELECT r FROM Reservation r WHERE r.user.id = :userId ORDER BY r.dateReservation DESC",
                Reservation.class)
                 .setParameter("userId", userId)
                 .setHint("jakarta.persistence.loadgraph", graph)
                 .getResultList();
    }

    public Reservation findByIdForUser(long id, long userId, LockModeType lock) {
        var query = em.createQuery("SELECT r FROM Reservation r WHERE r.id = :id AND r.user.id = :userId",
                Reservation.class).setParameter("id", id).setParameter("userId", userId);
        if (lock == LockModeType.NONE) {
            var graph = em.createEntityGraph(Reservation.class);
            graph.addSubgraph("event", Event.class).addAttributeNodes("category");
            query.setHint("jakarta.persistence.loadgraph", graph);
        } else {
            query.setLockMode(lock);
        }
        return query.getSingleResultOrNull();
    }

    public List<Reservation> findByEvent(Long eventId) {
        return em.createQuery(
                "SELECT r FROM Reservation r JOIN FETCH r.user WHERE r.event.id = :eventId ORDER BY r.dateReservation DESC",
                Reservation.class)
                 .setParameter("eventId", eventId)
                 .getResultList();
    }

    /**
     * Calcule le nombre total de places réservées confirmées pour un événement donné.
     */
    public int countReservedPlaces(Long eventId) {
        Long sum = em.createQuery(
                "SELECT COALESCE(SUM(r.quantite), 0L) FROM Reservation r " +
                "WHERE r.event.id = :eventId AND r.statut = :statut",
                Long.class)
                     .setParameter("eventId", eventId)
                     .setParameter("statut", ReservationStatus.CONFIRMEE)
                     .getSingleResult();
        return sum != null ? sum.intValue() : 0;
    }

    public Map<Long, Long> countReservedPlacesForPublishedEvents() {
        var rows = em.createQuery(
                "SELECT r.event.id, SUM(r.quantite) FROM Reservation r " +
                "WHERE r.statut = :status AND r.event.statut = :eventStatus GROUP BY r.event.id",
                Object[].class)
                .setParameter("status", ReservationStatus.CONFIRMEE)
                .setParameter("eventStatus", EventStatus.PUBLIE)
                .getResultList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) counts.put((Long) row[0], (Long) row[1]);
        return counts;
    }

    @Transactional
    public Reservation save(Reservation reservation) {
        em.persist(reservation);
        return reservation;
    }

    @Transactional
    public Reservation update(Reservation reservation) {
        return em.merge(reservation);
    }
}
