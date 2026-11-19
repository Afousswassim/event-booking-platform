# Evently — Comprendre et refaire l’authentification

Ce guide décrit la partie du Développeur 1 telle qu’elle existe dans le dépôt. Il suit les données depuis le formulaire HTTP jusqu’à MySQL, puis de MySQL jusqu’à la réponse. Le [contrat HTTP pour le frontend](docs/auth.md) est disponible séparément.

## 1. Ce qui fonctionne dans Evently

- **Inscription (Register).** Le serveur valide le nom, l’email et le mot de passe, contrôle la disponibilité de l’email, calcule une empreinte BCrypt et enregistre un compte CLIENT. Le nouveau compte doit ensuite se connecter.
- **Connexion (Login).** Le serveur recherche l’email dans MySQL et vérifie le mot de passe avec BCrypt. Si la vérification réussit, il remplace la session existante et renvoie le profil ainsi qu’un nouveau jeton CSRF.
- **Déconnexion (Logout).** Le serveur invalide la session. L’ancien cookie perd son accès.
- **Autorisations.** CLIENT, ADMIN et AGENT sont trois rôles distincts. Chacun a accès à son propre espace et à son propre profil. Le rôle vient de MySQL lors de la connexion.
- **Profil.** Un utilisateur connecté peut consulter son identifiant, son nom, son email et son rôle. Il peut modifier son nom et son email en fournissant son mot de passe actuel.
- **Protection des requêtes.** Le filtre contrôle la session et le rôle avant les routes protégées. Pour les actions POST, il contrôle en plus un jeton CSRF lié à cette session. Le cookie porte des attributs de sécurité et la session expire côté serveur après 30 minutes d’inactivité.

**À retenir.** L’authentification répond à « qui es-tu ? », l’autorisation à « as-tu le droit de faire cette action ? » et le contrôle CSRF à « cette demande d’action possède-t-elle le jeton attendu pour ta session ? ».

### Périmètre actuel

Le backend d’authentification fournit des routes JSON et des routes de formulaire compatibles avec les JSP de connexion et d’inscription à venir. Les pages de connexion, d’inscription et de profil restent à intégrer côté frontend. Les routes `/auth/access/*` répondent par un simple statut et servent à vérifier les droits. Les préfixes `/client/*`, `/admin/*` et `/agent/*` protègent les routes que l’application y expose. `/my-reservations` et `/my-tickets` sont aussi réservées aux CLIENT.

Le changement et la réinitialisation du mot de passe, la vérification de la boîte email, la gestion des comptes ou des rôles par un administrateur, la limitation des tentatives de connexion et l’authentification multifacteur restent à développer. Il n’y a ni jeton JWT ni route publique d’attribution des rôles. Le rôle en session est une copie du rôle lu à la connexion. Une modification du rôle directement dans MySQL sera prise en compte lors d’une prochaine connexion, après remplacement de la session.

## 2. Les fichiers et les relations entre les classes

Tous les chemins Java de production ci-dessous partent de `src/main/java/com/eventbooking/`.

| Fichier | Responsabilité et méthodes à repérer |
| --- | --- |
| [controller/AuthServlet.java](src/main/java/com/eventbooking/controller/AuthServlet.java) | Reçoit HTTP. `doGet` et `doPost` choisissent l’action d’après `path(request)`. `handleForm` lit les champs et appelle le service. `respondToFailure` convertit les erreurs métier en statuts HTTP. `profileJson` construit la représentation publique. |
| [service/UserService.java](src/main/java/com/eventbooking/service/UserService.java) | Contient les règles métier. `register` crée un CLIENT. `authenticate` vérifie les identifiants. `profile` lit un compte. `updateProfile` valide et modifie le compte de la session. Les types imbriqués `Profile`, `Problem` et `Failure` transportent le résultat public ou une erreur métier. |
| [repository/UserRepository.java](src/main/java/com/eventbooking/repository/UserRepository.java) | Accède aux entités via `EntityManager`. `findByEmail` recherche un email, `findById` recherche la clé primaire, `findByIdForUpdate` prend un verrou d’écriture, `save` enregistre une nouvelle entité, `flush` envoie les écritures en attente à MySQL. |
| [model/User.java](src/main/java/com/eventbooking/model/User.java) | Représente un enregistrement de la table `users`. Contient `id`, `nom`, `email`, `passwordHash` et `role`. |
| [model/Role.java](src/main/java/com/eventbooking/model/Role.java) | Énumération Java des valeurs `CLIENT`, `ADMIN` et `AGENT`. |
| [security/PasswordUtils.java](src/main/java/com/eventbooking/security/PasswordUtils.java) | Vérifie les limites BCrypt avec `canVerify`. `hash` construit une empreinte. `matches` vérifie un mot de passe avec une empreinte existante ou fait un calcul factice lorsque le compte n’est pas trouvé. |
| [security/AuthFilter.java](src/main/java/com/eventbooking/security/AuthFilter.java) | Intercepte les URL concernées. `isPublic` repère les chemins publics et `requiredRole` détermine le rôle exigé. `doFilter` contrôle l’accès et le jeton avant de laisser continuer la chaîne. |
| [security/AuthSession.java](src/main/java/com/eventbooking/security/AuthSession.java) | Centralise les attributs de session. `identity` lit l’objet `authUser`. `csrfToken` crée un jeton ou relit celui de la session. `validCsrf` compare les jetons. Le record sérialisable `Identity` contient l’identifiant et le rôle. |
| [security/AuthResponse.java](src/main/java/com/eventbooking/security/AuthResponse.java) | `json` définit le statut, le type JSON et l’encodage UTF-8, puis écrit la réponse. `error` construit un corps de la forme `{"error":"code"}`. |
| [service/AuthService.java](src/main/java/com/eventbooking/service/AuthService.java) | Conserve l’interface Java des commits distants avec `register` et `login`. Délègue à `UserService` puis charge l’entité `User` pour les appelants de cette interface. Le servlet utilise directement `UserService` et son type `Profile`. |

