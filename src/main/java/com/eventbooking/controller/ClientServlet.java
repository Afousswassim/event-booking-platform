package com.eventbooking.controller;

import com.eventbooking.model.EventStatus;
import com.eventbooking.model.Reservation;
import com.eventbooking.model.ReservationStatus;
import com.eventbooking.model.Role;
import com.eventbooking.model.TicketStatus;
import com.eventbooking.security.AuthSession;
import com.eventbooking.service.ReservationService;
import com.eventbooking.service.TicketService;
import com.eventbooking.service.UserService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;

@WebServlet({"/client", "/client/profile"})
public class ClientServlet extends HttpServlet {
    private static final String PROFILE = "/WEB-INF/views/auth/client-profile.jsp";
    private static final String PROFILE_UPDATED = ClientServlet.class.getName() + ".profileUpdated";

    @Inject
    private UserService users;

    @Inject
    private ReservationService reservations;

    @Inject
    private TicketService tickets;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        var session = request.getSession(false);
        var identity = AuthSession.identity(session);
        try {
            var profile = users.profile(identity.id());
            if (profile.role() != Role.CLIENT) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            AuthSession.csrfToken(session);
            request.setAttribute("clientName", profile.nom());
            if ("/client/profile".equals(request.getServletPath())) {
                request.setAttribute("clientEmail", profile.email());
                request.setAttribute("profileUpdated", session.getAttribute(PROFILE_UPDATED));
                session.removeAttribute(PROFILE_UPDATED);
                request.getRequestDispatcher(PROFILE).forward(request, response);
            } else {
                showDashboard(request, response, identity.id());
            }
        } catch (UserService.Failure failure) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (!"/client/profile".equals(request.getServletPath())) {
            response.setHeader("Allow", "GET, HEAD");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        var session = request.getSession(false);
        var identity = AuthSession.identity(session);
        String name = request.getParameter("nom");
        String email = request.getParameter("email");
        try {
            var updated = users.updateProfile(identity.id(), name, email, request.getParameter("currentPassword"));
            session.setAttribute(AuthServlet.USER_NAME, updated.nom());
            session.setAttribute(PROFILE_UPDATED, true);
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            response.setHeader("Location", request.getContextPath() + "/client/profile");
        } catch (UserService.Failure failure) {
            if (failure.problem() == UserService.Problem.ACCOUNT_UNAVAILABLE) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            String error = switch (failure.problem()) {
                case INVALID_NAME -> "Saisissez un nom valide.";
                case INVALID_EMAIL -> "Saisissez une adresse email valide.";
                case EMAIL_TAKEN -> "Cette adresse email est déjà utilisée.";
                case INVALID_PASSWORD, INVALID_CREDENTIALS -> "Le mot de passe actuel est incorrect.";
                case ACCOUNT_UNAVAILABLE -> throw new IllegalStateException("Account unavailable");
            };
            response.setStatus(switch (failure.problem()) {
                case EMAIL_TAKEN -> HttpServletResponse.SC_CONFLICT;
                case INVALID_CREDENTIALS -> HttpServletResponse.SC_UNAUTHORIZED;
                default -> HttpServletResponse.SC_BAD_REQUEST;
            });
            request.setAttribute("clientName", name);
            request.setAttribute("clientEmail", email);
            request.setAttribute("profileError", error);
            request.getRequestDispatcher(PROFILE).forward(request, response);
        }
    }

    private void showDashboard(HttpServletRequest request, HttpServletResponse response, long clientId)
            throws IOException, ServletException {
        var now = LocalDateTime.now();
        var ownReservations = reservations.getReservationsByUser(clientId);
        var ownTickets = tickets.getTicketsByUser(clientId);
        Comparator<Reservation> byEventDate = Comparator.comparing(item -> item.getEvent().getDateDebut());
        var upcoming = ownReservations.stream().filter(item -> upcoming(item, now))
                .sorted(byEventDate).map(Booking::from).toList();
        var history = ownReservations.stream().filter(item -> !upcoming(item, now))
                .sorted(byEventDate.reversed()).map(Booking::from).toList();

        request.setAttribute("upcoming", upcoming);
        request.setAttribute("history", history);
        request.setAttribute("reservationCount", ownReservations.size());
        request.setAttribute("upcomingCount", upcoming.size());
        request.setAttribute("ticketCount", ownTickets.stream()
                .filter(ticket -> ticket.getStatut() == TicketStatus.VALIDE && upcoming(ticket.getReservation(), now))
                .count());
        request.setAttribute("participationCount", ownTickets.stream()
                .filter(ticket -> ticket.getStatut() == TicketStatus.UTILISE)
                .map(ticket -> ticket.getReservation().getEvent().getId()).distinct().count());
        request.getRequestDispatcher("/WEB-INF/views/auth/client.jsp").forward(request, response);
    }

    private static boolean upcoming(Reservation booking, LocalDateTime now) {
        return booking.getStatut() == ReservationStatus.CONFIRMEE
                && booking.getEvent().getStatut() == EventStatus.PUBLIE
                && !booking.getEvent().getDateDebut().isBefore(now);
    }

    public record Booking(Long id, int quantity, boolean cancelled, boolean eventCancelled, EventServlet.EventCard event) {
        static Booking from(Reservation booking) {
            var event = booking.getEvent();
            return new Booking(booking.getId(), booking.getQuantite(), booking.getStatut() == ReservationStatus.ANNULEE,
                    event.getStatut() == EventStatus.ANNULE, EventServlet.toCard(event, null));
        }

        public Long getId() { return id; }
        public int getQuantity() { return quantity; }
        public boolean isCancelled() { return cancelled; }
        public boolean isEventCancelled() { return eventCancelled; }
        public EventServlet.EventCard getEvent() { return event; }
    }
}
