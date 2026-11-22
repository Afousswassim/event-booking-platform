<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mon espace client" />
<c:set var="clientTab" value="overview" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/client.css">
</head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main id="main-content">
        <header class="client-hero">
            <div class="site-container client-hero-content">
                <div>
                    <p class="site-kicker">Mon espace client</p>
                    <h1>Bonjour, <c:out value="${clientName}" /> <span aria-hidden="true">👋</span></h1>
                    <p>Bienvenue dans votre espace Evently. Retrouvez vos prochaines sorties et vos billets au même endroit.</p>
                </div>
                <a class="button button-primary" href="${ctx}/events#catalogue">Explorer les événements <span aria-hidden="true">↗</span></a>
            </div>
        </header>

        <div class="client-surface">
            <div class="site-container client-content">
                <%@ include file="/WEB-INF/views/fragments/client-nav.jspf" %>

                <dl class="client-stats" aria-label="Vos statistiques">
                    <div><dt>Réservations</dt><dd><span aria-hidden="true">▤</span>${reservationCount}</dd></div>
                    <div><dt>Réservations à venir</dt><dd><span aria-hidden="true">▦</span>${upcomingCount}</dd></div>
                    <div><dt>Billets disponibles</dt><dd><span aria-hidden="true">◇</span>${ticketCount}</dd></div>
                    <div><dt>Événements auxquels vous avez participé</dt><dd><span aria-hidden="true">✳</span>${participationCount}</dd></div>
                </dl>

                <section class="client-section" aria-labelledby="upcoming-title">
                    <header class="client-section-heading">
                        <h2 id="upcoming-title">Mes prochaines réservations</h2>
                        <a class="client-link" href="${ctx}/my-reservations">Toutes mes réservations</a>
                    </header>
                    <c:choose>
                        <c:when test="${empty upcoming}">
                            <div class="client-empty">
                                <h3>Votre prochaine sortie reste à choisir</h3>
                                <p>Vos réservations confirmées apparaîtront ici.</p>
                                <a class="button button-primary" href="${ctx}/events#catalogue">Explorer les événements</a>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="client-bookings">
                                <c:forEach var="booking" items="${upcoming}">
                                    <article class="client-booking">
                                        <img src="${ctx}/images/<c:out value='${booking.event.image}' />" alt="" width="416" height="240" loading="lazy">
                                        <div class="client-booking-body">
                                            <span class="client-status">Confirmée</span>
                                            <h3><c:out value="${booking.event.title}" /></h3>
                                            <p><time datetime="${booking.event.start}"><c:out value="${booking.event.date}" /> · <c:out value="${booking.event.time}" /></time></p>
                                            <p><c:out value="${booking.event.venue}" /></p>
                                            <p class="client-booking-quantity">${booking.quantity} ${booking.quantity eq 1 ? 'billet' : 'billets'}</p>
                                            <div class="action-row">
                                                <a class="button client-secondary" href="${ctx}/events/${booking.event.id}">Voir les détails</a>
                                                <a class="button button-primary" href="${ctx}/my-tickets?reservationId=${booking.id}">Mes billets</a>
                                            </div>
                                        </div>
                                    </article>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="client-section" aria-labelledby="history-title">
                    <header class="client-section-heading"><h2 id="history-title">Historique des réservations</h2></header>
                    <c:choose>
                        <c:when test="${empty history}">
                            <div class="client-empty"><p>Vos réservations passées ou annulées apparaîtront ici.</p></div>
                        </c:when>
                        <c:otherwise>
                            <ul class="client-history">
                                <c:forEach var="booking" items="${history}">
                                    <li>
                                        <div>
                                            <h3><c:out value="${booking.event.title}" /></h3>
                                            <p><time datetime="${booking.event.start}"><c:out value="${booking.event.date}" /></time> · <c:out value="${booking.event.venue}" /></p>
                                        </div>
                                        <div class="client-history-meta">
                                            <span>${booking.quantity} ${booking.quantity eq 1 ? 'billet' : 'billets'}</span>
                                            <span class="client-status">${booking.cancelled ? 'Annulée' : 'Confirmée'}</span>
                                            <c:if test="${booking.eventCancelled}"><span class="client-status">Événement annulé</span></c:if>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </div>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
