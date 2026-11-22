<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mon espace" />
<!doctype html>
<html lang="fr">
<head><%@ include file="/WEB-INF/views/fragments/head.jspf" %></head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main class="site-container detail-page" id="main-content">
        <section class="empty-state" aria-labelledby="account-title">
            <p class="site-kicker">
                <c:choose>
                    <c:when test="${sessionScope.userRole eq 'ADMIN'}">Espace administrateur</c:when>
                    <c:when test="${sessionScope.userRole eq 'AGENT'}">Espace agent</c:when>
                    <c:otherwise>Espace client</c:otherwise>
                </c:choose>
            </p>
            <h1 id="account-title">Bonjour, <c:out value="${sessionScope.userName}" />.</h1>
            <div class="action-row">
                <a class="button button-primary" href="${ctx}/events#catalogue">Explorer les événements</a>
                <c:if test="${sessionScope.userRole eq 'CLIENT'}">
                    <a class="button button-outline" href="${ctx}/my-reservations">Mes réservations</a>
                </c:if>
                <form method="post" action="${ctx}/logout">
                    <input type="hidden" name="csrfToken" value="<c:out value='${requestScope.csrfToken}' />">
                    <button class="button button-outline" type="submit">Se déconnecter</button>
                </form>
            </div>
        </section>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
