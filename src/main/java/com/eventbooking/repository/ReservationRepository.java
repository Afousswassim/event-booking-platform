package com.eventbooking.repository;

import com.eventbooking.model.Reservation;
import com.eventbooking.model.ReservationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

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
        return em.createQuery(
                "SELECT r FROM Reservation r JOIN FETCH r.event e JOIN FETCH e.category " +
                "WHERE r.user.id = :userId ORDER BY r.dateReservation DESC",
                Reservation.class)
                 .setParameter("userId", userId)
                 .getResultList();
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
