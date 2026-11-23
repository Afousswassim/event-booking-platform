package com.eventbooking.repository;

import com.eventbooking.model.Event;
import com.eventbooking.model.Reservation;
import com.eventbooking.model.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Repository JPA pour la gestion des tickets numériques.
 */
@ApplicationScoped
public class TicketRepository {

    @PersistenceContext(unitName = "eventBookingPU")
    private EntityManager em;

    public Ticket findById(Long id) {
        if (id == null) return null;
        return em.find(Ticket.class, id);
    }

    public Ticket findByIdWithLock(Long id) {
        if (id == null) return null;
        return em.find(Ticket.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public Ticket findByCodeUnique(String codeUnique) {
        return em.createQuery(
                "SELECT t FROM Ticket t WHERE t.codeUnique = :codeUnique",
                Ticket.class)
                 .setParameter("codeUnique", codeUnique)
                 .setHint("jakarta.persistence.loadgraph", eventGraph())
                 .getSingleResultOrNull();
    }

    /**
     * Recherche d'un ticket avec verrouillage pessimiste pour empêcher deux agents
     * de valider simultanément le même ticket numérique.
     */
    public Ticket findByCodeUniqueWithLock(String codeUnique) {
        return em.createQuery(
                "SELECT t FROM Ticket t JOIN FETCH t.reservation WHERE t.codeUnique = :codeUnique",
                Ticket.class)
                 .setParameter("codeUnique", codeUnique)
                 .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                 .getSingleResultOrNull();
    }

    public List<Ticket> findByReservation(Long reservationId) {
        return em.createQuery(
                "SELECT t FROM Ticket t WHERE t.reservation.id = :reservationId ORDER BY t.id ASC",
                Ticket.class)
                 .setParameter("reservationId", reservationId)
                 .getResultList();
    }

    public List<Ticket> findByUser(long userId) {
        return em.createQuery(
                "SELECT t FROM Ticket t WHERE t.reservation.user.id = :userId ORDER BY t.id DESC",
                Ticket.class)
                .setParameter("userId", userId)
                .setHint("jakarta.persistence.loadgraph", eventGraph())
                .getResultList();
    }

    public Ticket findByIdForUser(long id, long userId) {
        return em.createQuery(
                "SELECT t FROM Ticket t WHERE t.id = :id AND t.reservation.user.id = :userId", Ticket.class)
                .setParameter("id", id)
                .setParameter("userId", userId)
                .setHint("jakarta.persistence.loadgraph", eventGraph())
                .getSingleResultOrNull();
    }

    private EntityGraph<Ticket> eventGraph() {
        var graph = em.createEntityGraph(Ticket.class);
        graph.addSubgraph("reservation", Reservation.class).addSubgraph("event", Event.class).addAttributeNodes("category");
        return graph;
    }

    @Transactional
    public Ticket save(Ticket ticket) {
        em.persist(ticket);
        return ticket;
    }

    @Transactional
    public Ticket update(Ticket ticket) {
        return em.merge(ticket);
    }
}
