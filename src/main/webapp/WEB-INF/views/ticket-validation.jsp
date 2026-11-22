<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Valider un billet" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/booking.css">
</head>
<body class="site-page booking-page">
    <%@ include file="fragments/header.jspf" %>
    <main class="booking-surface booking-library" id="main-content">
        <div class="site-container booking-content">
            <nav aria-label="Espace agent"><a class="booking-link" href="${ctx}/agent">Mon espace</a></nav>
            <section class="booking-card booking-receipt" aria-labelledby="ticket-title">
                <h1 id="ticket-title">Valider un billet</h1>
                <c:if test="${ticketSuccess}"><p class="booking-notice" role="status">Le billet a été validé.</p></c:if>
                <c:if test="${not empty ticketError}"><p class="booking-error" role="alert"><c:out value="${ticketError}" /></p></c:if>
                <form method="post" action="${ctx}/agent/tickets">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                    <div class="booking-field">
                        <label for="ticket-code">Code du billet</label>
                        <input id="ticket-code" name="code" maxlength="100" required autocomplete="off" spellcheck="false">
                    </div>
                    <button class="button button-primary" type="submit">Valider le billet</button>
                </form>
            </section>
        </div>
    </main>
    <%@ include file="fragments/footer.jspf" %>
</body>
</html>
