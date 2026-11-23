package com.eventbooking.controller;

import com.eventbooking.model.EventStatus;
import com.eventbooking.model.ReservationStatus;
import com.eventbooking.model.Ticket;
import com.eventbooking.model.TicketStatus;
import com.eventbooking.security.AuthSession;
import com.eventbooking.service.TicketService;
import com.eventbooking.util.QrCodeService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;

@WebServlet({"/my-tickets", "/agent/tickets"})
public class TicketServlet extends HttpServlet {
    @Inject
    private TicketService tickets;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/agent/tickets".equals(request.getServletPath())) {
            showAgent(request, response);
            return;
        }
        var client = AuthSession.identity(request.getSession(false));
        String qrId = request.getParameter("qr");
        String detailId = request.getParameter("ticket");
        if (qrId != null || detailId != null) {
            showTicket(request, response, client.id(), qrId != null ? qrId : detailId, qrId != null);
            return;
        }

        var ownTickets = tickets.getTicketsByUser(client.id());
        String reservation = request.getParameter("reservationId");
        if (reservation != null) {
            Long id = ReservationServlet.positiveId(reservation);
            ownTickets = ownTickets.stream().filter(ticket -> ticket.getReservation().getId().equals(id)).toList();
            if (ownTickets.isEmpty()) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            else request.setAttribute("reservationId", id);
            request.setAttribute("selectedReservation", true);
        }
        var now = LocalDateTime.now();
        var passes = ownTickets.stream().map(ticket -> Pass.from(ticket, now))
                .sorted(Comparator.comparing(Pass::upcoming).reversed()
                        .thenComparing(pass -> pass.upcoming() ? pass.event().start() : "")
                        .thenComparing(Pass::id, Comparator.reverseOrder()))
                .toList();
        request.setAttribute("tickets", passes);
        request.setAttribute("totalCount", passes.size());
        request.setAttribute("validCount", passes.stream().filter(pass -> pass.status() == TicketStatus.VALIDE).count());
        request.setAttribute("usedCount", passes.stream().filter(pass -> pass.status() == TicketStatus.UTILISE).count());
        request.setAttribute("cancelledCount", passes.stream().filter(pass -> pass.status() == TicketStatus.ANNULE).count());
        request.getRequestDispatcher("/WEB-INF/views/tickets.jsp").forward(request, response);
    }

    private void showTicket(HttpServletRequest request, HttpServletResponse response, long clientId, String input, boolean qr)
            throws IOException, ServletException {
        Long id = ReservationServlet.positiveId(input);
        var ticket = id == null ? null : tickets.getTicketForUser(id, clientId);
        if (ticket == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (qr) {
            response.setContentType("image/png");
            QrCodeService.writePng(ticket.getCodeUnique(), response.getOutputStream());
            return;
        }
        var pass = Pass.from(ticket, LocalDateTime.now());
        Long reservation = ReservationServlet.positiveId(request.getParameter("reservationId"));
        if (reservation != null && reservation == pass.reservationId()) request.setAttribute("reservationId", reservation);
        request.setAttribute("pass", pass);
        request.getRequestDispatcher("/WEB-INF/views/ticket.jsp").forward(request, response);
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

    public record Pass(long id, long reservationId, String code, TicketStatus status,
                       boolean upcoming, EventServlet.EventCard event) {
        static Pass from(Ticket ticket, LocalDateTime now) {
            var reservation = ticket.getReservation();
            var event = reservation.getEvent();
            var status = ticket.getStatut() == TicketStatus.VALIDE && reservation.getStatut() == ReservationStatus.ANNULEE
                    ? TicketStatus.ANNULE : ticket.getStatut();
            boolean upcoming = status == TicketStatus.VALIDE && event.getStatut() == EventStatus.PUBLIE
                    && event.getDateDebut().isAfter(now);
            return new Pass(ticket.getId(), reservation.getId(), ticket.getCodeUnique(), status, upcoming,
                    EventServlet.toCard(event, null));
        }

        public long getId() { return id; }
        public long getReservationId() { return reservationId; }
        public String getCode() { return code; }
        public TicketStatus getStatus() { return status; }
        public boolean isUpcoming() { return upcoming; }
        public EventServlet.EventCard getEvent() { return event; }
    }
}
