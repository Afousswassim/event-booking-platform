package com.eventbooking.service;

import com.eventbooking.model.Reservation;
import com.eventbooking.model.ReservationStatus;
import com.eventbooking.model.Ticket;
import com.eventbooking.model.TicketStatus;
import com.eventbooking.repository.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la gestion et la validation sécurisée des tickets numériques.
 */
@ApplicationScoped
public class TicketService {

    @Inject
    private TicketRepository ticketRepository;

    public TicketService() {
    }

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Crée un nouveau ticket avec un code UUID unique associé à une réservation.
     */
    @Transactional
    public Ticket createTicket(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("La réservation associée est obligatoire.");
        }
        String codeUnique = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(codeUnique, reservation);
        return ticketRepository.save(ticket);
    }

    public Ticket getTicketById(Long id) {
        if (id == null) return null;
        return ticketRepository.findById(id);
    }

    public Ticket getTicketByCode(String codeUnique) {
        if (codeUnique == null || codeUnique.trim().isEmpty()) {
            throw new IllegalArgumentException("Le code du ticket est obligatoire.");
        }
        return ticketRepository.findByCodeUnique(codeUnique.trim());
    }

    public List<Ticket> getTicketsByReservation(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("L'identifiant de réservation est obligatoire.");
        }
        return ticketRepository.findByReservation(reservationId);
    }

    /**
     * Valide un ticket lors de l'entrée à l'événement.
     * Cette méthode acquiert un verrou pessimiste d'écriture pour interdire formellement
     * que deux agents scannent et valident simultanément le même code de ticket.
     *
     * @param codeUnique Le code unique du ticket (lu par le lecteur QR Code)
     * @return Le ticket mis à jour avec le statut UTILISE et sa date d'utilisation
     */
    @Transactional
    public Ticket validerTicket(String codeUnique) {
        if (codeUnique == null || codeUnique.trim().isEmpty()) {
            throw new IllegalArgumentException("Le code du ticket ne peut pas être vide.");
        }

        // Acquisition d'un verrou pessimiste pour empêcher la double validation concurrente
        Ticket ticket = ticketRepository.findByCodeUniqueWithLock(codeUnique.trim());
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket invalide : aucun ticket trouvé avec le code " + codeUnique);
        }

        // Vérification de la réservation associée
        if (ticket.getReservation() != null && ticket.getReservation().getStatut() == ReservationStatus.ANNULEE) {
            throw new IllegalStateException("Validation refusée : la réservation associée à ce ticket a été annulée.");
        }

        // Vérification du statut actuel du ticket
        if (ticket.getStatut() == TicketStatus.UTILISE) {
            throw new IllegalStateException(
                "Validation refusée : ce ticket a déjà été utilisé le " + ticket.getDateUtilisation()
            );
        }

        if (ticket.getStatut() == TicketStatus.ANNULE) {
            throw new IllegalStateException("Validation refusée : ce ticket a été annulé.");
        }

        // Validation réussie : passage à UTILISE et horodatage
        ticket.setStatut(TicketStatus.UTILISE);
        ticket.setDateUtilisation(LocalDateTime.now());

        return ticketRepository.update(ticket);
    }
}
