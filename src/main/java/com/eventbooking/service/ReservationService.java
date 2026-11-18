package com.eventbooking.service;

import com.eventbooking.model.*;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.ReservationRepository;
import com.eventbooking.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la gestion des réservations avec garantie de concurrence stricte
 * via verrouillage pessimiste au niveau de la base de données.
 */
@ApplicationScoped
public class ReservationService {

    @Inject
    private ReservationRepository reservationRepository;

    @Inject
    private EventRepository eventRepository;

    @Inject
    private UserRepository userRepository;

    public ReservationService() {
    }

    public ReservationService(ReservationRepository reservationRepository,
                              EventRepository eventRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Effectue une réservation de places pour un utilisateur et un événement donné.
     * Cette méthode est transactionnelle et utilise un verrouillage pessimiste d'écriture
     * sur l'événement pour interdire toute surréservation lors d'accès simultanés.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param eventId   Identifiant de l'événement
     * @param quantite  Nombre de places demandées
     * @return La réservation confirmée avec ses tickets générés
     */
    @Transactional
    public Reservation reserver(Long userId, Long eventId, int quantite) {
        // 1. Validation des paramètres
        if (userId == null) {
            throw new IllegalArgumentException("L'identifiant de l'utilisateur est obligatoire.");
        }
        if (eventId == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantité de places réservées doit être strictement positive.");
        }

        // 2. Vérification de l'existence de l'utilisateur (contrat avec Développeur 1)
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable avec l'identifiant : " + userId);
        }

        // 3. Acquisition du verrou pessimiste d'écriture (PESSIMISTIC_WRITE) sur l'événement
        Event event = eventRepository.findByIdWithLock(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Événement introuvable avec l'identifiant : " + eventId);
        }

        // 4. Vérification de l'état et de la date de l'événement sous verrou
        if (event.getStatut() != EventStatus.PUBLIE) {
            throw new IllegalStateException("Impossible de réserver : l'événement n'est pas ouvert aux réservations (statut actuel : " + event.getStatut() + ").");
        }
        if (event.getDateDebut().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de réserver : la date de l'événement est déjà passée.");
        }

        // 5. Calcul des places déjà réservées et vérification de la capacité restante
        int placesReservees = reservationRepository.countReservedPlaces(eventId);
        int placesDisponibles = event.getCapacite() - placesReservees;

        if (quantite > placesDisponibles) {
            throw new IllegalStateException(
                "Capacité insuffisante : seulement " + placesDisponibles +
                " place(s) disponible(s) alors que " + quantite + " ont été demandées."
            );
        }

        // 6. Création de la réservation
        Reservation reservation = new Reservation(user, event, quantite);

        // 7. Génération des tickets numériques uniques (un par place demandée)
        for (int i = 0; i < quantite; i++) {
            String codeUnique = UUID.randomUUID().toString();
            Ticket ticket = new Ticket(codeUnique, reservation);
            reservation.addTicket(ticket);
        }

        // 8. Enregistrement en base (les tickets sont persistés en cascade)
        return reservationRepository.save(reservation);
    }

    /**
     * Annule une réservation existante et invalide ses tickets non utilisés.
     */
    @Transactional
    public Reservation annulerReservation(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("L'identifiant de réservation est obligatoire.");
        }

        Reservation reservation = reservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Réservation introuvable avec l'identifiant : " + reservationId);
        }

        if (reservation.getStatut() == ReservationStatus.ANNULEE) {
            throw new IllegalStateException("Cette réservation est déjà annulée.");
        }

        if (reservation.getEvent().getDateDebut().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible d'annuler une réservation pour un événement déjà passé.");
        }

        // Mettre à jour le statut de la réservation
        reservation.setStatut(ReservationStatus.ANNULEE);

        // Invalider les tickets associés qui n'ont pas encore été utilisés
        if (reservation.getTickets() != null) {
            for (Ticket ticket : reservation.getTickets()) {
                if (ticket.getStatut() == TicketStatus.VALIDE) {
                    ticket.setStatut(TicketStatus.ANNULE);
                }
            }
        }

        return reservationRepository.update(reservation);
    }

    public List<Reservation> getReservationsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("L'identifiant de l'utilisateur est obligatoire.");
        }
        return reservationRepository.findByUser(userId);
    }

    public List<Reservation> getReservationsByEvent(Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return reservationRepository.findByEvent(eventId);
    }

    public Reservation getReservationById(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("L'identifiant de la réservation est obligatoire.");
        }
        return reservationRepository.findById(reservationId);
    }
}
