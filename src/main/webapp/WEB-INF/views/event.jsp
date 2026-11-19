<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="${empty event ? 'Événement introuvable' : event.title}" />
<!doctype html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/fragments/head.jspf" %></head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main class="site-container detail-page" id="main-content">
        <a class="text-link" href="${ctx}/events#catalogue">← Tous les événements</a>
        <c:choose>
            <c:when test="${empty event}">
                <section class="empty-state"><h1>Événement introuvable.</h1><p>Cet événement n’est pas disponible dans le catalogue.</p></section>
            </c:when>
            <c:otherwise>
                <article class="event-detail">
                    <img src="${ctx}/images/${event.image}" alt="" width="752" height="540">
                    <div class="detail-copy">
                        <p class="site-kicker"><c:out value="${event.category}" /></p>
                        <h1><c:out value="${event.title}" /></h1>
                        <dl class="event-facts">
                            <div><dt>Date et heure</dt><dd><time datetime="${event.start}"><c:out value="${event.date}" /> à <c:out value="${event.time}" /></time></dd></div>
                            <div><dt>Lieu</dt><dd><c:out value="${event.venue}" /></dd></div>
                            <div><dt>Tarif</dt><dd><c:out value="${event.price}" /></dd></div>
                        </dl>
                        <c:if test="${not empty event.description}"><p class="event-description"><c:out value="${event.description}" /></p></c:if>
                    </div>
                </article>
            </c:otherwise>
        </c:choose>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
