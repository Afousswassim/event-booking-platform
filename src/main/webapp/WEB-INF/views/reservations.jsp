<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mes réservations" />
<c:set var="clientTab" value="reservations" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/client.css">
    <link rel="stylesheet" href="${ctx}/css/tickets.css">
    <link rel="stylesheet" href="${ctx}/css/reservations.css">
    <c:if test="${not empty reservations}"><script src="${ctx}/js/reservations.js" defer></script></c:if>
</head>
<body class="site-page tickets-page">
    <%@ include file="fragments/header.jspf" %>
    <main class="client-surface" id="main-content">
        <div class="site-container client-content">
            <%@ include file="fragments/client-nav.jspf" %>
            <nav class="ticket-breadcrumb" aria-label="Fil d’Ariane">
                <a href="${ctx}/client">Espace client</a><span aria-hidden="true">/</span>
                <c:choose>
                    <c:when test="${empty reservationDetail}"><span aria-current="page">Mes réservations</span></c:when>
                    <c:otherwise>
                        <a href="${ctx}/my-reservations">Mes réservations</a><span aria-hidden="true">/</span><span aria-current="page">EVT-${reservationDetail.id}</span>
                    </c:otherwise>
                </c:choose>
            </nav>
            <c:choose>
                <c:when test="${not empty reservationDetail}">
                    <section class="reservation-detail" aria-labelledby="reservations-title">
                        <header class="ticket-section-title"><h1 id="reservations-title">Réservation EVT-${reservationDetail.id}</h1></header>
                        <c:if test="${not empty cancellationError}">
                            <p class="reservation-error" role="alert"><c:out value="${cancellationError}" /></p>
                        </c:if>
                        <c:set var="booking" value="${reservationDetail}" />
                        <c:set var="featured" value="${true}" />
                        <%@ include file="fragments/reservation-card.jspf" %>
                        <c:if test="${reservationDetail.cancellable}">
                            <details class="client-empty reservation-cancellation">
                                <summary>Annuler cette réservation</summary>
                                <p>L’annulation invalide les billets associés qui n’ont pas été utilisés.</p>
                                <form method="post" action="${ctx}/my-reservations">
                                    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />">
                                    <input type="hidden" name="cancelReservation" value="${reservationDetail.id}">
                                    <button type="submit" class="button client-secondary">Confirmer l’annulation</button>
                                </form>
                            </details>
                        </c:if>
                    </section>
                </c:when>
                <c:otherwise>
                    <header class="ticket-hero">
                        <div>
                            <p class="site-kicker">Your next memories</p>
                            <h1>Vos événements.<br>Vos <em>histoires.</em></h1>
                            <p>Retrouvez vos réservations et préparez votre prochaine expérience.</p>
                        </div>
                        <span class="ticket-hero-symbol" aria-hidden="true">✳</span>
                    </header>
                    <dl class="client-stats ticket-stats" aria-label="Statistiques des réservations">
                        <div><dt>Total des réservations</dt><dd><span aria-hidden="true">▤</span>${fn:length(reservations)}</dd></div>
                        <div><dt>À venir</dt><dd><span aria-hidden="true">✳</span>${upcomingCount}</dd></div>
                        <div><dt>Terminées</dt><dd><span aria-hidden="true">✓</span>${finishedCount}</dd></div>
                        <div><dt>Annulées</dt><dd><span aria-hidden="true">⊘</span>${cancelledCount}</dd></div>
                    </dl>
                    <section aria-labelledby="reservations-title">
                        <header class="ticket-section-title">
                            <div>
                                <p class="site-kicker">Your booking history</p>
                                <h2 id="reservations-title">Mes réservations <span aria-hidden="true">✳</span></h2>
                            </div>
                        </header>
                        <c:choose>
                            <c:when test="${empty reservations}">
                                <div class="client-empty">
                                    <h3>Vos prochaines sorties commencent ici</h3>
                                    <p>Vos réservations apparaîtront ici dès leur confirmation.</p>
                                    <a class="button button-primary" href="${ctx}/events#catalogue">Explorer les événements</a>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="ticket-toolbar reservation-toolbar" hidden>
                                    <div class="ticket-filters" role="group" aria-label="Filtrer les réservations">
                                        <button type="button" data-reservation-filter="upcoming" aria-pressed="${upcomingCount gt 0}">À venir <span>${upcomingCount}</span></button>
                                        <button type="button" data-reservation-filter="finished" aria-pressed="false">Terminées <span>${finishedCount}</span></button>
                                        <button type="button" data-reservation-filter="cancelled" aria-pressed="false">Annulées <span>${cancelledCount}</span></button>
                                        <button type="button" data-reservation-filter="all" aria-pressed="${upcomingCount eq 0}">Toutes <span>${fn:length(reservations)}</span></button>
                                    </div>
                                    <form class="ticket-search" role="search">
                                        <label class="visually-hidden" for="reservation-search">Rechercher par événement ou référence</label>
                                        <input id="reservation-search" type="search" placeholder="Événement, référence…" maxlength="120">
                                        <button type="submit">Rechercher</button>
                                    </form>
                                    <label class="visually-hidden" for="reservation-sort">Trier les réservations</label>
                                    <select id="reservation-sort">
                                        <option value="upcoming">À venir en premier</option>
                                        <option value="recent">Réservées récemment</option>
                                        <option value="event-date">Date d’événement décroissante</option>
                                    </select>
                                </div>
                                <p class="ticket-results" id="reservation-results" role="status" hidden></p>
                                <div class="client-empty" id="reservation-no-match" hidden>
                                    <h3>Aucune réservation ne correspond à votre recherche</h3>
                                    <button class="button client-secondary" type="button" id="reservation-reset">Effacer les filtres</button>
                                </div>
                                <div class="reservation-list" id="reservation-list">
                                    <c:forEach var="booking" items="${reservations}" varStatus="loop">
                                        <c:set var="featured" value="${loop.first}" />
                                        <%@ include file="fragments/reservation-card.jspf" %>
                                        <c:if test="${loop.first}">
                                            <div class="reservation-more" id="reservation-more" ${fn:length(reservations) le 1 ? 'hidden' : ''}>
                                                <h3>Autres réservations</h3><span id="reservation-more-count"></span>
                                            </div>
                                        </c:if>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </section>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
    <%@ include file="fragments/footer.jspf" %>
</body>
</html>
