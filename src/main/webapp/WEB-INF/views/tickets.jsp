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
    <link rel="stylesheet" href="${ctx}/css/tickets.css">
    <script src="${ctx}/js/tickets.js" defer></script>
</head>
<body class="site-page tickets-page">
    <%@ include file="fragments/header.jspf" %>
    <main class="client-surface" id="main-content">
        <div class="site-container client-content">
            <%@ include file="fragments/client-nav.jspf" %>
            <nav class="ticket-breadcrumb" aria-label="Fil d’Ariane">
                <a href="${ctx}/client">Espace client</a><span aria-hidden="true">/</span><span aria-current="page">Mes billets</span>
            </nav>
            <header class="ticket-hero">
                <div>
                    <p class="site-kicker">Your moments. Your tickets.</p>
                    <h1>Vos billets.<br>Vos <em>moments.</em></h1>
                    <p>Retrouvez vos accès à vos prochains événements.</p>
                </div>
                <span class="ticket-hero-symbol" aria-hidden="true">✳</span>
            </header>

            <dl class="client-stats ticket-stats" aria-label="Statistiques de billets">
                <div><dt>Total des billets</dt><dd><span aria-hidden="true">▤</span>${totalCount}</dd></div>
                <div><dt>Valides</dt><dd><span aria-hidden="true">✳</span>${validCount}</dd></div>
                <div><dt>Utilisés</dt><dd><span aria-hidden="true">✓</span>${usedCount}</dd></div>
                <div><dt>Annulés</dt><dd><span aria-hidden="true">⊘</span>${cancelledCount}</dd></div>
            </dl>

            <section aria-labelledby="digital-tickets">
                <header class="ticket-section-title">
                    <div>
                        <p class="site-kicker">Your digital passes</p>
                        <h2 id="digital-tickets">Mes billets numériques <span aria-hidden="true">✳</span></h2>
                    </div>
                    <c:if test="${selectedReservation}">
                        <a class="client-link" href="${ctx}/my-tickets">Tous mes billets</a>
                    </c:if>
                </header>
                <c:choose>
                    <c:when test="${empty tickets}">
                        <div class="client-empty">
                            <h3>${selectedReservation ? 'Aucun billet pour cette réservation' : 'Vos billets vous attendront ici'}</h3>
                            <p>${selectedReservation ? 'Consultez votre liste de réservations.' : 'Vos billets apparaissent ici dès la confirmation de votre réservation.'}</p>
                            <a class="button button-primary" href="${ctx}${selectedReservation ? '/my-reservations' : '/events#catalogue'}">${selectedReservation ? 'Mes réservations' : 'Explorer les événements'}</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="ticket-toolbar" hidden>
                            <div class="ticket-filters" role="group" aria-label="Filtrer par statut">
                                <button type="button" data-ticket-filter="VALIDE" aria-pressed="false">Valides <span>${validCount}</span></button>
                                <button type="button" data-ticket-filter="UTILISE" aria-pressed="false">Utilisés <span>${usedCount}</span></button>
                                <button type="button" data-ticket-filter="ANNULE" aria-pressed="false">Annulés <span>${cancelledCount}</span></button>
                                <button type="button" data-ticket-filter="all" aria-pressed="true">Tous</button>
                            </div>
                            <form class="ticket-search" role="search">
                                <label class="visually-hidden" for="ticket-search">Rechercher par événement, réservation ou code de billet</label>
                                <input id="ticket-search" type="search" placeholder="Rechercher un billet…" maxlength="120">
                                <button type="submit">Rechercher</button>
                            </form>
                        </div>
                        <p class="ticket-results" id="ticket-results" role="status" hidden></p>
                        <div class="client-empty" id="ticket-no-match" hidden>
                            <h3>Aucun billet ne correspond à votre recherche</h3>
                            <button class="button client-secondary" type="button" id="ticket-reset">Effacer les filtres</button>
                        </div>
                        <div class="ticket-list">
                            <c:forEach var="pass" items="${tickets}" varStatus="loop">
                                <c:set var="featured" value="${loop.first}" />
                                <%@ include file="fragments/ticket-pass.jspf" %>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </div>
    </main>
    <%@ include file="fragments/footer.jspf" %>
</body>
</html>
