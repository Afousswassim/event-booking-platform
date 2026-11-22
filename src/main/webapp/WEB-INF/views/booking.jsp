<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="${empty receipt ? 'Réservation' : 'Réservation confirmée'}" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/booking.css">
    <c:if test="${not empty checkoutToken}"><script defer src="${ctx}/js/booking.js"></script></c:if>
</head>
<body class="site-page booking-page">
    <%@ include file="fragments/header.jspf" %>
    <main id="main-content">
        <header class="booking-hero">
            <div class="site-container hero-copy">
                <nav aria-label="Fil d’Ariane">
                    <a href="${ctx}/">Accueil</a><span aria-hidden="true">/</span>
                    <a href="${ctx}/events#catalogue">Événements</a><span aria-hidden="true">/</span>
                    <span aria-current="page">${pageTitle}</span>
                </nav>
                <c:choose>
                    <c:when test="${not empty receipt}">
                        <p class="site-kicker">Réservation confirmée</p>
                        <h1>Votre réservation est <em>confirmée !</em></h1>
                    </c:when>
                    <c:otherwise>
                        <p class="site-kicker">Votre prochaine expérience commence ici</p>
                        <h1>Votre place pour <em>l’inoubliable.</em></h1>
                        <c:if test="${not empty checkoutToken}"><p>Choisissez vos billets et retrouvez-les dans votre espace Evently.</p></c:if>
                    </c:otherwise>
                </c:choose>
            </div>
        </header>

        <div class="booking-surface">
            <div class="site-container booking-content">
                <c:choose>
                    <c:when test="${not empty receipt}">
                        <section class="booking-card booking-receipt" aria-labelledby="receipt-title">
                            <span class="booking-mark" aria-hidden="true">✓</span>
                            <h2 id="receipt-title"><c:out value="${receipt.title}" /></h2>
                            <dl class="booking-totals">
                                <div><dt>Référence</dt><dd>EVT-${receipt.id}</dd></div>
                                <div><dt>Billets</dt><dd>${receipt.quantity}</dd></div>
                                <div class="booking-total"><dt>Montant total</dt><dd><c:out value="${receipt.total}" /></dd></div>
                            </dl>
                            <div class="action-row">
                                <a class="button button-primary" href="${ctx}/my-tickets">Voir mes billets <span aria-hidden="true">↗</span></a>
                                <a class="button booking-secondary" href="${ctx}/events#catalogue">Retour aux événements</a>
                            </div>
                        </section>
                    </c:when>
                    <c:when test="${empty checkoutToken}">
                        <section class="booking-card booking-receipt">
                            <h2>Réservation indisponible</h2>
                            <p role="alert"><c:out value="${bookingError}" /></p>
                            <div class="action-row">
                                <a class="button button-primary" href="${ctx}/events#catalogue">Retour aux événements</a>
                                <a class="button booking-secondary" href="${ctx}/my-reservations">Mes réservations</a>
                            </div>
                        </section>
                    </c:when>
                    <c:otherwise>
                        <ol class="booking-progress" aria-label="Progression de la réservation" hidden>
                            <li aria-current="step"><span>1</span> Mes billets</li>
                            <li><span>2</span> Mes coordonnées</li>
                            <li><span>3</span> Confirmation</li>
                        </ol>
                        <div class="booking-layout">
                            <form class="booking-card booking-form" id="booking-form" method="post" action="${ctx}/my-reservations"
                                    data-unit-cents="${unitCents}">
                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                <input type="hidden" name="checkoutToken" value="<c:out value='${checkoutToken}' />">
                                <input type="hidden" name="eventId" value="${event.id}">
                                <c:if test="${not empty bookingError}">
                                    <p class="booking-error" role="alert"><c:out value="${bookingError}" /></p>
                                </c:if>

                                <section class="booking-step" aria-labelledby="tickets-step">
                                    <div class="booking-step-heading">
                                        <span class="booking-mark" aria-hidden="true">▤</span>
                                        <h2 id="tickets-step" tabindex="-1">Choisissez vos billets</h2>
                                    </div>
                                    <p>Sélectionnez le nombre de places que vous souhaitez réserver.</p>
                                    <div class="booking-event">
                                        <img src="${ctx}/images/<c:out value='${event.image}' />" width="96" height="96" alt="">
                                        <div>
                                            <span class="booking-badge"><c:out value="${event.category}" /></span>
                                            <h3><c:out value="${event.title}" /></h3>
                                            <p><c:out value="${event.venue}" /></p>
                                            <p><time datetime="<c:out value='${event.start}' />"><c:out value="${event.date}" /> · <c:out value="${event.time}" /></time></p>
                                        </div>
                                    </div>
                                    <div class="booking-ticket">
                                        <div><strong>Billet standard</strong><p>Accès individuel à l’événement</p></div>
                                        <strong><c:out value="${unitPrice}" /></strong>
                                    </div>
                                    <div class="booking-quantity">
                                        <div>
                                            <label for="booking-quantity">Nombre de billets</label>
                                            <p id="booking-limit">Maximum ${maximum} par réservation</p>
                                        </div>
                                        <div class="booking-stepper">
                                            <button type="button" data-quantity-change="-1" aria-label="Retirer un billet" hidden>−</button>
                                            <input id="booking-quantity" name="quantity" type="number" inputmode="numeric" min="1"
                                                    max="${maximum}" value="${quantity}" aria-describedby="booking-limit booking-stock" readonly required>
                                            <button type="button" data-quantity-change="1" aria-label="Ajouter un billet" hidden>+</button>
                                        </div>
                                    </div>
                                    <p class="booking-stock" id="booking-stock">${event.availablePlaces} ${event.availablePlaces eq 1 ? 'place restante' : 'places restantes'}</p>
                                    <div class="booking-actions">
                                        <a class="booking-link" href="${ctx}/events/${event.id}">Détails de l’événement</a>
                                        <button class="button button-primary" type="button" data-booking-next hidden>Continuer <span aria-hidden="true">↗</span></button>
                                    </div>
                                </section>

                                <section class="booking-step" aria-labelledby="contact-step">
                                    <div class="booking-step-heading">
                                        <span class="booking-mark" aria-hidden="true">◎</span>
                                        <h2 id="contact-step" tabindex="-1">Vérifiez vos coordonnées</h2>
                                    </div>
                                    <p>Vos billets seront associés à ce compte Evently.</p>
                                    <p id="booking-contact-error" class="booking-error" role="alert" tabindex="-1" hidden>Vérifiez que votre compte contient un nom et une adresse email valides.</p>
                                    <div class="booking-field">
                                        <label for="booking-name">Nom complet</label>
                                        <input id="booking-name" value="<c:out value='${clientName}' />" readonly>
                                    </div>
                                    <div class="booking-field">
                                        <label for="booking-email">Adresse email</label>
                                        <input id="booking-email" type="email" value="<c:out value='${clientEmail}' />" readonly>
                                    </div>
                                    <div class="booking-actions">
                                        <button class="button booking-secondary" type="button" data-booking-back hidden>Retour</button>
                                        <button class="button button-primary" type="button" data-booking-next hidden>Vérifier ma réservation</button>
                                    </div>
                                </section>

                                <section class="booking-step" aria-labelledby="review-step">
                                    <div class="booking-step-heading">
                                        <span class="booking-mark" aria-hidden="true">✓</span>
                                        <h2 id="review-step" tabindex="-1">Confirmez votre réservation</h2>
                                    </div>
                                    <dl class="booking-totals booking-review">
                                        <div><dt>Événement</dt><dd><c:out value="${event.title}" /></dd></div>
                                        <div><dt>Client</dt><dd><c:out value="${clientName}" /><br><c:out value="${clientEmail}" /></dd></div>
                                        <div><dt>Prix unitaire</dt><dd><c:out value="${unitPrice}" /></dd></div>
                                        <div><dt>Billets</dt><dd data-booking-quantity>${quantity}</dd></div>
                                        <div class="booking-total"><dt>Total</dt><dd data-booking-total><c:out value="${total}" /></dd></div>
                                    </dl>
                                    <label class="booking-consent"><input name="confirmed" value="yes" type="checkbox" required>
                                        Je confirme les informations et le nombre de billets.
                                    </label>
                                    <div class="booking-actions">
                                        <button class="button booking-secondary" type="button" data-booking-back hidden>Retour</button>
                                        <button class="button button-primary" type="submit">Confirmer ma réservation <span aria-hidden="true">↗</span></button>
                                    </div>
                                </section>
                            </form>

                            <aside class="booking-card booking-summary" aria-labelledby="summary-title">
                                <img src="${ctx}/images/<c:out value='${event.image}' />" width="416" height="240" alt="">
                                <div class="booking-summary-body">
                                    <h2 id="summary-title">Récapitulatif</h2>
                                    <p><c:out value="${event.title}" /></p>
                                    <dl class="booking-totals">
                                        <div><dt>Prix par billet</dt><dd><c:out value="${unitPrice}" /></dd></div>
                                        <div><dt>Billets</dt><dd data-booking-quantity>${quantity}</dd></div>
                                        <div class="booking-total"><dt>Total</dt><dd><output data-booking-total aria-live="polite"><c:out value="${total}" /></output></dd></div>
                                    </dl>
                                </div>
                            </aside>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </main>
    <%@ include file="fragments/footer.jspf" %>
</body>
</html>
