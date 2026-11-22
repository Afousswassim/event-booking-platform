<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mes réservations" />
<!doctype html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/fragments/head.jspf" %></head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main class="site-container detail-page" id="main-content">
        <a class="text-link" href="${ctx}/events#catalogue">← Tous les événements</a>
        <section class="empty-state" aria-labelledby="reservations-title">
            <h1 id="reservations-title">Mes réservations</h1>
            <c:choose>
                <c:when test="${not empty bookingEventId}">
                    <p role="alert">La réservation n’a pas pu être confirmée. Vérifiez la date de l’événement et le nombre de places disponibles.</p>
                    <div class="action-row"><a class="button button-primary" href="${ctx}/events/${bookingEventId}">Retour à l’événement</a></div>
                </c:when>
                <c:when test="${empty reservations}"><p>Vous n’avez pas encore de réservation.</p></c:when>
                <c:otherwise>
                    <ul>
                        <c:forEach var="reservation" items="${reservations}">
                            <li>
                                <strong><c:out value="${reservation.event.titre}" /></strong>
                                · ${reservation.quantite} ${reservation.quantite eq 1 ? 'place' : 'places'}
                                · ${reservation.statut eq 'CONFIRMEE' ? 'Confirmée' : 'Annulée'}
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