| Configuration | Fonction dans le parcours |
| --- | --- |
| [pom.xml](pom.xml) | Compile pour Java 21 et produit `target/event-booking-platform.war`. Déclare Jakarta EE 11 en scope `provided`, BCrypt et les dépendances des tests. WildFly fournit les API et leurs implémentations d’exécution Jakarta EE. BCrypt est livré avec le WAR. |
| [META-INF/beans.xml](src/main/resources/META-INF/beans.xml) | Active la découverte CDI en mode `annotated`. Les classes de service et de dépôt portent `@ApplicationScoped`. |
| [META-INF/persistence.xml](src/main/resources/META-INF/persistence.xml) | Déclare `eventBookingPU` en mode JTA, utilise `java:jboss/datasources/EventBookingDS` et inclut explicitement `User`. La valeur `none` désactive la création et la modification automatiques du schéma. Elle ne lance pas non plus une validation du schéma au démarrage. |
| [WEB-INF/web.xml](src/main/webapp/WEB-INF/web.xml) | Configure 30 minutes d’inactivité, `HttpOnly` et le suivi des sessions par cookie. |
| [WEB-INF/undertow-handlers.conf](src/main/webapp/WEB-INF/undertow-handlers.conf) | Demande à Undertow, le serveur web de WildFly, d’appliquer `SameSite=Lax` à `JSESSIONID` et `Secure` aux cookies des échanges HTTPS. |

Une lecture de profil suit ce chemin. Une demande de jeton CSRF, un contrôle de rôle ou la déconnexion peuvent se terminer sans accès à MySQL.

```text
Requête du navigateur + cookie JSESSIONID
        |
        v
AuthFilter  ->  lit AuthSession et décide si la requête continue
        |
        v
AuthServlet  ->  lit HTTP et appelle UserService
        |
        v
UserService  ->  applique les règles métier et utilise PasswordUtils si nécessaire
        |
        v
UserRepository  ->  EntityManager / Hibernate  ->  EventBookingDS  ->  MySQL
        |
        v
UserService.Profile  ->  AuthServlet  ->  AuthResponse  ->  réponse HTTP
```

## 3. Lire les morceaux essentiels du vrai code

### 3.1 Servlet, CDI et règles métier

Extrait d’`AuthServlet.java`.

```java
@WebServlet({"/auth/*", "/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String USER_NAME = "userName";

    @Inject
    private UserService users;
```

`@WebServlet` associe les chemins sous `/auth/` au servlet. Par exemple, pour `/auth/login`, `request.getPathInfo()` vaut `/login`. `HttpServlet` appelle `doGet` ou `doPost` d’après la méthode de la requête. `getParameter("email")` lit un champ du formulaire dans le corps POST après le contrôle du filtre. Le filtre fixe UTF-8 avant la lecture des champs. Les POST avec paramètres dans l’URL sont refusés pour les routes d’authentification.

`@Inject` demande à WildFly de fournir le service. CDI fait également l’injection du `UserRepository` par le constructeur annoté de `UserService`. `@ApplicationScoped` donne un composant partagé à l’échelle de l’application. Les données de chaque utilisateur restent dans des variables locales ou dans la session courante. Un champ du servlet tel que `private User currentUser;` mélangerait les requêtes des utilisateurs.

`UserService.Profile` est un `record` Java avec les quatre champs publics du profil. Le service le construit à partir de `User`, puis le servlet fabrique explicitement le JSON. L’empreinte reste dans l’entité et dans MySQL.

```java
private static JsonObject profileJson(Profile profile) {
    return Json.createObjectBuilder().add("id", profile.id()).add("nom", profile.nom())
            .add("email", profile.email()).add("role", profile.role().name()).build();
}
```

### 3.2 Entité, JPA et MySQL

Extrait de `User.java`.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(nullable = false, unique = true, length = 150)
private String email;

