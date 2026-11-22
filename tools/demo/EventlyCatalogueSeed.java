import com.eventbooking.model.Category;
import com.eventbooking.model.Event;
import com.eventbooking.model.EventStatus;
import com.eventbooking.service.EventService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Singleton
@Startup
public class EventlyCatalogueSeed {
    private static final System.Logger LOG = System.getLogger(EventlyCatalogueSeed.class.getName());
    private static final String DEMO = " — Démo Evently";
    private static final List<Sample> SAMPLES = List.of(
            new Sample("Sessions de l’Atlantique", "Musique", "Cour de la Fabrique culturelle, Casablanca",
                    "Concert acoustique mêlant guitare, oud et percussions. Ouverture des portes trente minutes avant le premier set.",
                    5, 20, 0, 180, "140.00"),
            new Sample("Relais des remparts", "Sport", "Esplanade de Bab Jdid, Marrakech",
                    "Relais urbain de 5 km par équipe de deux, avec échauffement encadré et ravitaillement à l’arrivée.",
                    13, 9, 0, 120, "85.00"),
            new Sample("Rabat Product Lab", "Technologie", "Technopark, Rabat",
                    "Une matinée de démonstrations et d’ateliers sur le prototypage, l’accessibilité et la conception de produits numériques.",
                    18, 10, 30, 140, "0.00"),
            new Sample("Regards sur la médina", "Art & culture", "Galerie de la médina, Fès",
                    "Visite commentée d’une exposition photo suivie d’un échange sur les métiers et les lieux de la médina.",
                    25, 17, 0, 75, "110.00"),
            new Sample("Les tables du Souss", "Gastronomie", "Espace culinaire du front de mer, Agadir",
                    "Atelier en petits groupes et dégustation autour des produits du Souss, avec une option végétarienne sur place.",
                    36, 12, 30, 60, "320.00")
    );

    @Inject
    private EventService events;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void seed() {
        int before = events.getPublishedEvents().size();
        var existing = events.getAllEvents();
        Map<String, Category> categories = new HashMap<>();
        for (Category category : events.getAllCategories()) {
            categories.putIfAbsent(category.getNom().toLowerCase(java.util.Locale.ROOT), category);
        }
        var today = LocalDate.now(ZoneId.of("Africa/Casablanca"));
        int created = 0;
        int published = 0;

        for (Sample sample : SAMPLES) {
            var title = sample.title() + DEMO;
            var event = existing.stream().filter(item -> item.getTitre().equalsIgnoreCase(title)
                    && item.getLieu().equalsIgnoreCase(sample.venue())).findFirst().orElse(null);
            if (event == null) {
                var category = categories.computeIfAbsent(sample.category().toLowerCase(java.util.Locale.ROOT),
                        key -> events.createCategory(new Category(sample.category(), null)));
                event = events.createEvent(new Event(title, sample.description(), today.plusDays(sample.days())
                        .atTime(LocalTime.of(sample.hour(), sample.minute())), sample.venue(), sample.capacity(),
                        new BigDecimal(sample.price()), category), category.getId());
                created++;
            }
            if (event.getStatut() == EventStatus.BROUILLON) {
                events.publishEvent(event.getId());
                published++;
            }
            LOG.log(System.Logger.Level.INFO, "Evently seed #{0} {1}", event.getId(), title);
        }
        LOG.log(System.Logger.Level.INFO, "Evently seed: {0} published before, {1} created, {2} published now, {3} published total",
                before, created, published, events.getPublishedEvents().size());
    }

    private record Sample(String title, String category, String venue, String description,
                          int days, int hour, int minute, int capacity, String price) {}
}
