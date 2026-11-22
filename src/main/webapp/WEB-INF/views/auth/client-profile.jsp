<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="pageTitle" value="Mon profil" />
<c:set var="clientTab" value="profile" />
<!doctype html>
<html lang="fr">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <link rel="stylesheet" href="${ctx}/css/client.css">
</head>
<body class="site-page">
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>
    <main id="main-content" class="client-surface">
        <div class="site-container client-content">
            <%@ include file="/WEB-INF/views/fragments/client-nav.jspf" %>
            <section class="client-profile" aria-labelledby="profile-title">
                <h1 id="profile-title">Mon profil</h1>
                <p>Modifiez le nom et l’adresse email associés à votre compte Evently.</p>
                <c:if test="${profileUpdated}"><p class="client-feedback" role="status">Votre profil a été mis à jour.</p></c:if>
                <c:if test="${not empty profileError}"><p class="client-feedback client-error" role="alert"><c:out value="${profileError}" /></p></c:if>
                <form class="client-profile-form" method="post" action="${ctx}/client/profile">
                    <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />">
                    <label for="client-name">Nom complet</label>
                    <input id="client-name" name="nom" autocomplete="name" maxlength="100" value="<c:out value='${clientName}' />" required>
                    <label for="client-email">Adresse email</label>
                    <input id="client-email" name="email" type="email" autocomplete="email" maxlength="150" value="<c:out value='${clientEmail}' />" required>
                    <label for="client-password">Mot de passe actuel</label>
                    <input id="client-password" name="currentPassword" type="password" autocomplete="current-password" maxlength="72" required>
                    <button type="submit" class="button button-primary">Enregistrer les modifications</button>
                </form>
            </section>
        </div>
    </main>
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
