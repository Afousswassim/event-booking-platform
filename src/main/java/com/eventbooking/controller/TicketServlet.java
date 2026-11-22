package com.eventbooking.controller;

import com.eventbooking.security.AuthSession;
import com.eventbooking.service.TicketService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

@WebServlet({"/my-tickets", "/agent/tickets"})
public class TicketServlet extends HttpServlet {
    private static final DateTimeFormatter EVENT_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH);
    @Inject
    private TicketService tickets;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/agent/tickets".equals(request.getServletPath())) {
            showAgent(request, response);
        } else {
            var client = AuthSession.identity(request.getSession(false));
            var ownTickets = tickets.getTicketsByUser(client.id());
            String selectedReservation = request.getParameter("reservationId");
            if (selectedReservation != null) {
                Long id = ReservationServlet.positiveId(selectedReservation);
                ownTickets = ownTickets.stream().filter(ticket -> ticket.getReservation().getId().equals(id)).toList();
                if (ownTickets.isEmpty()) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("selectedReservation", true);
            }
            var eventDates = new HashMap<Long, String>();
            for (var ticket : ownTickets) {
                var event = ticket.getReservation().getEvent();
                eventDates.computeIfAbsent(event.getId(), ignored -> EVENT_DATE.format(event.getDateDebut()));
            }
            request.setAttribute("ticketEventDates", eventDates);
            request.setAttribute("tickets", ownTickets);
            request.getRequestDispatcher("/WEB-INF/views/tickets.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/agent/tickets".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String code = request.getParameter("code");
        if (code == null || code.isBlank() || code.length() > 100) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("ticketError", "Saisissez un code de billet valide.");
        } else {
            try {
                tickets.validerTicket(code);
                request.setAttribute("ticketSuccess", true);
            } catch (IllegalArgumentException invalid) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("ticketError", "Aucun billet ne correspond à ce code.");
            } catch (IllegalStateException refused) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                request.setAttribute("ticketError", refused.getMessage());
            }
        }
        showAgent(request, response);
    }

    private static void showAgent(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setAttribute("csrfToken", AuthSession.csrfToken(request.getSession(false)));
        request.getRequestDispatcher("/WEB-INF/views/ticket-validation.jsp").forward(request, response);
    }
}
