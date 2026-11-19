package com.eventbooking.service;

import com.eventbooking.BaseJpaTest;
import com.eventbooking.model.*;
import com.eventbooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

public class TicketServiceTest extends BaseJpaTest {

    private TicketService ticketService;
    private ReservationService reservationService;

    private TicketRepository ticketRepository;
    private ReservationRepository reservationRepository;
    private EventRepository eventRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    public void setup() {
        ticketRepository = new TicketRepository();
        reservationRepository = new ReservationRepository();
        eventRepository = new EventRepository();
        categoryRepository = new CategoryRepository();
        userRepository = new UserRepository();

        injectEntityManager(ticketRepository, em);
        injectEntityManager(reservationRepository, em);
        injectEntityManager(eventRepository, em);
        injectEntityManager(categoryRepository, em);
        injectEntityManager(userRepository, em);

        ticketService = new TicketService(ticketRepository);
        reservationService = new ReservationService(reservationRepository, eventRepository, userRepository);

        doInTransaction(() -> {
            testUser = new User("Alice Dupont", "alice" + System.nanoTime() + "@example.com", "secret", Role.CLIENT);
            userRepository.save(testUser);

            Category cat = categoryRepository.save(new Category("Cinéma", "Festivals du film"));
            testEvent = new Event(
                    "Avant-première Festival", "Projection exclusive",
                    LocalDateTime.now().plusDays(25), "Cinéma Le Grand Rex",
                    100, new BigDecimal("18.50"), cat
            );
            testEvent.setStatut(EventStatus.PUBLIE);
            eventRepository.save(testEvent);
        });
    }

    @Test
    @DisplayName("Devrait valider un ticket valide avec succès")
    public void testValiderTicketSucces() {
        doInTransaction(() -> {
            Reservation res = reservationService.reserver(testUser.getId(), testEvent.getId(), 1);
            Ticket ticket = res.getTickets().get(0);

            Ticket validated = ticketService.validerTicket(ticket.getCodeUnique());

            assertThat(validated.getStatut()).isEqualTo(TicketStatus.UTILISE);
            assertThat(validated.getDateUtilisation()).isNotNull();
        });
    }

    @Test
    @DisplayName("Devrait refuser la validation d'un ticket déjà utilisé")
    public void testRefusValidationTicketDejaUtilise() {
        doInTransaction(() -> {
            Reservation res = reservationService.reserver(testUser.getId(), testEvent.getId(), 1);
            Ticket ticket = res.getTickets().get(0);

            // Première validation
            ticketService.validerTicket(ticket.getCodeUnique());

            // Deuxième tentative de validation (doit échouer)
            assertThatThrownBy(() -> ticketService.validerTicket(ticket.getCodeUnique()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("déjà été utilisé");
        });
    }

    @Test
    @DisplayName("Devrait refuser la validation d'un ticket annulé")
    public void testRefusValidationTicketAnnule() {
        doInTransaction(() -> {
            Reservation res = reservationService.reserver(testUser.getId(), testEvent.getId(), 1);
            Ticket ticket = res.getTickets().get(0);

            // Annulation de la réservation
            reservationService.annulerReservation(res.getId());

            // Tentative de validation du ticket annulé
            assertThatThrownBy(() -> ticketService.validerTicket(ticket.getCodeUnique()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Validation refusée");
        });
    }

    @Test
    @DisplayName("Devrait refuser la validation d'un code inexistant")
    public void testRefusValidationCodeInexistant() {
        doInTransaction(() -> {
            assertThatThrownBy(() -> ticketService.validerTicket("CODE_INEXISTANT_12345"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("aucun ticket trouvé");
        });
    }
}