@Column(name = "password_hash", nullable = false, length = 255)
private String passwordHash;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.CLIENT;
```

`@Entity` enregistre la classe auprès de JPA et `@Table(name = "users")` indique la table. `@Id` marque la clé primaire, générée par MySQL conformément au mapping `IDENTITY`. `@Enumerated(EnumType.STRING)` conserve le nom du rôle dans la colonne, par exemple `CLIENT`. Le constructeur sans argument permet à JPA de reconstruire un objet à la lecture.

Le dépôt obtient son `EntityManager` de l’unité `eventBookingPU` grâce à `@PersistenceContext(unitName = "eventBookingPU")`. JPA définit l’API de persistance, Hibernate l’implémente et le pilote JDBC parle à MySQL. La datasource est le point d’accès géré par WildFly, qui fournit et réutilise des connexions. La configuration de l’application référence la datasource par son nom JNDI.

Extrait de `UserRepository.java`.

```java
public User findByEmail(String email) {
    return em.createQuery(
        "SELECT u FROM User u WHERE lower(u.email) = :email",
        User.class
    )
    .setParameter("email", email)
    .setMaxResults(1)
    .getResultList()
    .stream()
    .findFirst()
    .orElse(null);
}
```

JPQL utilise les noms de l’entité et de ses champs. `setParameter` passe l’email comme une valeur séparée de la requête. `lower(u.email)` permet de comparer une adresse existante à l’adresse normalisée par le service. Le dépôt retourne `null` quand aucune ligne n’est trouvée.

`@Transactional` sur `register` et `updateProfile` place chaque opération d’écriture dans une transaction JTA. À la fin d’une opération réussie, la transaction est validée. Un échec d’écriture empêche sa validation. `persist` rend une nouvelle entité gérée par JPA. `flush` synchronise les écritures avec MySQL pendant la méthode, ce qui permet d’intercepter une erreur SQL avant de répondre. `flush` et la validation de la transaction sont deux étapes distinctes.

**Unicité.** La recherche préalable donne une erreur lisible. Deux requêtes simultanées peuvent toutes deux passer cette recherche. Il faut donc une vraie contrainte unique sur `users.email` dans MySQL, car le démarrage de l’application ne crée pas l’index à partir de `unique = true`. Le service remonte la chaîne des causes via `getCause()` et transforme l’erreur MySQL `1062` en `EMAIL_TAKEN`. Les autres erreurs de persistance remontent au conteneur.

### 3.3 BCrypt et validation

Extrait de `PasswordUtils.java`.

```java
private static final int COST = 12;
private static final int MAX_BYTES = 72;
private static final String DUMMY_HASH = hash(UUID.randomUUID().toString());
```

```java
return BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(COST, password.toCharArray());
```

```java
public static boolean matches(String password, String hash) {
    if (!canVerify(password)) {
        return false;
    }
    var result = BCrypt.verifyer().verify(password.toCharArray(), hash == null ? DUMMY_HASH : hash);
    return hash != null && result.verified;
}
```

BCrypt est un hachage conçu pour les mots de passe. Son coût rend chaque essai plus lent. Le sel fait normalement varier l’empreinte d’un enregistrement à l’autre pour un même mot de passe. L’empreinte encode le sel et le coût, que la bibliothèque réutilise lors de la vérification. Le serveur ne déchiffre rien. Les nouvelles empreintes d’Evently commencent par `$2b$12$`. Avec un email valide mais absent, `DUMMY_HASH` permet quand même d’effectuer un calcul BCrypt, puis `matches` renvoie `false`. Cela réduit la différence de temps avec un mauvais mot de passe pour un compte existant.

| Donnée | Règle dans le code |
| --- | --- |
| Nom | `validateName` retire les blancs en bordure avec `strip()`. Le résultat contient de 1 à 100 points de code Unicode et aucun caractère de contrôle ISO. |
| Email | `normalizeEmail` applique `strip()` et `toLowerCase(Locale.ROOT)` puis contrôle le motif avec `EMAIL.matcher(...).matches()`. Limites de 150 caractères au total et de 64 avant `@`. Le motif attend une adresse de forme ASCII avec un domaine contenant au moins un point. Cette règle vérifie la forme, pas la possession de la boîte. |
| Nouveau mot de passe | Au moins 12 points de code, au plus 72 octets en UTF-8. Le code refuse une chaîne vide, une valeur entièrement blanche et le caractère nul. Il conserve la casse et les espaces présents dans un mot de passe valide. |
| Mot de passe à vérifier | `canVerify` contrôle la présence, le caractère nul et la limite BCrypt. La règle des 12 points de code ne s’applique pas à la connexion ni au mot de passe actuel du profil, pour permettre la vérification de comptes antérieurs. |

Les caractères accentués occupent parfois plusieurs octets. Par exemple, une chaîne de 37 lettres `é` occupe 74 octets en UTF-8 et doit être refusée pour BCrypt.

### 3.4 Session HTTP, cookie et CSRF

HTTP traite chaque requête séparément. Le navigateur renvoie `JSESSIONID` pour que WildFly retrouve l’`HttpSession` côté serveur. Le cookie transporte l’identifiant de session. Les attributs suivants vivent côté serveur.

| Attribut | Valeur |
| --- | --- |
| `authUser` | Présent après la connexion. Contient une `AuthSession.Identity` avec `id` et `role`. Les accès Java sont `identity.id()` et `identity.role()`. En JSP, `sessionScope.authUser.id` et `sessionScope.authUser.role` accèdent au même objet via les getters. |
| `csrfToken` | Chaîne générée par `SecureRandom` à partir de 32 octets aléatoires, encodés en Base64 URL sans remplissage. Résultat de 43 caractères. Présent aussi sur une session anonyme créée par `GET /auth/csrf`. |
| `userId`, `userName`, `userRole` | Attributs exposés aux JSP lorsque la connexion réussit. Le filtre lit `authUser` pour décider des droits. |

`getSession(false)` retrouve une session existante ou renvoie `null`. `getSession(true)` peut en créer une. `invalidate()` détruit la session et rend son identifiant inutilisable pour l’application.

| Réglage | Pourquoi il sert |
| --- | --- |
| `HttpOnly` | Empêche le JavaScript de lire directement le cookie de session via `document.cookie`. Le navigateur continue de joindre le cookie aux requêtes appropriées. |
| `Secure` | Réserve l’envoi de ce cookie à une connexion sécurisée. Dans ce projet, Undertow ajoute l’attribut quand l’échange est HTTPS. Le test local sur HTTP reste possible. HTTPS protège aussi le mot de passe et le jeton pendant le transport. |
| `SameSite=Lax` | Limite l’envoi automatique du cookie dans certaines requêtes entre sites. Un lien de navigation peut encore entraîner un GET. Les actions d’écriture restent des POST protégés par jeton. |
| `COOKIE` | Utilise le cookie pour suivre la session, ce qui évite de transporter l’identifiant dans les URL. |
| `session-timeout` à `30` | Après 30 minutes d’inactivité, il faut ouvrir une nouvelle session et se reconnecter pour retrouver l’accès privé. |

Une attaque **CSRF** consiste à faire déclencher une requête à un navigateur déjà connecté. Le navigateur peut joindre un cookie automatiquement. Le filtre exige donc aussi une valeur que le client légitime a lue depuis l’application, dans `X-CSRF-Token` ou dans le champ de formulaire `csrfToken`. `AuthSession.validCsrf` vérifie la longueur, l’existence de la session et l’égalité avec la valeur enregistrée, via `MessageDigest.isEqual`. Le jeton est réutilisable dans la session actuelle. Le renouvellement de session à la connexion le remplace.

### 3.5 Filtre et autorisations

Extrait d’`AuthFilter.java`.

```java
@WebFilter(urlPatterns = {"/auth/*", "/login", "/register", "/logout",
        "/client/*", "/admin/*", "/agent/*", "/my-reservations", "/my-tickets"})
