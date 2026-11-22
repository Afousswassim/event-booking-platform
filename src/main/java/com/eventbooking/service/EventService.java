package com.eventbooking.service;

import com.eventbooking.model.Category;
import com.eventbooking.model.Event;
import com.eventbooking.model.EventStatus;
import com.eventbooking.repository.CategoryRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.ReservationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Service métier pour la gestion des catégories et des événements.
 */
@ApplicationScoped
public class EventService {

    @Inject
    private EventRepository eventRepository;

    @Inject
    private CategoryRepository categoryRepository;

    @Inject
    private ReservationRepository reservationRepository;

    public EventService() {
    }

    public EventService(EventRepository eventRepository,
                        CategoryRepository categoryRepository,
                        ReservationRepository reservationRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.reservationRepository = reservationRepository;
    }

    // ==========================================
    // Gestion des Catégories
    // ==========================================

    @Transactional
    public Category createCategory(Category category) {
        if (category == null || category.getNom() == null || category.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire.");
        }
        category.setNom(category.getNom().trim());
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // ==========================================
    // Gestion des Événements
    // ==========================================

    @Transactional
    public Event createEvent(Event event, Long categoryId) {
        if (event == null) {
            throw new IllegalArgumentException("L'événement ne peut pas être nul.");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("L'identifiant de la catégorie est obligatoire.");
        }
        Category category = categoryRepository.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Catégorie introuvable avec l'identifiant : " + categoryId);
        }

        validateEventData(event);
        event.setCategory(category);
        if (event.getStatut() == null) {
            event.setStatut(EventStatus.BROUILLON);
        }

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Event event) {
        if (event == null || event.getId() == null) {
            throw new IllegalArgumentException("L'événement et son identifiant sont obligatoires pour la mise à jour.");
        }
        Event existing = eventRepository.findById(event.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Événement introuvable avec l'identifiant : " + event.getId());
        }

        if (existing.getStatut() == EventStatus.TERMINE || existing.getStatut() == EventStatus.ANNULE) {
            throw new IllegalStateException("Impossible de modifier un événement terminé ou annulé.");
        }

        validateEventData(event);

        existing.setTitre(event.getTitre());
        existing.setDescription(event.getDescription());
        existing.setDateDebut(event.getDateDebut());
        existing.setLieu(event.getLieu());
        existing.setCapacite(event.getCapacite());
        existing.setPrix(event.getPrix());
        if (event.getCategory() != null) {
            existing.setCategory(event.getCategory());
        }

        return eventRepository.update(existing);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public List<Event> getEventsByCategory(Long categoryId) {
        return eventRepository.findByCategory(categoryId);
    }

    public List<Event> getPublishedEvents() {
        return eventRepository.findPublishedEvents();
    }

    public Map<Long, Integer> getAvailablePlacesForPublishedEvents(List<Event> published) {
        if (published.isEmpty()) return Map.of();
        var reserved = reservationRepository.countReservedPlacesForPublishedEvents();
        var now = LocalDateTime.now();
        Map<Long, Integer> available = new HashMap<>();
        for (Event event : published) {
            long remaining = event.getCapacite() - reserved.getOrDefault(event.getId(), 0L);
            available.put(event.getId(), event.getDateDebut().isAfter(now) ? (int) Math.max(0, remaining) : 0);
        }
        return available;
    }

    public Event getPublishedEvent(long id) {
        return eventRepository.findPublishedById(id);
    }

    @Transactional
    public Event publishEvent(Long eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Événement introuvable avec l'identifiant : " + eventId);
        }
        if (event.getStatut() == EventStatus.ANNULE) {
            throw new IllegalStateException("Impossible de publier un événement annulé.");
        }
        if (event.getStatut() == EventStatus.TERMINE) {
            throw new IllegalStateException("Impossible de publier un événement terminé.");
        }
        if (event.getDateDebut().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible de publier un événement dont la date est déjà passée.");
        }
        if (event.getCapacite() <= 0) {
            throw new IllegalStateException("Impossible de publier un événement avec une capacité nulle ou négative.");
        }

        event.setStatut(EventStatus.PUBLIE);
        return eventRepository.update(event);
    }

    @Transactional
    public Event cancelEvent(Long eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Événement introuvable avec l'identifiant : " + eventId);
        }
        if (event.getStatut() == EventStatus.TERMINE) {
            throw new IllegalStateException("Impossible d'annuler un événement déjà terminé.");
        }
        event.setStatut(EventStatus.ANNULE);
        return eventRepository.update(event);
    }

    /**
     * Calcule le nombre de places restantes disponibles à la réservation pour un événement.
     */
    public int getAvailablePlaces(Long eventId) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Événement introuvable avec l'identifiant : " + eventId);
        }
        if (event.getStatut() != EventStatus.PUBLIE) {
            return 0;
        }
        int reserved = reservationRepository.countReservedPlaces(eventId);
        return Math.max(0, event.getCapacite() - reserved);
    }

    @Transactional
    public boolean deleteEvent(Long id) {
        int reserved = reservationRepository.countReservedPlaces(id);
        if (reserved > 0) {
            throw new IllegalStateException("Impossible de supprimer un événement possédant des réservations confirmées.");
        }
        return eventRepository.delete(id);
    }

    private void validateEventData(Event event) {
        if (event.getTitre() == null || event.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de l'événement est obligatoire.");
        }
        if (event.getLieu() == null || event.getLieu().trim().isEmpty()) {
            throw new IllegalArgumentException("Le lieu de l'événement est obligatoire.");
        }
        if (event.getDateDebut() == null) {
            throw new IllegalArgumentException("La date de début de l'événement est obligatoire.");
        }
        if (event.getCapacite() <= 0) {
            throw new IllegalArgumentException("La capacité de l'événement doit être strictement positive.");
        }
        if (event.getPrix() == null || event.getPrix().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix de l'événement doit être supérieur ou égal à zéro.");
        }
    }
}
