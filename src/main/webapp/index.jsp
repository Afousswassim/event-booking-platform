<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not requestScope.catalogueReady}"><jsp:forward page="/" /></c:if>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Concerts, festivals et sorties" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <meta name="description" content="Découvrez les concerts, festivals et sorties à l’affiche sur Evently.">
</head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main id="main-content">
        <section class="home-hero">
            <div class="site-container hero-content">
                <div class="hero-copy">
                    <p class="site-kicker">Concerts, festivals et sorties au Maroc</p>
                    <h1>Moins de scroll.<br>Plus de <em>vibes.</em></h1>
                    <p>Des concerts qui vous font vibrer, des festivals qui vous rassemblent. Trouvez votre prochaine sortie.</p>
                    <div class="action-row">
                        <a class="button button-primary" href="${ctx}/events">Explorer les événements <span aria-hidden="true">↗</span></a>
                        <a class="button button-outline" href="#categories">Voir les catégories</a>
                    </div>
                </div>
                <c:if test="${not empty featured}">
                    <a class="hero-feature" href="${ctx}/events/${featured.id}">
                        <img src="${ctx}/images/${featured.image}" alt="" width="420" height="470">
                        <div class="hero-feature-text">
                            <p class="site-kicker">À l’affiche</p>
                            <h2><c:out value="${featured.title}" /></h2>
                            <p><time datetime="${featured.start}"><c:out value="${featured.date}" /></time> · <c:out value="${featured.venue}" /></p>
                            <span>Voir l’événement <span aria-hidden="true">↗</span></span>
                        </div>
                    </a>
                </c:if>
            </div>
        </section>
        <section class="site-container catalogue" id="catalogue" aria-labelledby="catalogue-title">
            <div class="section-heading">
                <div>
                    <p class="site-kicker">Au programme</p>
                    <h2 id="catalogue-title">Les événements à découvrir</h2>
                </div>
                <form class="search-form" method="get" action="${ctx}/events#catalogue" role="search">
                    <label class="visually-hidden" for="catalogue-query">Rechercher un événement, un lieu ou une catégorie</label>
                    <input id="catalogue-query" class="site-input" type="search" name="q" maxlength="120" placeholder="Événement, lieu, catégorie" value="<c:out value='${query}' />">
                    <c:if test="${not empty category}"><input type="hidden" name="category" value="<c:out value='${category}' />"></c:if>
                    <button class="button button-primary" type="submit">Rechercher</button>
                </form>
            </div>
            <div class="categories" id="categories">
                <h3>Catégories</h3>
                <c:url var="allEvents" value="/events"><c:if test="${not empty query}"><c:param name="q" value="${query}" /></c:if></c:url>
                <div class="category-list">
                    <a class="category-link" href="<c:out value='${allEvents}' />#catalogue" ${empty category ? 'aria-current="true"' : ''}>Tous les événements</a>
                    <c:forEach var="item" items="${categories}">
                        <c:url var="categoryUrl" value="/events"><c:param name="category" value="${item}" /><c:if test="${not empty query}"><c:param name="q" value="${query}" /></c:if></c:url>
                        <a class="category-link" href="<c:out value='${categoryUrl}' />#catalogue" ${category eq item ? 'aria-current="true"' : ''}><c:out value="${item}" /></a>
                    </c:forEach>
                </div>
            </div>
            <c:choose>
                <c:when test="${empty events}">
                    <div class="empty-state">
                        <c:choose>
                            <c:when test="${not empty query or not empty category}">
                                <h3>Aucun événement ne correspond à votre recherche.</h3>
                                <a class="text-link" href="${ctx}/events#catalogue">Effacer les filtres</a>
                            </c:when>
                            <c:otherwise><h3>Aucun événement publié pour le moment.</h3></c:otherwise>
                        </c:choose>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="event-grid">
                        <c:forEach var="event" items="${events}">
                            <article class="event-card">
                                <a href="${ctx}/events/${event.id}">
                                    <img src="${ctx}/images/${event.image}" width="416" height="240" alt="" loading="lazy">
                                    <div class="event-card-body">
                                        <span class="site-kicker"><c:out value="${event.category}" /></span>
                                        <h3><c:out value="${event.title}" /></h3>
                                        <p><time datetime="${event.start}"><c:out value="${event.date}" /></time> · <c:out value="${event.venue}" /></p>
                                        <div class="card-meta"><strong><c:out value="${event.price}" /></strong><span>Détails <span aria-hidden="true">↗</span></span></div>
                                    </div>
                                </a>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
        <section class="site-container">
            <div class="join-panel">
                <div>
                    <h2>Vos prochaines sorties commencent ici.</h2>
                    <p>Retrouvez votre espace Evently et explorez les événements à l’affiche.</p>
                </div>
                <c:choose>
                    <c:when test="${not empty sessionScope.authUser}"><a class="button button-primary" href="${ctx}${sessionScope.authUser.role.homePath}">Mon espace</a></c:when>
                    <c:otherwise><a class="button button-primary" href="${ctx}/register">Créer un compte</a></c:otherwise>
                </c:choose>
            </div>
        </section>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
