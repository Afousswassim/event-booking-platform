<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Explorer les événements" />
<c:set var="isCataloguePage" value="${true}" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <meta name="description" content="Explorez les événements à venir au Maroc et trouvez votre prochaine sortie sur Evently.">
    <link rel="stylesheet" href="${ctx}/css/catalogue.css">
    <script defer src="${ctx}/js/catalogue.js"></script>
</head>
<body class="site-page catalogue-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main id="main-content">
        <section class="explore-hero" aria-labelledby="explore-title">
            <div class="site-container hero-copy">
                <p class="site-kicker">Votre prochaine histoire commence ici</p>
                <h1 id="explore-title">Des moments à vivre.<br>Des <em>histoires à raconter.</em></h1>
                <p>Concerts, festivals, art, sport et innovation. Trouvez les événements qui vous ressemblent et préparez votre prochaine sortie.</p>
                <ul class="explore-stats" aria-label="En chiffres">
                    <li><strong>${fn:length(events)}</strong> événements</li>
                    <li id="city-stat" hidden><strong></strong> villes</li>
                    <li><strong>${fn:length(categories)}</strong> catégories</li>
                </ul>
            </div>
        </section>

        <div class="explore-surface">
            <div class="site-container">
                <form id="event-search" class="explore-search" action="${ctx}/events#catalogue" method="get" role="search">
                    <div class="explore-search-field">
                        <label for="explore-query">Événement, artiste ou lieu</label>
                        <input id="explore-query" type="search" name="q" maxlength="120" placeholder="Un événement, un artiste, une ville…" value="<c:out value='${query}' />">
                    </div>
                    <div class="explore-search-field explore-city" hidden>
                        <label for="explore-city">Ville</label>
                        <select id="explore-city"><option value="">Toutes les villes</option></select>
                    </div>
                    <button class="button button-primary" type="submit">Rechercher <span aria-hidden="true">↗</span></button>
                </form>

                <section id="catalogue" class="explore-catalogue" aria-labelledby="explore-heading">
                    <header class="explore-heading">
                        <p class="site-kicker">Explore your vibe</p>
                        <h2 id="explore-heading">Les événements à ne pas manquer <span aria-hidden="true">✳</span></h2>
                        <p>Explorez librement. Filtrez uniquement quand vous en avez besoin.</p>
                    </header>

                    <div class="explore-filters" id="categories" hidden>
                        <div class="explore-filter-heading">
                            <h3 id="category-heading">Explorer par catégorie</h3>
                            <button id="advanced-toggle" class="explore-control" type="button" aria-controls="advanced-filters" aria-expanded="false">
                                <span aria-hidden="true">☷</span> Filtres avancés <span class="explore-chevron" aria-hidden="true"></span>
                            </button>
                        </div>
                        <div id="category-options" class="explore-options" role="group" aria-labelledby="category-heading"></div>
                        <div class="explore-quick">
                            <label class="visually-hidden" for="explore-date">Date</label>
                            <select class="explore-control" id="explore-date">
                                <option value="">Toutes les dates</option>
                                <option value="week">7 prochains jours</option>
                                <option value="month">Ce mois-ci</option>
                                <option value="next-month">Le mois prochain</option>
                            </select>
                            <label class="visually-hidden" for="explore-budget">Budget</label>
                            <select class="explore-control" id="explore-budget">
                                <option value="">Tous les budgets</option>
                                <option value="free">Gratuit</option>
                                <option value="low">Jusqu’à 150 DH</option>
                                <option value="medium">Plus de 150 à 300 DH</option>
                                <option value="high">Plus de 300 DH</option>
                            </select>
                            <label class="explore-availability-filter">
                                <input type="checkbox" id="explore-available"> Places disponibles
                            </label>
                            <button id="explore-reset" class="explore-control" type="button">Réinitialiser</button>
                        </div>

                        <div id="advanced-filters" class="explore-advanced" aria-hidden="true" inert>
                            <div class="explore-advanced-inner">
                                <div class="explore-advanced-fields">
                                    <div class="explore-range">
                                        <label for="explore-price">Prix maximal</label>
                                        <output id="explore-price-value" for="explore-price">Sans limite</output>
                                        <input id="explore-price" type="range" min="0" max="0" value="0">
                                    </div>
                                    <div>
                                        <p id="month-heading">Mois de l’événement</p>
                                        <div id="month-options" class="explore-months" role="group" aria-labelledby="month-heading"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="explore-results">
                        <div>
                            <h3 id="result-count" role="status">${fn:length(events)} ${fn:length(events) eq 1 ? 'événement' : 'événements'} à découvrir</h3>
                            <ul id="active-filters" class="explore-active" aria-label="Filtres actifs" hidden></ul>
                        </div>
                        <div id="explore-sort-control" hidden>
                            <label class="visually-hidden" for="explore-sort">Trier les événements</label>
                            <select class="explore-control" id="explore-sort">
                                <option value="">Date la plus proche</option>
                                <option value="price-asc">Prix croissant</option>
                                <option value="price-desc">Prix décroissant</option>
                            </select>
                        </div>
                    </div>

                    <div class="event-grid explore-grid" id="explore-grid">
                        <c:forEach var="event" items="${events}">
                            <article class="explore-card" data-category="<c:out value='${event.category}' />" data-price="<c:out value='${event.price}' />" data-available="${event.availablePlaces}">
                                <div class="explore-card-image">
                                    <img src="${ctx}/images/<c:out value='${event.image}' />" alt="" width="416" height="240" loading="lazy">
                                    <span class="explore-badge"><c:out value="${event.category}" /></span>
                                    <time datetime="<c:out value='${event.start}' />" class="explore-card-date">
                                        <c:out value="${event.date}" />
                                    </time>
                                </div>
                                <div class="explore-card-body">
                                    <p class="explore-card-meta"><span class="explore-card-city" hidden></span><c:out value="${event.time}" /></p>
                                    <h3><c:out value="${event.title}" /></h3>
                                    <p class="explore-card-venue"><span aria-hidden="true">⌖</span> <span><c:out value="${event.venue}" /></span></p>
                                    <p class="explore-card-availability">
                                        ${event.availablePlaces} ${event.availablePlaces eq 1 ? 'place disponible' : 'places disponibles'}
                                    </p>
                                    <span class="explore-card-description" hidden><c:out value="${event.description}" /></span>
                                    <div class="explore-card-footer">
                                        <div>
                                            <c:if test="${event.price ne 'Gratuit'}"><span>Tarif</span></c:if>
                                            <strong><c:out value="${fn:replace(event.price, ' MAD', ' DH')}" /></strong>
                                        </div>
                                        <a class="button button-primary" href="${ctx}/events/${event.id}" aria-label="Découvrir <c:out value='${event.title}' />">Découvrir <span aria-hidden="true">↗</span></a>
                                    </div>
                                </div>
                            </article>
                        </c:forEach>
                    </div>

                    <div id="explore-empty" class="explore-empty" ${not empty events ? 'hidden' : ''}>
                        <h3 id="explore-empty-title">${empty query and empty category ? 'Aucun événement publié pour le moment.' : 'Aucun événement ne correspond à votre recherche.'}</h3>
                        <a id="empty-reset" class="button button-primary" href="${ctx}/events#catalogue" ${empty query and empty category ? 'hidden' : ''}>Réinitialiser</a>
                    </div>
                </section>
            </div>
        </div>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
