package com.eventbooking.controller;

import com.eventbooking.model.Role;
import com.eventbooking.security.AuthSession;
import com.eventbooking.service.EventService;
import com.eventbooking.service.ReservationService;
import com.eventbooking.service.UserService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.UUID;

@WebServlet("/my-reservations")
public class ReservationServlet extends HttpServlet {
    private static final String HISTORY = "/WEB-INF/views/reservations.jsp";
    private static final String BOOKING = "/WEB-INF/views/booking.jsp";
    private static final String STATE = ReservationServlet.class.getName() + ".checkouts";
    private static final int MAX_TICKETS = 8;

    @Inject
    private ReservationService reservations;

    @Inject
    private EventService events;

    @Inject
    private UserService users;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        var session = request.getSession(false);
        var client = AuthSession.identity(session);
        String confirmation = request.getParameter("confirmation");
        String event = request.getParameter("eventId");
        if (confirmation != null) {
            Checkout checkout;
            synchronized (client) {
                checkout = checkouts(session).get(confirmation);
            }
            if (checkout == null || checkout.receipt() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("bookingError", "Cette confirmation a expiré. Retrouvez vos réservations dans votre espace.");
            } else {
                request.setAttribute("receipt", checkout.receipt());
            }
            request.getRequestDispatcher(BOOKING).forward(request, response);
        } else if (event != null) {
            showCheckout(request, response, positiveId(event), 1, null, HttpServletResponse.SC_OK);
        } else {
            request.setAttribute("reservations", reservations.getReservationsByUser(client.id()));
            request.getRequestDispatcher(HISTORY).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        var session = request.getSession(false);
        var client = AuthSession.identity(session);
        var token = request.getParameter("checkoutToken");
        Long eventId = positiveId(request.getParameter("eventId"));
        Long count = positiveId(request.getParameter("quantity"));
        int quantity = count != null && count <= MAX_TICKETS ? count.intValue() : 1;

        // All requests for this login share the identity object. A replay sees the stored receipt.
        synchronized (client) {
            var drafts = checkouts(session);
            var checkout = drafts.get(token);
            if (checkout == null || !Long.valueOf(checkout.eventId()).equals(eventId)) {
                showCheckout(request, response, eventId, quantity, "Cette session de réservation a expiré. Vérifiez les informations avant de confirmer.", 409);
                return;
            }
            if (checkout.receipt() != null) {
                redirectToReceipt(request, response, token);
                return;
            }
            if (count == null || count > MAX_TICKETS || !"yes".equals(request.getParameter("confirmed"))) {
                showCheckout(request, response, eventId, quantity, "Choisissez entre 1 et 8 billets et cochez la case de confirmation.", 400);
                return;
            }
            if (clientProfile(client.id()) == null) {
                showCheckout(request, response, eventId, quantity, null, 403);
                return;
            }
            try {
                var reservation = reservations.reserver(client.id(), eventId, quantity, checkout.price());
                var receipt = new Receipt(reservation.getId(), checkout.title(), quantity,
                        amount(checkout.price().multiply(BigDecimal.valueOf(quantity))));
                drafts.put(token, new Checkout(eventId, checkout.title(), checkout.price(), receipt));
            } catch (IllegalStateException | IllegalArgumentException unavailable) {
                showCheckout(request, response, eventId, quantity,
                        "La disponibilité ou le prix a changé. Vérifiez votre sélection avant de confirmer.", 409);
                return;
            }
            redirectToReceipt(request, response, token);
        }
    }

    private void showCheckout(HttpServletRequest request, HttpServletResponse response, Long eventId,
                              int quantity, String error, int status) throws ServletException, IOException {
        var event = eventId == null ? null : events.getPublishedEvent(eventId);
        var session = request.getSession(false);
        var client = AuthSession.identity(session);
        if (event == null) {
            status = 404;
            error = "Cet événement est introuvable ou n’est plus publié.";
        } else if (!event.getDateDebut().isAfter(LocalDateTime.now())) {
            status = 409;
            error = "Les réservations sont closes pour cet événement.";
        } else {
            int remaining = events.getAvailablePlaces(eventId);
            request.setAttribute("event", EventServlet.toCard(event, remaining));
            var profile = clientProfile(client.id());
            if (remaining == 0) {
                status = 409;
                error = "Cet événement est complet.";
            } else if (profile == null) {
                status = 403;
                error = "Un compte client avec un nom et une adresse email valides est nécessaire.";
            } else {
                String token;
                synchronized (client) {
                    var drafts = checkouts(session);
                    token = drafts.entrySet().stream()
                            .filter(entry -> entry.getValue().matches(eventId, event.getPrix()))
                            .map(java.util.Map.Entry::getKey).findFirst().orElse(null);
                    if (token == null) {
                        if (drafts.size() >= 32) drafts.pollFirstEntry();
                        token = UUID.randomUUID().toString();
                        drafts.put(token, new Checkout(eventId, event.getTitre(), event.getPrix(), null));
                    }
                }
                int maximum = Math.min(MAX_TICKETS, remaining);
                int selected = Math.min(quantity, maximum);
                request.setAttribute("checkoutToken", token);
                request.setAttribute("csrfToken", AuthSession.csrfToken(session));
                request.setAttribute("clientName", profile.nom());
                request.setAttribute("clientEmail", profile.email());
                request.setAttribute("maximum", maximum);
                request.setAttribute("quantity", selected);
                request.setAttribute("unitPrice", amount(event.getPrix()));
                request.setAttribute("unitCents", event.getPrix().movePointRight(2).longValueExact());
                request.setAttribute("total", amount(event.getPrix().multiply(BigDecimal.valueOf(selected))));
            }
        }
        response.setStatus(status);
        request.setAttribute("bookingError", error);
        request.getRequestDispatcher(BOOKING).forward(request, response);
    }

    private UserService.Profile clientProfile(long id) {
        try {
            var profile = users.profile(id);
            return profile.role() == Role.CLIENT && profile.nom() != null && !profile.nom().isBlank()
                    && profile.email() != null && profile.email().contains("@") ? profile : null;
        } catch (UserService.Failure missing) {
            return null;
        }
    }

    private static CheckoutState checkouts(HttpSession session) {
        if (session.getAttribute(STATE) instanceof CheckoutState state) return state;
        var state = new CheckoutState();
        session.setAttribute(STATE, state);
        return state;
    }

    private static void redirectToReceipt(HttpServletRequest request, HttpServletResponse response, String token) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", request.getContextPath() + "/my-reservations?confirmation=" + token);
    }

    static Long positiveId(String input) {
        if (input == null || !input.matches("[1-9][0-9]{0,18}")) return null;
        try {
            return Long.valueOf(input);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static String amount(BigDecimal price) {
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(price) + " DH";
    }

    private static final class CheckoutState extends LinkedHashMap<String, Checkout> {
        private static final long serialVersionUID = 1L;
    }

    private record Checkout(long eventId, String title, BigDecimal price, Receipt receipt) implements Serializable {
        boolean matches(long event, BigDecimal amount) {
            return receipt == null && eventId == event && price.compareTo(amount) == 0;
        }
    }

    public record Receipt(long id, String title, int quantity, String total) implements Serializable {
        public long getId() { return id; }
        public String getTitle() { return title; }
        public int getQuantity() { return quantity; }
        public String getTotal() { return total; }
    }
}
