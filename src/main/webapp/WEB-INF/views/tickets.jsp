<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mes billets" />
<c:set var="clientTab" value="tickets" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/booking.css">
    <link rel="stylesheet" href="${ctx}/css/client.css">
</head>
<body class="site-page booking-page">
    <%@ include file="fragments/header.jspf" %>
    <main class="booking-surface booking-library" id="main-content">
        <div class="site-container booking-content">
            <%@ include file="fragments/client-nav.jspf" %>
            <h1>Mes billets</h1>
            <c:choose>
                <c:when test="${empty tickets}">
                    <div class="booking-card">
                        <p>${selectedReservation ? 'Aucun billet disponible pour cette réservation.' : 'Vous n’avez pas encore de billet.'}</p>
                        <a class="booking-link" href="${ctx}/events#catalogue">Explorer les événements</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <p>Présentez le code de chaque billet à l’entrée de l’événement.</p>
                    <div class="event-grid">
                        <c:forEach var="ticket" items="${tickets}">
                            <c:set var="ticketEvent" value="${ticket.reservation.event}" />
                            <article class="booking-card">
                                <c:choose>
                                    <c:when test="${ticket.statut eq 'VALIDE' and ticket.reservation.statut eq 'CONFIRMEE'}"><span class="booking-badge">Valide</span></c:when>
                                    <c:when test="${ticket.statut eq 'UTILISE'}"><span class="booking-badge">Utilisé</span></c:when>
                                    <c:otherwise><span class="booking-badge">Annulé</span></c:otherwise>
                                </c:choose>
                                <h2><c:out value="${ticketEvent.titre}" /></h2>
                                <p><time datetime="${ticketEvent.dateDebut}"><c:out value="${ticketEventDates[ticketEvent.id]}" /></time></p>
                                <p><c:out value="${ticketEvent.lieu}" /></p>
                                <p>Réservation EVT-${ticket.reservation.id}</p>
                                <code><c:out value="${ticket.codeUnique}" /></code>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
    <%@ include file="fragments/footer.jspf" %>
</body>
</html>