```

```java
if (!isPublic(path)) {
    if (identity == null) {
        AuthResponse.error(response, 401, "authentication_required");
        return;
    }
    Role required = requiredRole(path);
    if (required != null && identity.role() != required) {
        AuthResponse.error(response, 403, "forbidden");
        return;
    }
}
```

Le filtre reconstitue le chemin à partir de `getServletPath()` et `getPathInfo()`. Les chemins publics sont `/auth/csrf`, `/auth/register`, `/auth/login`, `/register` et `/login`. Les autres chemins couverts exigent une session authentifiée. Les espaces par rôle exigent une égalité exacte. Un ADMIN n’a pas automatiquement accès à l’espace CLIENT. Un GET ou HEAD anonyme sur les deux routes de réservations et de billets redirige vers `/login`.

Le contrôle de session et de rôle a lieu avant le contrôle CSRF. Le filtre contrôle le jeton pour les méthodes autres que GET, HEAD et OPTIONS. Un en-tête fourni a priorité sur le champ de formulaire. Le champ est lu uniquement pour un POST sans query string. `chain.doFilter(request, response)` transmet la requête au composant suivant après les contrôles. Chaque `return` après une réponse d’erreur arrête le traitement. Le filtre ajoute aussi `Cache-Control: no-store` pour éviter la mise en cache de réponses sensibles et `X-Content-Type-Options: nosniff` pour demander au navigateur de respecter le type indiqué.

## 4. Suivre une requête de bout en bout

### 4.1 Obtenir un jeton avant d’agir

1. Le client appelle `GET /auth/csrf`. Le filtre autorise ce chemin sans demander d’identité.
2. Le servlet appelle `request.getSession(true)`. Le conteneur retrouve la session existante ou en ouvre une.
3. `AuthSession.csrfToken` relit le jeton de cette session ou en génère un. Le serveur répond `200` avec `{"csrfToken":"…"}`.
4. Le client garde le cookie `JSESSIONID`. Pour le POST suivant, il transmet à la fois le cookie et le jeton. Un jeton d’une autre session échoue, même s’il a la bonne longueur.

### 4.2 Inscrire un compte

1. Le client envoie `POST /auth/register` avec `nom`, `email`, `password` et le jeton.
2. Le filtre contrôle le jeton. Le servlet vérifie que le corps est un formulaire URL-encodé, puis passe les trois champs à `UserService.register`.
3. Le service valide les champs et appelle `requireAvailableEmail(validEmail, null)`. Le dépôt interroge MySQL. Si un compte possède déjà l’email normalisé, le service s’arrête avec `EMAIL_TAKEN`.
4. Le service construit l’entité. La valeur du rôle est écrite dans le code. Un champ `role` envoyé par le formulaire est ignoré.

```java
requireAvailableEmail(validEmail, null);
User user = new User(validName, validEmail, PasswordUtils.hash(password), Role.CLIENT);
try {
    users.save(user);
} catch (PersistenceException failure) {
    throw translateWriteFailure(failure);
}
return Profile.from(user);
```

5. JPA envoie l’insertion à MySQL et récupère l’identifiant généré. Quand l’appel transactionnel se termine correctement, le servlet répond `201` et expose seulement les quatre champs publics de `user`. La session reste anonyme et le jeton reste utilisable pour le formulaire de connexion.

### 4.3 Connecter un compte

1. Le client envoie `POST /auth/login` à la même origine avec `email`, `password`, le cookie de session et le jeton obtenu précédemment.
2. `AuthFilter` autorise le chemin public puis contrôle le jeton. Si celui-ci est incorrect ou absent, la réponse est immédiatement `403 invalid_csrf`. Aucune recherche de compte n’a lieu.
3. `AuthServlet` appelle `users.authenticate(email, password)`. `UserService` normalise l’email et vérifie que le mot de passe est utilisable par BCrypt. Ensuite `UserRepository.findByEmail` exécute la requête via JPA, Hibernate, la datasource et JDBC.
4. MySQL retourne la ligne correspondante si elle existe, avec son empreinte et son rôle. `PasswordUtils.matches` vérifie le mot de passe reçu. En cas d’email inconnu ou de mauvais mot de passe, la réponse est `401 invalid_credentials`. Le message est commun à ces deux cas. L’inscription, elle, peut annoncer un doublon avec `409`.
5. Après une vérification réussie, le service retourne un `Profile`. Le servlet détruit la session précédente, en crée une et y met le nouveau jeton ainsi que l’identité issue de MySQL.

Extrait du cas `/login` dans `AuthServlet.handleForm`.

```java
Profile profile = users.authenticate(email, request.getParameter("password"));
HttpSession previous = request.getSession(false);
previous.invalidate();
HttpSession session = request.getSession(true);
String csrf = AuthSession.csrfToken(session);
session.setAttribute(AuthSession.IDENTITY, new AuthSession.Identity(profile.id(), profile.role()));
session.setAttribute("userId", profile.id());
session.setAttribute(USER_NAME, profile.nom());
session.setAttribute("userRole", profile.role());
if (isLegacyForm(request)) {
    response.sendRedirect(request.getContextPath() + "/");
} else {
    AuthResponse.json(response, 200, Json.createObjectBuilder()
            .add("user", profileJson(profile)).add("csrfToken", csrf).build());
}
```

6. WildFly émet un nouveau cookie et la réponse `200` contient `user` et `csrfToken`. Le client utilise cette nouvelle paire. Cette étape empêche la **fixation de session**, où un identifiant connu avant la connexion servirait ensuite à accéder au compte.

### 4.4 Consulter ou modifier son profil

- **GET.** Le filtre exige `authUser`. Le servlet passe uniquement `identity.id()` à `UserService.profile`, qui appelle `findById`. Le JSON est construit à partir des valeurs relues en base.
- **POST.** Avec une session et un jeton valides, le servlet appelle `updateProfile` avec l’identifiant de la session, `nom`, `email` et `currentPassword`. Modifier un paramètre `id`, `userId` ou `role` dans le formulaire ne change ni le compte ciblé ni son rôle.
- **Dans le service.** La transaction valide le nom et l’email puis charge le compte avec `LockModeType.PESSIMISTIC_WRITE`. Ce verrou ordonne les modifications concurrentes du même compte. BCrypt vérifie le mot de passe actuel. Le contrôle d’email autorise le compte à garder sa propre adresse. Les setters changent le nom et l’email sur l’entité déjà gérée et Hibernate détecte ces changements. `flush` transmet l’écriture. Le servlet répond `200` avec le nouveau profil.
- **Si la ligne n’existe plus.** `requireUser` lève `ACCOUNT_UNAVAILABLE`. Pour cette consultation ou modification, le servlet invalide la session et répond `401 authentication_required`.

### 4.5 Vérifier les droits et se déconnecter

Pour `GET /auth/access/admin`, une session ADMIN valide franchit le filtre et le servlet renvoie `204`. Une session CLIENT reçoit `403 forbidden`. Sans identité, le filtre renvoie `401 authentication_required`. Le même principe s’applique à CLIENT et AGENT. Sur une URL sous un préfixe protégé, le droit d’accès ne garantit pas qu’une page ou un servlet existe à cette adresse.

Pour `POST /auth/logout`, le filtre demande une identité et le jeton courant. Le servlet appelle `request.getSession(false).invalidate()` puis envoie `204`, avec un corps vide. Le compte reste en base. Le navigateur doit demander un jeton anonyme avec `GET /auth/csrf` avant de se reconnecter.

### 4.6 Tableau des routes

Les chemins commencent après le contexte `/event-booking-platform`. Les formulaires utilisent `application/x-www-form-urlencoded` en UTF-8. Les routes `/auth/*` répondent en `application/json` lorsqu’elles ont un contenu. Envoyer le jeton avec chaque POST. Le [contrat HTTP](docs/auth.md) présente aussi les routes de formulaire `/login`, `/register` et `/logout` et leurs redirections.

| Méthode et chemin | Accès | Champs du formulaire hors CSRF | Succès |
| --- | --- | --- | --- |
| `GET /auth/csrf` | Public | Aucun | `200` et `csrfToken` |
| `POST /auth/register` | Public avec jeton | `nom`, `email`, `password` | `201` et `user` |
| `POST /auth/login` | Public avec jeton | `email`, `password` | `200`, `user`, nouveau `csrfToken` et nouveau cookie |
| `GET /auth/profile` | Tout rôle connecté | Aucun | `200` et `user` |
| `POST /auth/profile` | Tout rôle connecté avec jeton | `nom`, `email`, `currentPassword` | `200` et profil actualisé |
| `POST /auth/logout` | Tout rôle connecté avec jeton | Aucun | `204` |
| `GET /auth/access/client` | CLIENT | Aucun | `204` |
| `GET /auth/access/admin` | ADMIN | Aucun | `204` |
| `GET /auth/access/agent` | AGENT | Aucun | `204` |

Exemple de profil renvoyé à l’inscription ou sur `GET /auth/profile`. L’identifiant est généré par MySQL et varie.

```json
{
  "user": {
    "id": 42,
    "nom": "Sam",
    "email": "sam@example.test",
    "role": "CLIENT"
  }
}
```

## 5. Refaire cette partie depuis zéro

Travaille dans un projet d’exercice ou dans une copie de travail dédiée. Les extraits plus haut viennent du projet et les liens permettent de vérifier le code complet. Termine et vérifie chaque couche avant de passer à la suivante.

### Étape 1 — Préparer Maven, MySQL et WildFly

1. Prépare un JDK qui permet de compiler avec `release 21`, Maven, WildFly 41 et un serveur MySQL. Vérifie le JDK utilisé avec `mvn -version`.
2. Dans [pom.xml](pom.xml), choisis `war` pour le packaging, `21` pour `maven.compiler.release` et `event-booking-platform` pour `finalName`. Déclare `jakarta.platform:jakarta.jakartaee-api:11.0.0` en `provided` et `at.favre.lib:bcrypt:0.10.2` en scope normal. Ajoute JUnit Jupiter, Mockito et Parsson en scope `test`, avec les versions du fichier du projet. Parsson fournit l’implémentation JSON-P lors des tests Maven, qui tournent hors de WildFly.
3. Configure dans WildFly le pilote MySQL et la datasource `EventBookingDS`. Pour reproduire l’installation locale d’Evently, place `module.xml` et `mysql-connector-j-8.0.32.jar` ensemble dans `modules/com/mysql/main/`, dans le dossier WildFly. Déclare le module `com.mysql`, son JAR et les cinq dépendances Java listées dans la section 7. Dans le sous-système `datasources` de `standalone.xml`, enregistre le pilote `mysql` avec le module `com.mysql` et les classes `com.mysql.cj.jdbc.Driver` et `com.mysql.cj.jdbc.MysqlXADataSource`. Associe ce pilote à la datasource de pool `EventBookingDS`, au nom JNDI `java:jboss/datasources/EventBookingDS` et à une URL JDBC vers ta base. L’installation locale pointe vers `jdbc:mysql://localhost:3306/event_booking` avec ses paramètres de connexion.
4. Fournis les identifiants MySQL de ton environnement au serveur. Dans la configuration locale, le mot de passe vient de l’expression `${env.MYSQL_PASSWORD}`, donc la variable doit être présente dans le processus au démarrage. Garde sa valeur hors des fichiers du projet et de l’historique des commandes.
5. Dans une base existante, vérifie la table à l’aide de `SHOW CREATE TABLE users;`. Sur une base d’exercice vide, prépare le schéma avant le déploiement d’après le mapping suivant. Ce SQL est un exemple de reconstruction, pas un fichier exécuté par Evently.

```sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (email)
) DEFAULT CHARACTER SET utf8mb4;
```

6. Crée `src/main/resources/META-INF/persistence.xml`. Déclare l’unité `eventBookingPU` avec `transaction-type="JTA"`, la datasource JNDI, la classe `com.eventbooking.model.User`, `exclude-unlisted-classes` à `true` et `jakarta.persistence.schema-generation.database.action` à `none`. Reprends le fichier existant pour le namespace XML et les propriétés Hibernate. Dans le même dossier crée `beans.xml` avec `bean-discovery-mode="annotated"`.

### Étape 2 — Écrire le modèle et le dépôt

1. Crée `model/Role.java` avec `public enum Role { CLIENT, ADMIN, AGENT }`.
2. Crée `model/User.java`. Écris les annotations et champs de la section 3.2. Ajoute `@Column(nullable = false, length = 100) private String nom;`, le constructeur sans argument, celui qui prend `nom`, `email`, `passwordHash` et `role`, puis les getters et setters de l’entité existante. L’identifiant commence à `null` avant la création et est renseigné par JPA.
3. Crée `repository/UserRepository.java`. Mets `@ApplicationScoped` sur la classe et injecte `EntityManager` avec `@PersistenceContext(unitName = "eventBookingPU")`. Écris la requête paramétrée `findByEmail` de la section 3.2. Pour `findById`, retourne `em.find(User.class, id)`. Pour `findByIdForUpdate`, passe en troisième argument `LockModeType.PESSIMISTIC_WRITE`. Dans `save`, annotée `@Transactional`, appelle `em.persist(user)` puis `em.flush()`. Fais aussi une méthode `flush()` que le service appellera après une modification.

### Étape 3 — Écrire BCrypt et le service

1. Crée `security/PasswordUtils.java` comme classe utilitaire `final` avec constructeur privé. Reprends `COST`, `MAX_BYTES` et `DUMMY_HASH`. Dans `canVerify`, vérifie la présence, la chaîne non vide, l’absence de `'\0'` et les longueurs Java et UTF-8. Dans `hash`, refuse une valeur invalide puis utilise l’expression BCrypt de la section 3.3. Écris `matches` avec la vérification factice et la condition finale `hash != null && result.verified`.
2. Crée `service/UserService.java` avec `@ApplicationScoped`, un constructeur protégé sans argument et un constructeur `@Inject` prenant `UserRepository`. Ajoute le record `Profile`, l’énumération `Problem` et l’exception `Failure extends RuntimeException` avec son accesseur `problem()`.
3. Écris les validations avant les quatre méthodes publiques. Place le motif `EMAIL` en constante et utilise le motif exact du fichier [UserService.java](src/main/java/com/eventbooking/service/UserService.java). `normalizeEmail` retourne `null` si la forme ne convient pas. `validateEmail` lève alors `INVALID_EMAIL`. Pour `validateName`, applique la règle du tableau. `requireUser` lève `ACCOUNT_UNAVAILABLE` si la recherche ne trouve rien. `requireAvailableEmail` refuse tout propriétaire différent de l’identifiant courant.
4. Écris `register` en suivant la section 4.2 et `authenticate` en suivant la section 4.3. La conversion en `Profile` doit lire uniquement l’identifiant, le nom, l’email et le rôle. Utilise le rôle fixe `Role.CLIENT` à l’inscription et le rôle de l’entité à la connexion.
5. Écris `profile` puis `updateProfile` d’après la section 4.4. Annote `register` et `updateProfile` avec `@Transactional`. Encadre `save` et `flush` avec `catch (PersistenceException failure)` et remonte la chaîne des causes dans `translateWriteFailure` pour convertir le code MySQL `1062`. Remonte l’exception initiale pour les autres codes.

### Étape 4 — Écrire les outils de session et de réponse

1. Crée `security/AuthSession.java` avec les clés publiques `IDENTITY = "authUser"` et `CSRF = "csrfToken"`. Écris `record Identity(long id, Role role) implements Serializable` et ses deux getters pour les JSP.
2. Dans `identity`, teste d’abord que la session existe. Retourne l’attribut uniquement s’il est de type `Identity`. Dans `csrfToken`, utilise un bloc `synchronized (session)`, relis l’attribut et crée-le seulement s’il manque. Appelle `SecureRandom.nextBytes` sur un tableau neuf de 32 octets. Encode-le avec `Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)` et conserve-le dans la session. Dans `validCsrf`, contrôle la session, la présence et la longueur du jeton fourni, puis compare les octets UTF-8 avec `MessageDigest.isEqual`.
3. Crée `security/AuthResponse.java`. Dans `json`, appelle `setStatus`, `setContentType("application/json")` et `setCharacterEncoding("UTF-8")` avant `getWriter().write(body.toString())`. Dans `error`, appelle `json` avec un objet créé par `Json.createObjectBuilder().add("error", code).build()`.
4. Sous `src/main/webapp/WEB-INF/`, crée `web.xml` pour le délai d’inactivité, `HttpOnly` et le mode `COOKIE`. Reprends le [descripteur existant](src/main/webapp/WEB-INF/web.xml), qui utilise le namespace Jakarta et la version Servlet 6.1. Dans `undertow-handlers.conf`, écris les deux lignes existantes.

```text
samesite-cookie(mode=Lax, cookie-pattern=JSESSIONID)
secure() -> secure-cookie()
```

### Étape 5 — Écrire le filtre et le servlet

1. Crée `security/AuthFilter.java` avec les motifs `@WebFilter` de la section 3.5. Implémente les méthodes privées `isPublic`, `requiredRole` et `isClientPage`, puis `doFilter`. Dans cette méthode, reconstruis le chemin, règle les en-têtes de réponse et l’encodage, lis la session sans en créer, puis vérifie l’identité et le rôle. Pour une méthode qui modifie l’état, vérifie l’absence de query string sur les routes d’authentification, lis le jeton et contrôle-le. Termine par `chain.doFilter` seulement si la demande a passé les contrôles.
2. Crée `controller/AuthServlet.java` avec la déclaration `@WebServlet` de la section 3.1 et `@Inject UserService`. Dans `doGet`, gère le jeton, le profil, les trois contrôles de rôle et le transfert vers les JSP lorsqu’elles existent. Dans `doPost`, exige le bon type de formulaire pour l’inscription, la connexion et la modification du profil. Implémente aussi la déconnexion. Pour la connexion, reproduis l’ordre de la section 4.3 afin que l’ancienne session soit invalidée après vérification des identifiants.
3. Ajoute `path`, `isForm`, `methodNotAllowed`, `userBody` et `profileJson`. Pour les erreurs du service, utilise `respondToFailure` avec la correspondance de la section 7. Au moment d’écrire la réponse, décide explicitement si elle doit avoir un corps JSON ou un statut `204`.

### Étape 6 — Écrire les tests et déployer

1. Crée les trois classes de test décrites ci-dessous. Simule les autres composants avec Mockito et vérifie chaque action avec JUnit. Exécute `mvn clean verify` depuis la racine.
2. Sous Windows, démarre WildFly avec `bin\standalone.bat` si le serveur est arrêté. Lance `bin\jboss-cli.bat --connect`. Exécute `/subsystem=datasources/data-source=EventBookingDS:test-connection-in-pool` et vérifie `"result" => [true]`.
3. Dans la CLI WildFly, déploie `deploy "C:/chemin/vers/event-booking-platform/target/event-booking-platform.war"`. Si ce WAR est déjà déployé, utilise `--force` pour le remplacer. Vérifie avec `/deployment=event-booking-platform.war:read-attribute(name=status)`. La valeur attendue est `OK`.
4. Fais le parcours HTTP de la section suivante. Un déploiement `OK` ou une page d’accueil `200` confirme que l’application est disponible. Le parcours d’authentification vérifie en plus la table MySQL, les écritures, les droits et la validité des cookies.

## 6. Tester la partie authentification

### 6.1 Tests Maven

Depuis la racine du projet, lancer `mvn clean verify`. Les trois classes sont sous `src/test/java/com/eventbooking/`.

| Classe | Ce qu’elle vérifie |
| --- | --- |
| [UserServiceTest](src/test/java/com/eventbooking/service/UserServiceTest.java) | Normalisation et validation, création automatique d’un CLIENT, vérification de l’empreinte BCrypt, doublon trouvé et erreur de doublon MySQL simulée, identifiants valides ou invalides, changement de profil et conservation du rôle et de l’empreinte. |
| [AuthServletTest](src/test/java/com/eventbooking/controller/AuthServletTest.java) | Ordre d’invalidation et de création de session, enregistrement de l’identité et du jeton, identifiant du profil obtenu via la session, déconnexion. |
| [AuthFilterTest](src/test/java/com/eventbooking/security/AuthFilterTest.java) | Refus d’une session anonyme, matrice des trois rôles, préfixe protégé, jetons manquants ou incorrects, jeton fourni en en-tête ou dans le corps, refus des paramètres transmis dans l’URL. |

Les rapports Maven se trouvent dans `target/surefire-reports/`. Ces tests isolent les classes avec Mockito. Ils vérifient les décisions du code, y compris l’obligation d’un POST avec jeton sur les routes de formulaire et la protection des routes de réservation et de billets. Les tests HTTP ci-dessous font intervenir le vrai conteneur, le navigateur simulé par PowerShell et MySQL.

### 6.2 Préparer un client HTTP PowerShell avec des cookies

Lance les blocs dans la même console PowerShell, dans l’ordre. Le script génère un compte CLIENT d’exercice et le laisse en base. Réserve ce parcours à une base locale ou de test. Pour les erreurs, le script affiche le statut et, si PowerShell fournit le JSON, son code d’erreur.

```powershell
$base = 'http://localhost:8080/event-booking-platform'
$uri = [uri]"$base/"
$browser = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Send-Auth {
    param(
        [string]$Method,
        [string]$Path,
        $Session,
        [hashtable]$Form,
        [string]$Token
    )

    $argsHttp = @{
        Uri = "$base$Path"
        Method = $Method
        WebSession = $Session
        UseBasicParsing = $true
        ErrorAction = 'Stop'
    }
    if ($PSBoundParameters.ContainsKey('Form')) {
        $argsHttp.Body = $Form
        $argsHttp.ContentType = 'application/x-www-form-urlencoded; charset=UTF-8'
    }
    if ($Token) {
        $argsHttp.Headers = @{ 'X-CSRF-Token' = $Token }
    }

    try {
        $response = Invoke-WebRequest @argsHttp
        $data = if ($response.Content) { $response.Content | ConvertFrom-Json }
        [pscustomobject]@{ Status = [int]$response.StatusCode; Data = $data }
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        if ($status -eq 0) { throw }
        $data = if ($_.ErrorDetails.Message) {
            $_.ErrorDetails.Message | ConvertFrom-Json -ErrorAction SilentlyContinue
        }
        [pscustomobject]@{ Status = $status; Data = $data }
    }
}

function Show-AuthResult($Result) {
    [pscustomobject]@{ Status = $Result.Status; Erreur = $Result.Data.error }
}

$suffix = [guid]::NewGuid().ToString('N')
$email = "evently-$suffix@example.test"
$newEmail = "evently-profil-$suffix@example.test"
$testPassword = [guid]::NewGuid().ToString('N')
$register = @{ nom = ' Sam '; email = $email.ToUpperInvariant(); password = $testPassword; role = 'ADMIN' }
$login = @{ email = $email; password = $testPassword }
```

`WebRequestSession` garde les cookies reçus et les renvoie aux bonnes URL. Garde toujours le même hôte dans ce parcours. Alterner `localhost` et `127.0.0.1` peut changer le cookie envoyé.

### 6.3 Inscrire, vérifier la session et se connecter

```powershell
Show-AuthResult (Send-Auth GET /auth/profile $browser)                 # 401

$bootstrap = Send-Auth GET /auth/csrf $browser
$bootstrap.Status                                                     # 200
$token = $bootstrap.Data.csrfToken
$before = $browser.Cookies.GetCookies($uri)['JSESSIONID'].Value

Show-AuthResult (Send-Auth POST /auth/register $browser -Form $register) # 403 invalid_csrf
$created = Send-Auth POST /auth/register $browser -Form $register -Token $token
$created.Status                                                       # 201
$created.Data.user                                                    # nom Sam, email minuscule, rôle CLIENT
Show-AuthResult (Send-Auth GET /auth/profile $browser)                 # 401, toujours anonyme
Show-AuthResult (Send-Auth POST /auth/register $browser -Form $register -Token $token) # 409

$badLogin = @{ email = $email; password = "$testPassword-x" }
Show-AuthResult (Send-Auth POST /auth/login $browser -Form $badLogin -Token $token) # 401

$connected = Send-Auth POST /auth/login $browser -Form $login -Token $token
$connected.Status                                                     # 200
$after = $browser.Cookies.GetCookies($uri)['JSESSIONID'].Value
$activeToken = $connected.Data.csrfToken
($before -ne $after) -and ($token -ne $activeToken)                     # True
```

Pour vérifier l’alternative du champ caché, recommence avec un email de test distinct et ajoute `$register.csrfToken = $token` avant le POST dans une session anonyme. Omettre `-Token` donne alors le même résultat `201`. Pour vérifier la liaison à la session, ouvre une deuxième `WebRequestSession`, récupère son jeton et utilise-le avec le cookie de la première pour un POST d’inscription ou de connexion. Le résultat attendu est `403 invalid_csrf`.

### 6.4 Vérifier les rôles, le profil et la déconnexion

```powershell
Show-AuthResult (Send-Auth GET /auth/access/client $browser)            # 204
Show-AuthResult (Send-Auth GET /auth/access/admin $browser)             # 403 forbidden
Show-AuthResult (Send-Auth GET /auth/access/agent $browser)             # 403 forbidden
Show-AuthResult (Send-Auth GET /admin/reports $browser)                 # 403, filtre de préfixe

$profile = Send-Auth GET /auth/profile $browser
$profile.Status                                                       # 200
$profile.Data.user                                                     # même identifiant que l’inscription

$edit = @{ nom = 'Sam mis à jour'; email = $newEmail; currentPassword = "$testPassword-x"; userId = '-1'; role = 'AGENT' }
Show-AuthResult (Send-Auth POST /auth/profile $browser -Form $edit)       # 403 invalid_csrf
Show-AuthResult (Send-Auth POST /auth/profile $browser -Form $edit -Token $activeToken) # 401
$edit.currentPassword = $testPassword
$edited = Send-Auth POST /auth/profile $browser -Form $edit -Token $activeToken
$edited.Status                                                         # 200
$edited.Data.user                                                       # nouveau nom et email, même identifiant et rôle CLIENT

function Browser-WithCookie([string]$Id) {
    $jar = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $cookie = [System.Net.Cookie]::new('JSESSIONID', $Id, '/event-booking-platform')
    $jar.Cookies.Add($uri, $cookie)
    $jar
}

$oldBrowser = Browser-WithCookie $before
Show-AuthResult (Send-Auth GET /auth/profile $oldBrowser)                # 401, ancien cookie
Show-AuthResult (Send-Auth POST /auth/logout $browser -Token $token)       # 403, ancien jeton
Show-AuthResult (Send-Auth POST /auth/logout $browser -Token $activeToken) # 204
$replayBrowser = Browser-WithCookie $after
Show-AuthResult (Send-Auth GET /auth/profile $replayBrowser)             # 401, session invalidée
```

Ensuite, redemande un jeton à `GET /auth/csrf`. Une connexion avec l’ancien email doit donner `401 invalid_credentials`. Avec `$newEmail` et le même mot de passe, elle doit donner `200`. Utilise la réponse de cette dernière connexion pour te déconnecter à nouveau.

Si tu disposes déjà de comptes de test ADMIN et AGENT, connecte-les chacun avec une nouvelle session et exécute les trois GET de contrôle de rôle. Le tableau donne les résultats attendus. Sans compte privilégié de test, `AuthFilterTest` vérifie toutes les cases avec des sessions simulées.

| Rôle de la session | `/auth/access/client` | `/auth/access/admin` | `/auth/access/agent` |
| --- | --- | --- | --- |
| CLIENT | 204 | 403 | 403 |
| ADMIN | 403 | 204 | 403 |
| AGENT | 403 | 403 | 204 |
| Anonyme | 401 | 401 | 401 |

### 6.5 Compléter les vérifications

| Essai concret | Résultat attendu |
| --- | --- |
| Inscription avec nom composé uniquement d’espaces, email `invalide` ou mot de passe `'x' * 11`. Faire trois essais avec un jeton valide, un seul champ invalide à la fois. | `400 invalid_name`, `400 invalid_email` ou `400 invalid_password`, respectivement. |
| Inscription avec `'é' * 37` comme mot de passe et des champs valides. | `400 invalid_password` car la chaîne dépasse 72 octets en UTF-8. |
| POST vers `/auth/login?email=sam@example.test` depuis une session anonyme avec un jeton valide. Utiliser une adresse d’exemple, jamais un mot de passe dans l’URL. | `400 unexpected_query`. |
| POST vers `/auth/login` avec `Content-Type: application/json` et un en-tête CSRF valide. | `415 unsupported_content_type`. Le jeton valide permet d’atteindre la vérification du format dans le servlet. |
| `GET /auth/register` sans session. | `405 method_not_allowed` et en-tête `Allow: POST`. |
| Profil après 30 minutes sans requête pour cette session. | `401 authentication_required`. |
| Vérifier `Set-Cookie` sur `GET /auth/csrf` dans l’onglet Réseau du navigateur et refaire la vérification en HTTPS local, sur `https://localhost:8443/event-booking-platform/auth/csrf` si ce connecteur est actif. | `HttpOnly` et `SameSite=Lax`. En HTTPS, `Secure` doit aussi être présent. |
| Exécuter en lecture seule `SELECT id, nom, email, role, LEFT(password_hash, 7) AS bcrypt_prefix FROM users WHERE email = 'adresse-de-test';` avec l’email exact du compte créé. | Une ligne CLIENT et le préfixe `$2b$12$`, sans afficher l’empreinte complète. |

## 7. Erreurs fréquentes et résolution

| Symptôme | Explication et vérification à faire |
| --- | --- |
| `401 authentication_required` en ouvrant le profil | Le filtre ne trouve pas d’identité en session. Vérifie le cookie, l’hôte et le chemin du contexte. Reconnecte-toi après une expiration ou une déconnexion. Sur le profil, ce code apparaît aussi si la ligne du compte a disparu. |
| `401 invalid_credentials` pendant le login ou l’édition du profil | Vérifie l’email et le mot de passe correspondant. L’édition attend `currentPassword`. En cas de changement d’email, utilise la nouvelle adresse pour la prochaine connexion. |
| `403 invalid_csrf` | Demande un jeton via `GET /auth/csrf`, réutilise le même cookie et envoie le jeton dans `X-CSRF-Token` ou le champ `csrfToken`. Après connexion, remplace le jeton initial par celui de la réponse. Vérifie aussi que l’en-tête transmis ne contient pas un ancien jeton qui prendrait la priorité sur le champ. |
| `403 forbidden` | La session est reconnue et son rôle ne correspond pas à la route. Vérifie le rôle renvoyé à la connexion ou par le profil. Relance une connexion après un changement du rôle en base pour renouveler l’identité utilisée par le filtre. |
| `400 invalid_name`, `invalid_email` ou `invalid_password` | Relis les limites du tableau de validation. Pour le mot de passe, compte les octets UTF-8 en plus des caractères. |
| `400 unexpected_query` | Retire les paramètres de l’URL du POST d’authentification. Envoie les champs dans le corps et le jeton dans le corps ou l’en-tête prévu. |
| `409 email_taken` | L’adresse est déjà utilisée après normalisation. Essaie une adresse distincte. Pour une modification de profil, tu peux conserver l’adresse déjà attribuée à ton propre compte. |
| `415 unsupported_content_type` | Envoie un formulaire `application/x-www-form-urlencoded`. En JavaScript, un corps `new URLSearchParams({ email, password })` convient. Le JSON brut est un format différent. |
| `404` ou `405` | Compare la méthode et le contexte au tableau des routes. Sur un chemin protégé inconnu, le filtre peut répondre `401` avant que le servlet puisse produire `404`. Pour un GET explicitement refusé, lis l’en-tête `Allow`. |
| `500` pendant une requête d’authentification | Regarde la cause dans `standalone/log/server.log`. Vérifie le déploiement, le pool `EventBookingDS` et `SHOW CREATE TABLE users;`. Une datasource peut répondre au test de connexion alors que la table ou une colonne manque. |
| `WFLYJCA0115` mentionne `com.mysql` ou `jakarta.api` | Compare le nom du module, le fichier JAR déclaré dans `module.xml` et les modules référencés. Lors de la correction de WildFly 41, la dépendance `jakarta.api` absente a été remplacée, avec l’ancienne dépendance de transaction, par `java.management`, `java.naming`, `java.security.sasl`, `java.sql` et `java.transaction.xa`. |
| `WFLYCTL0113` sur `password`, erreur d’expression ou `Access denied` MySQL | Vérifie que le processus WildFly reçoit `MYSQL_PASSWORD` et que le compte configuré a accès à `event_booking`. Un mot de passe vide ou une variable absente ne fournit pas un accès valide. Garde la valeur hors de la console partagée et de Git. |
| `WFLYSRV0056` | C’est le bilan d’un démarrage échoué. Remonte dans le journal jusqu’à la première cause, par exemple un module manquant ou un problème de paramètre, puis redémarre après sa correction. |
| Échec de compilation avec `release version 21 not supported` | Exécute `mvn -version`. Corrige le JDK utilisé par Maven, puis relance `mvn clean verify`. |
| Injection CDI ou unité de persistance introuvable | Vérifie l’emplacement des deux fichiers `META-INF`, l’annotation `@ApplicationScoped`, les imports `jakarta.*` et l’orthographe de `eventBookingPU` ainsi que du nom JNDI. Assure-toi de tester le WAR reconstruit. |

Pour retrouver rapidement l’origine d’une erreur, vérifie dans cet ordre la méthode et l’URL, le cookie, le jeton et le rôle, les champs du formulaire, puis WildFly et MySQL. Cette démarche suit les contrôles que la requête traverse réellement.
