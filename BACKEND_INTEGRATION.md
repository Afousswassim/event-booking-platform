# DOCUMENTATION D'INTÉGRATION BACKEND — EVENT BOOKING PLATFORM

> **Auteur** : Développeur 2 (Backend, Base de données & Logique Métier)  
> **Branche de référence** : `feature/backend`  
> **Cible** : Développeur 1 (Authentification) & Développeur 3 (Frontend & QR Codes)  
> **Environnement** : Java 21, Jakarta EE 10, WildFly 41, MySQL 8, JPA/Hibernate, CDI

---

## 1. Vue d'Ensemble de l'Architecture Backend

Le Backend expose une architecture en couches MVC :
- **Entités JPA** : `Category`, `Event`, `Reservation`, `Ticket` (et le contrat partagé `User`).
- **Repositories JPA** : Injection via `@PersistenceContext`, requêtes JPQL typées et gestion fine des verrous.
- **Services Métier CDI** : Annotés `@ApplicationScoped` et `@Transactional`, assurant la cohérence métier et l'isolation transactionnelle.

---

## 2. Guide d'Intégration pour le Développeur 1 (Authentification)

### Contrat partagé : Entité `User`
L'entité `User` créée par le Développeur 1 est directement liée à nos réservations.
- **Table associée** : `users`
- **Champs utilisés par le Backend** : `id` (Long, clé primaire).
- **Règle pour les réservations** : Tout appel à `ReservationService.reserver(userId, eventId, quantite)` vérifie l'existence de l'utilisateur via `UserRepository.findById(userId)`. Si l'utilisateur n'existe pas, une `IllegalArgumentException` est levée.
- **Récupération des réservations d'un utilisateur connecté** :
  ```java
  @Inject
  private ReservationService reservationService;

  // Depuis votre AuthServlet / session utilisateur :
  Long currentUserId = user.getId();
  List<Reservation> userReservations = reservationService.getReservationsByUser(currentUserId);
  ```

---

## 3. Guide d'Intégration pour le Développeur 3 (Frontend & Contrôleurs JSP)

Les services ci-dessous sont injectables dans vos Servlets ou Managed Beans via `@Inject`.

### 3.1. `EventService` (Gestion & Consultation des Événements)

| Méthode | Paramètres | Type de retour | Exceptions possibles | Rôle |
|---|---|---|---|---|
| `getPublishedEvents()` | Aucun | `List<Event>` | Aucune | Récupère tous les événements publiés à afficher sur la page d'accueil ou catalogue. |
| `getEventById(id)` | `Long id` | `Event` | Aucune (null si non trouvé) | Détails complets d'un événement. |
| `getEventsByCategory(categoryId)` | `Long categoryId` | `List<Event>` | Aucune | Filtrage par catégorie. |
| `getAvailablePlaces(eventId)` | `Long eventId` | `int` | `IllegalArgumentException` | Retourne le nombre exact de places encore disponibles en temps réel. |
| `getAllCategories()` | Aucun | `List<Category>` | Aucune | Liste déroulante des catégories pour les formulaires. |
| `createEvent(event, categoryId)` | `Event event, Long categoryId` | `Event` | `IllegalArgumentException` | Création d'un événement (statut initial `BROUILLON`). |
| `publishEvent(eventId)` | `Long eventId` | `Event` | `IllegalStateException` | Publication de l'événement (devient réservable). |
| `cancelEvent(eventId)` | `Long eventId` | `Event` | `IllegalStateException` | Annulation de l'événement. |

---

### 3.2. `ReservationService` (Logique de Réservation & Concurrence)

| Méthode | Paramètres | Type de retour | Exceptions possibles | Rôle |
|---|---|---|---|---|
| `reserver(userId, eventId, quantite)` | `Long userId, Long eventId, int quantite` | `Reservation` | `IllegalArgumentException`, `IllegalStateException` | Effectue une réservation transactionnelle avec verrou pessimiste d'écriture. Génère un ticket numérique unique par place. |
| `annulerReservation(reservationId)` | `Long reservationId` | `Reservation` | `IllegalArgumentException`, `IllegalStateException` | Annule la réservation et bascule tous ses tickets non utilisés au statut `ANNULE`. |
| `getReservationsByUser(userId)` | `Long userId` | `List<Reservation>` | `IllegalArgumentException` | Historique des réservations du client. |
| `getReservationById(reservationId)` | `Long reservationId` | `Reservation` | `IllegalArgumentException` | Consultation d'une réservation spécifique. |

> [!IMPORTANT]
> **Gestion de la Concurrence** : Le service utilise `LockModeType.PESSIMISTIC_WRITE` sur l'événement en base. Si 2 utilisateurs tentent de réserver simultanément les dernières places, la première transaction valide l'achat et la seconde est automatiquement rejetée avec une `IllegalStateException` ("Capacité insuffisante"). Vos contrôleurs doivent capturer cette exception pour afficher un message clair à l'utilisateur.

---

### 3.3. `TicketService` (Validation & Contrôle d'Accès QR Code)

| Méthode | Paramètres | Type de retour | Exceptions possibles | Rôle |
|---|---|---|---|---|
| `getTicketByCode(codeUnique)` | `String codeUnique` | `Ticket` | `IllegalArgumentException` | Retrouve le ticket par son code unique (lu depuis le QR Code). |
| `getTicketsByReservation(resId)` | `Long resId` | `List<Ticket>` | `IllegalArgumentException` | Liste des tickets pour affichage ou génération des images QR. |
| `validerTicket(codeUnique)` | `String codeUnique` | `Ticket` | `IllegalArgumentException`, `IllegalStateException` | Valide l'entrée. Passe le statut de `VALIDE` à `UTILISE`, enregistre `dateUtilisation`. Rejette formellement si déjà scanné ou annulé. |

> [!TIP]
> **Génération des QR Codes (ZXing)** :
> Le Développeur 3 peut générer le QR code directement à partir de la chaîne `ticket.getCodeUnique()`.
> Lors du scan par l'agent à l'entrée, l'interface appelle `ticketService.validerTicket(codeScanne)`.

---

## 4. Configuration Déploiement : MySQL 8 & WildFly 41

### 4.1. Base de données MySQL
Exécuter le script `schema.sql` situé à la racine du projet :
```sql
mysql -u root -p < schema.sql
```

### 4.2. Configuration du Datasource dans WildFly (`standalone.xml`)
Dans le sous-système `<subsystem xmlns="urn:jboss:domain:datasources:7.2">` de WildFly :
```xml
<datasource jndi-name="java:jboss/datasources/EventBookingDS" pool-name="EventBookingDS" enabled="true" use-java-context="true">
    <connection-url>jdbc:mysql://localhost:3306/event_booking?useSSL=false&amp;serverTimezone=UTC&amp;allowPublicKeyRetrieval=true</connection-url>
    <driver>mysql</driver>
    <security>
        <user-name>root</user-name>
        <password>VOTRE_MOT_DE_PASSE</password>
    </security>
    <validation>
        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker"/>
        <validate-on-match>true</validate-on-match>
        <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter"/>
    </validation>
</datasource>

<drivers>
    <driver name="mysql" module="com.mysql">
        <xa-datasource-class>com.mysql.cj.jdbc.MysqlXADataSource</xa-datasource-class>
    </driver>
</drivers>
```

---

## 5. Tests et Validation

Pour exécuter la suite de tests automatisés (17 tests incluant concurrence multi-threads et tests JPA) :
```bash
mvn test
```

Pour générer l'archive WAR déployable sur WildFly :
```bash
mvn clean package
```
L'archive WAR se trouvera dans `target/event-booking-platform.war`.
