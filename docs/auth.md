# Intégration de l’authentification Evently

Préfixe local `http://localhost:8080/event-booking-platform`. Construire les URL à partir du contexte de l’application, par exemple avec `${pageContext.request.contextPath}` dans une JSP. Utiliser HTTPS hors du développement local.

Les POST transmettent leurs paramètres dans le corps UTF-8 au format `application/x-www-form-urlencoded`. Le backend répond en JSON UTF-8, avec un corps vide pour les réponses `204`. Il ne redirige pas. L’interface choisit ses pages et ses messages à partir du statut et du code d’erreur.

| Méthode et chemin | Accès | Paramètres du corps | Succès |
| --- | --- | --- | --- |
| `GET /auth/csrf` | Public ou connecté | Aucun | `200` avec `{"csrfToken":"…"}` |
| `POST /auth/register` | Public | `nom`, `email`, `password` | `201` avec `{"user":{…}}`. L’utilisateur est `CLIENT` et peut ensuite se connecter. |
| `POST /auth/login` | Public | `email`, `password` | `200` avec `{"user":{…},"csrfToken":"…"}` et un nouveau cookie de session |
| `POST /auth/logout` | Tout utilisateur connecté | Aucun, hors jeton CSRF dans un formulaire | `204`. La session est invalidée. |
| `GET /auth/profile` | Tout utilisateur connecté | Aucun | `200` avec `{"user":{…}}` |
| `POST /auth/profile` | Tout utilisateur connecté | `nom`, `email`, `currentPassword` | `200` avec le profil actualisé |
| `GET /auth/access/client` | `CLIENT` | Aucun | `204` |
| `GET /auth/access/admin` | `ADMIN` | Aucun | `204` |
| `GET /auth/access/agent` | `AGENT` | Aucun | `204` |

Chaque objet `user` expose `id` (entier), `nom`, `email` et `role` (`CLIENT`, `ADMIN` ou `AGENT`). Le profil modifiable est déterminé par la session. Les paramètres supplémentaires comme `id`, `userId` et `role` sont ignorés. Les préfixes `/client/*`, `/admin/*` et `/agent/*` exigent le rôle correspondant. Les trois rôles ont accès à leur propre profil.

## Routes pour les formulaires JSP

`GET /login` et `GET /register` chargent respectivement `WEB-INF/views/auth/login.jsp` et `WEB-INF/views/auth/register.jsp` lorsque les JSP existent. Le servlet leur transmet `csrfToken`. En l’absence de la JSP, la réponse est `404`. Les routes JSON restent disponibles pour une interface indépendante.

Les formulaires peuvent envoyer leurs champs à `POST /login` ou `POST /register` avec le jeton `csrfToken`. Une connexion réussie redirige vers l’accueil de l’application. Une inscription réussie redirige vers `/login?registered=1`. En cas d’erreur métier, une JSP présente est réaffichée avec un attribut `error`. Après une connexion par formulaire, le nouveau jeton est disponible dans la session ou via `GET /auth/csrf`.

`POST /logout` avec le jeton invalide la session et redirige vers `/login`. Pour une session authentifiée, `GET /logout` répond `405`. Les routes `/my-reservations` et `/my-tickets` exigent le rôle `CLIENT`. Un GET ou HEAD anonyme sur ces routes redirige vers `/login`.

## Validation

- Nom obligatoire après suppression des espaces en début et fin, 100 caractères Unicode au maximum et sans caractères de contrôle.
- Email ASCII au format `partie-locale@domaine.extension`, 150 caractères au maximum, dont au plus 64 avant le `@`. Il est enregistré en minuscules après suppression des espaces en début et fin. Il doit être unique.
- À l’inscription, le mot de passe doit compter au moins 12 caractères Unicode et au plus 72 octets UTF-8, sans caractère nul ni valeur composée uniquement d’espaces. Les mots de passe existants plus courts peuvent être vérifiés à la connexion. Les mots de passe sont sensibles à la casse et conservés sous forme d’empreintes BCrypt.
- Toute modification du nom ou de l’email exige le mot de passe actuel dans `currentPassword`.

## Session et CSRF

1. Appeler `GET /auth/csrf` avant d’afficher ou d’envoyer un formulaire. Garder le cookie `JSESSIONID` dans le navigateur et utiliser le jeton renvoyé.
2. Envoyer le jeton avec **chaque POST**, via `X-CSRF-Token` pour `fetch` ou le champ caché `csrfToken` pour un formulaire. Les POST d’authentification refusent les paramètres d’URL. Utiliser les routes sur la même origine et laisser le navigateur gérer le cookie.
3. Après connexion, remplacer le jeton précédent par celui de la réponse. Le cookie de session est renouvelé et le jeton précédent expire. Après déconnexion ou expiration, demander un nouveau jeton pour le prochain formulaire public.

Le cookie est `HttpOnly`, `SameSite=Lax` et `Secure` sur HTTPS. La session expire après 30 minutes d’inactivité. Ses attributs serveur sont `authUser`, pour l’identifiant et le rôle (`${sessionScope.authUser.id}`, `${sessionScope.authUser.role}` en JSP), et `csrfToken` (`${sessionScope.csrfToken}`). Une session anonyme contient uniquement le jeton.

Après une connexion, les formulaires JSP peuvent également lire les attributs `userId`, `userName` et `userRole`. Le filtre décide des droits à partir de `authUser`.

## Erreurs du backend

| Statut | `error` | Signification |
| --- | --- | --- |
| `400` | `invalid_name`, `invalid_email`, `invalid_password` | Donnée invalide |
| `400` | `unexpected_query` | Paramètres transmis dans l’URL d’un POST d’authentification |
| `401` | `authentication_required` | Session authentifiée requise ou expirée |
| `401` | `invalid_credentials` | Connexion refusée ou mot de passe actuel incorrect |
| `403` | `forbidden` | Rôle insuffisant |
| `403` | `invalid_csrf` | Jeton CSRF absent, incorrect ou expiré |
| `404` | `not_found` | Route inconnue dans le servlet d’authentification |
| `405` | `method_not_allowed` | Méthode GET/POST incorrecte pour la route, voir l’en-tête `Allow` |
| `409` | `email_taken` | Email déjà utilisé |
| `415` | `unsupported_content_type` | Format du corps différent d’un formulaire URL-encodé |

Par exemple, pour une connexion après récupération du jeton, envoyer `new URLSearchParams({ email, password })` comme corps de `fetch`, avec `method: "POST"`, `credentials: "same-origin"` et `headers: { "X-CSRF-Token": csrfToken }`.
