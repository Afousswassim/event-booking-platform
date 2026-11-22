package com.eventbooking.controller;

import com.eventbooking.security.AuthSession;
import com.eventbooking.service.EventService;
import com.eventbooking.service.ReservationService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/my-reservations")
public class ReservationServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/reservations.jsp";

    @Inject
    private ReservationService reservations;

    @Inject
    private EventService events;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        var client = AuthSession.identity(request.getSession(false));
        request.setAttribute("reservations", reservations.getReservationsByUser(client.id()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long eventId;
        int quantity;
        try {
            eventId = Long.parseLong(request.getParameter("eventId"));
            quantity = Integer.parseInt(request.getParameter("quantity"));
        } catch (NumberFormatException invalid) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (eventId <= 0 || quantity <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (events.getPublishedEvent(eventId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        var client = AuthSession.identity(request.getSession(false));
        try {
            reservations.reserver(client.id(), eventId, quantity);
        } catch (IllegalStateException unavailable) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.setAttribute("bookingEventId", eventId);
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", request.getContextPath() + "/my-reservations");
    }
}
