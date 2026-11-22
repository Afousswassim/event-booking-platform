<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mes réservations" />
<c:set var="clientTab" value="reservations" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/client.css">
</head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main class="site-container detail-page" id="main-content">
        <%@ include file="/WEB-INF/views/fragments/client-nav.jspf" %>
        <section class="empty-state" aria-labelledby="reservations-title">
            <h1 id="reservations-title">Mes réservations</h1>
            <p><a class="text-link" href="${ctx}/my-tickets">Voir mes billets</a></p>
            <c:choose>
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
