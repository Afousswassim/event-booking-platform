package com.eventbooking.service;

import com.eventbooking.BaseJpaTest;
import com.eventbooking.model.Category;
import com.eventbooking.model.Event;
import com.eventbooking.model.EventStatus;
import com.eventbooking.repository.CategoryRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class EventServiceTest extends BaseJpaTest {

    private EventService eventService;
    private CategoryRepository categoryRepository;
    private EventRepository eventRepository;
    private ReservationRepository reservationRepository;

    @BeforeEach
    public void setup() {
        categoryRepository = new CategoryRepository();
        eventRepository = new EventRepository();
        reservationRepository = new ReservationRepository();

        injectEntityManager(categoryRepository, em);
        injectEntityManager(eventRepository, em);
        injectEntityManager(reservationRepository, em);

        eventService = new EventService(eventRepository, categoryRepository, reservationRepository);
    }

    @Test
    @DisplayName("Devrait créer une catégorie avec succès")
    public void testCreateCategory() {
        Category cat = new Category("Conférence", "Événements académiques et tech");

        doInTransaction(() -> {
            Category created = eventService.createCategory(cat);
            assertThat(created.getId()).isNotNull();
            assertThat(created.getNom()).isEqualTo("Conférence");
        });
    }

    @Test
    @DisplayName("Devrait rejeter une catégorie avec un nom vide")
    public void testCreateCategoryInvalidName() {
        Category cat = new Category("", "Description");

        assertThatThrownBy(() -> eventService.createCategory(cat))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom de la catégorie est obligatoire");
    }

    @Test
    @DisplayName("Devrait créer un événement en statut BROUILLON avec succès")
    public void testCreateEvent() {
        doInTransaction(() -> {
            Category cat = categoryRepository.save(new Category("Concert", "Musique"));
            Event event = new Event(
                    "Concert Jazz Live",
                    "Soirée Jazz",
                    LocalDateTime.now().plusDays(10),
                    "Théâtre Municipal",
                    150,
                    new BigDecimal("45.00"),
                    null
            );

            Event created = eventService.createEvent(event, cat.getId());
            assertThat(created.getId()).isNotNull();
            assertThat(created.getStatut()).isEqualTo(EventStatus.BROUILLON);
            assertThat(created.getCategory().getId()).isEqualTo(cat.getId());
        });
    }

    @Test
    @DisplayName("Devrait rejeter la création d'un événement avec un prix négatif ou une capacité <= 0")
    public void testCreateEventValidationErrors() {
        doInTransaction(() -> {
            Category cat = categoryRepository.save(new Category("Théâtre", "Pièces"));

            Event eventPrixNegatif = new Event(
                    "Festival", "Desc", LocalDateTime.now().plusDays(5),
                    "Salle", 100, new BigDecimal("-10.00"), null
            );
            assertThatThrownBy(() -> eventService.createEvent(eventPrixNegatif, cat.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("prix");

            Event eventCapaciteNulle = new Event(
                    "Festival", "Desc", LocalDateTime.now().plusDays(5),
                    "Salle", 0, new BigDecimal("20.00"), null
            );
            assertThatThrownBy(() -> eventService.createEvent(eventCapaciteNulle, cat.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacité");
        });
    }

    @Test
    @DisplayName("Devrait publier un événement et le rendre visible dans les événements publiés")
    public void testPublishEvent() {
        doInTransaction(() -> {
            Category cat = categoryRepository.save(new Category("Exposition", "Arts"));
            Event event = new Event(
                    "Salon de la Peinture", "Galerie d'art",
                    LocalDateTime.now().plusDays(20), "Centre Culturel",
                    80, new BigDecimal("15.00"), null
            );
            Event created = eventService.createEvent(event, cat.getId());

            Event published = eventService.publishEvent(created.getId());
            assertThat(published.getStatut()).isEqualTo(EventStatus.PUBLIE);

            List<Event> publishedList = eventService.getPublishedEvents();
            assertThat(publishedList).extracting(Event::getId).contains(published.getId());
        });
    }

    @Test
    @DisplayName("Devrait annuler un événement avec succès")
    public void testCancelEvent() {
        doInTransaction(() -> {
            Category cat = categoryRepository.save(new Category("Sport", "Compétition"));
            Event event = new Event(
                    "Tournoi de Tennis", "Tournoi régional",
                    LocalDateTime.now().plusDays(15), "Stade",
                    200, new BigDecimal("30.00"), null
            );
            Event created = eventService.createEvent(event, cat.getId());

            Event cancelled = eventService.cancelEvent(created.getId());
            assertThat(cancelled.getStatut()).isEqualTo(EventStatus.ANNULE);
        });
    }
}
