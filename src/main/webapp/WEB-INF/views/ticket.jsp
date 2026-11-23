<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Billet numérique" />
<c:set var="clientTab" value="tickets" />
<c:set var="ticketDetail" value="${true}" />
<c:set var="featured" value="${true}" />
<c:url var="backUrl" value="/my-tickets">
    <c:if test="${not empty reservationId}"><c:param name="reservationId" value="${reservationId}" /></c:if>
</c:url>
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/client.css">
    <link rel="stylesheet" href="${ctx}/css/tickets.css">
    <script src="${ctx}/js/tickets.js" defer></script>
</head>
<body class="site-page tickets-page ticket-detail-page">
    <%@ include file="fragments/header.jspf" %>
    <main id="main-content" class="client-surface">
        <div class="site-container client-content">
            <%@ include file="fragments/client-nav.jspf" %>
            <a class="client-link ticket-back" href="<c:out value='${backUrl}' />">Retour à mes billets</a>
            <section class="ticket-detail" aria-labelledby="ticket-detail-title">
                <header class="ticket-section-title">
                    <h1 id="ticket-detail-title">Votre billet numérique</h1>
                    <div class="action-row ticket-actions">
                        <button class="button client-secondary" type="button" data-copy-ticket hidden>Copier le code</button>
                        <button class="button button-primary" type="button" data-print-ticket hidden>Imprimer</button>
                    </div>
                </header>
                <p class="ticket-feedback" role="status"></p>
                <%@ include file="fragments/ticket-pass.jspf" %>
                <c:if test="${not empty pass.event.description}">
                    <section class="ticket-description" aria-labelledby="ticket-event-title">
                        <h2 id="ticket-event-title">À propos de l’événement</h2>
                        <p><c:out value="${pass.event.description}" /></p>
                    </section>
                </c:if>
            </section>
        </div>
    </main>
    <%@ include file="fragments/footer.jspf" %>
</body>
</html>
