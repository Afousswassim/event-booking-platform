package com.eventbooking.service;

import com.eventbooking.BaseJpaTest;
import com.eventbooking.model.*;
import com.eventbooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ReservationServiceTest extends BaseJpaTest {

    private ReservationService reservationService;
    private EventService eventService;

    private ReservationRepository reservationRepository;
    private EventRepository eventRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    public void setup() {
        reservationRepository = new ReservationRepository();
        eventRepository = new EventRepository();
        categoryRepository = new CategoryRepository();
        userRepository = new UserRepository();

        injectEntityManager(reservationRepository, em);
        injectEntityManager(eventRepository, em);
        injectEntityManager(categoryRepository, em);
        injectEntityManager(userRepository, em);

        reservationService = new ReservationService(reservationRepository, eventRepository, userRepository);
        eventService = new EventService(eventRepository, categoryRepository, reservationRepository);

        doInTransaction(() -> {
            testUser = new User("Wassim Dupont", "wassim" + System.nanoTime() + "@example.com", "hashed_pass", Role.CLIENT);
            userRepository.save(testUser);

            Category cat = categoryRepository.save(new Category("Spectacle", "Théâtre et Danse"));
            testEvent = new Event(
                    "Grand Spectacle 2026", "Description",
                    LocalDateTime.now().plusDays(30), "Palais des Congrès",
                    50, new BigDecimal("25.00"), cat
            );
            testEvent.setStatut(EventStatus.PUBLIE);
            eventRepository.save(testEvent);
        });
    }

    @Test
    @DisplayName("Devrait créer une réservation valide et générer un ticket par place réservée")
    public void testReserverValid() {
        doInTransaction(() -> {
            Reservation res = reservationService.reserver(testUser.getId(), testEvent.getId(), 3);

            assertThat(res.getId()).isNotNull();
            assertThat(res.getStatut()).isEqualTo(ReservationStatus.CONFIRMEE);
            assertThat(res.getQuantite()).isEqualTo(3);
            assertThat(res.getTickets()).hasSize(3);

            // Vérification de l'unicité des codes tickets
            long distinctCodes = res.getTickets().stream()
                    .map(Ticket::getCodeUnique)
                    .distinct()
                    .count();
            assertThat(distinctCodes).isEqualTo(3);

            for (Ticket ticket : res.getTickets()) {
                assertThat(ticket.getStatut()).isEqualTo(TicketStatus.VALIDE);
                assertThat(ticket.getCodeUnique()).isNotBlank();
            }

            // Vérification de la mise à jour des places disponibles
            int placesRestantes = eventService.getAvailablePlaces(testEvent.getId());
            assertThat(placesRestantes).isEqualTo(47); // 50 - 3
        });
    }

    @Test
    @DisplayName("Devrait rejeter une réservation avec une quantité négative ou nulle")
    public void testReserverQuantiteInvalide() {
        assertThatThrownBy(() -> reservationService.reserver(testUser.getId(), testEvent.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantité");

        assertThatThrownBy(() -> reservationService.reserver(testUser.getId(), testEvent.getId(), -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantité");
    }

    @Test
    @DisplayName("Devrait refuser une réservation dépassant la capacité disponible")
    public void testReserverDepassantCapacite() {
        doInTransaction(() -> {
            assertThatThrownBy(() -> reservationService.reserver(testUser.getId(), testEvent.getId(), 51))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Capacité insuffisante");
        });
    }

    @Test
    @DisplayName("Devrait refuser une réservation si l'événement n'est pas publié")
    public void testReserverEvenementNonPublie() {
        doInTransaction(() -> {
            Category cat = categoryRepository.save(new Category("Tech", "Tech talks"));
            Event draft = new Event(
                    "Conférence Secrète", "Desc",
                    LocalDateTime.now().plusDays(10), "Salle A",
                    100, new BigDecimal("10.00"), cat
            );
            draft.setStatut(EventStatus.BROUILLON);
            eventRepository.save(draft);

            assertThatThrownBy(() -> reservationService.reserver(testUser.getId(), draft.getId(), 2))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pas ouvert aux réservations");
        });
    }

    @Test
    @DisplayName("Devrait refuser une réservation si la date de l'événement est passée")
    public void testReserverEvenementDatePassee() {
        doInTransaction(() -> {
            Category cat = categoryRepository.save(new Category("Passé", "Archives"));
            Event pastEvent = new Event(
                    "Concert Hier", "Desc",
                    LocalDateTime.now().minusDays(1), "Salle B",
                    100, new BigDecimal("10.00"), cat
            );
            pastEvent.setStatut(EventStatus.PUBLIE);
            eventRepository.save(pastEvent);

            assertThatThrownBy(() -> reservationService.reserver(testUser.getId(), pastEvent.getId(), 2))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("date de l'événement est déjà passée");
        });
    }

    @Test
    @DisplayName("Devrait annuler une réservation et invalider ses tickets associés")
    public void testAnnulerReservation() {
        doInTransaction(() -> {
            Reservation res = reservationService.reserver(testUser.getId(), testEvent.getId(), 2);
            assertThat(eventService.getAvailablePlaces(testEvent.getId())).isEqualTo(48);

            Reservation cancelled = reservationService.annulerReservation(res.getId());
            assertThat(cancelled.getStatut()).isEqualTo(ReservationStatus.ANNULEE);

            for (Ticket ticket : cancelled.getTickets()) {
                assertThat(ticket.getStatut()).isEqualTo(TicketStatus.ANNULE);
            }

            // Les places doivent être libérées
            int placesRestantes = eventService.getAvailablePlaces(testEvent.getId());
            assertThat(placesRestantes).isEqualTo(50);
        });
    }
}
