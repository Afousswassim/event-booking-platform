package com.eventbooking.controller;

import com.eventbooking.model.Event;
import com.eventbooking.service.EventService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@WebServlet({"", "/events/*"})
public class EventServlet extends HttpServlet {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    private EventService events;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        if (request.getServletPath().isEmpty() || path == null || path.equals("/")) {
            showCatalogue(request, response);
            return;
        }
        Long id = eventId(path);
        Event event = id == null ? null : events.getPublishedEvent(id);
        if (event == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        request.setAttribute("event", event == null ? null : toCard(event));
        request.getRequestDispatcher("/WEB-INF/views/event.jsp").forward(request, response);
    }

    private void showCatalogue(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String input = Objects.toString(request.getParameter("q"), "").strip();
        String query = input.length() > 120 ? input.substring(0, 120) : input;
        String category = Objects.toString(request.getParameter("category"), "");
        String search = query.toLowerCase(Locale.ROOT);
        List<EventCard> catalogue = events.getPublishedEvents().stream().map(EventServlet::toCard).toList();
        List<String> categories = catalogue.stream().map(EventCard::category).distinct().sorted().toList();
        request.setAttribute("catalogueReady", true);
        request.setAttribute("query", query);
        request.setAttribute("category", category);
        request.setAttribute("categories", categories);
        request.setAttribute("featured", catalogue.isEmpty() ? null : catalogue.getFirst());
        request.setAttribute("events", catalogue.stream().filter(event -> event.matches(search, category)).toList());
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    private static Long eventId(String path) {
        if (!path.matches("/[1-9][0-9]{0,18}")) return null;
        try {
            return Long.parseLong(path.substring(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static EventCard toCard(Event event) {
        String category = event.getCategory().getNom();
        String name = event.getTitre().toLowerCase(Locale.ROOT);
        String theme = category.toLowerCase(Locale.ROOT);
        String image = "hero-bg.jpg";
        if (name.equals("atlas afterdark")) image = "atlas-afterdark.jpg";
        else if (theme.contains("musique") || theme.contains("concert")) image = "event-music.jpg";
        else if (theme.contains("art") || theme.contains("culture")) image = "event-art.jpg";
        else if (theme.contains("tech") || theme.contains("conférence")) image = "event-tech.jpg";
        String price = event.getPrix().signum() == 0 ? "Gratuit" : NumberFormat.getNumberInstance(Locale.FRENCH).format(event.getPrix()) + " MAD";
        var starts = event.getDateDebut();
        return new EventCard(event.getId(), event.getTitre(), event.getDescription(), event.getLieu(), category,
                starts.toString(), DATE.format(starts), TIME.format(starts), price, image);
    }

    public record EventCard(long id, String title, String description, String venue, String category,
                            String start, String date, String time, String price, String image) {
        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getVenue() { return venue; }
        public String getCategory() { return category; }
        public String getStart() { return start; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getPrice() { return price; }
        public String getImage() { return image; }

        boolean matches(String query, String selectedCategory) {
            return (selectedCategory.isEmpty() || category.equals(selectedCategory))
                    && (title.toLowerCase(Locale.ROOT).contains(query) || venue.toLowerCase(Locale.ROOT).contains(query)
                    || category.toLowerCase(Locale.ROOT).contains(query));
        }
    }
}
