# CLAUDE.md – ubax-platform

## Vue d'ensemble

**ubax-platform** est le backend de la plateforme UBAX (Afrique) : marketplace immobilière/hôtelière permettant à des agences et hôtels partenaires de publier des annonces, gérer des locataires, des contrats, des paiements et leur équipe interne.

- **Stack** : Spring Boot 3.4.5 · Java 21 · PostgreSQL · Keycloak · MinIO
- **Port** : `9999` (local) · Context path : `/api`
- **Realm Keycloak** : `ubax-plateform`
- **Swagger UI** : `http://localhost:9999/api/swagger-ui.html`

---

## Architecture des modules

```
src/main/java/com/africa/ubaxplatform/
├── auth/           ✅ Auth, OTP, reset password, admins, sous-rôles
│   UserRole·UbaxAdminRole·AgenceRole·HotelRole · AuthController·AdminController·AgencyTeamController·UserSubRoleController · User·Agency·Hotel·OtpVerification·UserSubRole
├── partner/        ✅ Candidatures partenaires — PartnerController · PartnerApplication·ApplicationStatusLog
├── tenant/         ✅ Dossiers locataires (KYC) — TenantController · Tenant
├── property/       ✅ Biens, médias, documents, modération, boost
│   PropertyController · Property·PropertyMedia·PropertyDocument · PropertySchedulerJob
├── payment/        ✅ Paiements + scheduler loyers — PaymentController·ExpenseController · Payment·Expense
│   PaymentSchedulerJob (markOverduePayments@01h00, generateUpcomingRentPayments@06h00)
├── dashboard/      ✅ Analytics & KPIs par rôle — DashboardController
├── storage/        ✅ MinIO upload + URLs présignées — StorageController
├── bailleur/       ✅ Demandes bailleur, liens bailleur↔agence, création compte OWNER
│   BailleurApplicationStatus(PENDING/APPROVED/REJECTED/CANCELLED) · BailleurController · BailleurApplication·BailleurAgencyLink
├── reservation/    ✅ Réservations hôtelières — ReservationController · Reservation
│   ReservationStatus(PENDING→CONFIRMED→COMPLETED|NO_SHOW, PENDING|CONFIRMED→CANCELLED)
├── visitappointment/ ✅ Réservation de visites immobilières
│   VisitRequestStatus(PENDING/CONFIRMED/REJECTED/CANCELLED/COMPLETED) · PropertyVisitClientController·PropertyVisitAgencyController · PropertyVisitRequest·AgencyVisitAvailability
├── ticketing/      ✅ Tickets SAV — TicketController · Ticket(technicienId, interventionPrice)·TicketAttachment·TicketMessage
├── technicien/     ✅ Prestataires externes SAV (pas de compte plateforme) — TechnicienController · Technicien(agency XOR hotel, soft delete)
├── contract/       ✅ Baux et contrats — ContractController · Contract
│   ContractStatus(DRAFT→PENDING_SIGNATURE→ACTIVE→TERMINATED|CANCELLED) · activate()→1er loyer · submit()→PDF
├── document/       ✅ Génération PDF Thymeleaf → MinIO (pas d'API REST)
│   ContractGenerator·InvoiceGenerator·ReceiptGenerator · onPaymentPaid()→PDF→email @Async
├── notification/   ✅ Email (JavaMail) + SMS (LAfricaMobile) — EmailService·SmsService
├── common/         BaseEntity(UUID·createdAt·updatedAt) · CustomException·CustomResponse · RoleGuard·RequestHeaderParser·KeycloakJwtRolesConverter
└── config/         SecurityConfig · OpenApiConfig
```

---

## Sécurité & Rôles

### Deux filter chains Spring Security

| Ordre | Périmètre | Comportement |
|-------|-----------|--------------|
| 1 | Routes publiques (`WHITELIST`) | Aucun JWT requis |
| 2 | Toutes les autres routes | JWT Keycloak obligatoire |

Routes publiques : `/api-docs/**`, `/swagger-ui/**`, `/v1/auth/**` (sauf reset-password), `/v1/partner/apply`, `GET /v1/properties/**`, `GET /v1/code-list/type/**`.

### Rôles Keycloak (`UserRole` enum) — Niveau 1

| UserRole | Realm Keycloak | Périmètre |
|----------|----------------|-----------|
| `SUPER_ADMIN` | `UBAX_SUPER_ADMIN` | Accès total, gestion admins, config |
| `ADMIN` | `UBAX_ADMIN` | Administration courante back-office |
| `PARTNER` | `UBAX_PARTNER` | Partenaire agence **ou** hôtel |
| `OWNER` | `UBAX_OWNER` | Propriétaire individuel |
| `CLIENT` | `UBAX_CLIENT` | Locataire / acheteur |

### Sous-rôles applicatifs — Niveau 2 (table `user_sub_roles`, hors JWT)

Les sous-rôles affinent les accès à l'intérieur d'un rôle Keycloak. Persistés dans `user_sub_roles` avec un `scope`.

**Scope `UBAX_INTERNAL` — pour `ADMIN` / `SUPER_ADMIN` (`UbaxAdminRole`) :**

| Sous-rôle | Périmètre |
|-----------|-----------|
| `DIRECTEUR_GENERAL` | Vision globale, accès complet back-office |
| `SUPPORT_CLIENT` | Tickets, réclamations partenaires et clients |
| `OPERATIONS` | Onboarding partenaires, modération des annonces |
| `FINANCE` | Abonnements, commissions, revenus, rapports financiers |
| `COMMERCIAL` | Acquisition partenaires, suivi commercial |

**Scope `AGENCE` — pour `PARTNER` agence (`AgenceRole`) :**

| Sous-rôle | Tableau de bord | Périmètre |
|-----------|-----------------|-----------|
| `DIRECTEUR_AGENCE` | DG | Accès complet + gestion équipe |
| `COMMERCIAL` | Commercial | Prospects, rendez-vous, biens |
| `COMPTABLE_AGENCE` | Comptable | Finances (solde masqué) |
| `AGENT_SAV` | SAV | Tickets et interventions |
| `AGENT_IMMOBILIER` | — | Gestion des biens, visites et dossiers locataires/acheteurs |

**Scope `HOTEL` — pour `PARTNER` hôtel (`HotelRole`) :**

| Sous-rôle | Périmètre |
|-----------|-----------|
| `GERANT_HOTEL` | Accès complet hôtel |
| `RECEPTIONNISTE` | Réservations, check-in/out |
| `COMPTABLE_HOTEL` | Facturation et revenus |
| `RESPONSABLE_HEBERGEMENT` | Espaces et chambres |

### Vérification des rôles dans les contrôleurs

```java
// Niveau 1 — Rôle Keycloak (JWT) — toujours en premier
RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);

// Niveau 2 — Sous-rôle applicatif (DB) — charger l'entité User d'abord
User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
RoleGuard.checkAgenceRole(dbUser, subRoleRepo, AgenceRole.DIRECTEUR_AGENCE);
RoleGuard.checkHotelRole(dbUser, subRoleRepo, HotelRole.GERANT_HOTEL);
RoleGuard.checkAdminSubRole(dbUser, subRoleRepo, UbaxAdminRole.FINANCE);
```

`KeycloakJwtRolesConverter` extrait les rôles realm (`UBAX_*`) du JWT et les expose comme `GrantedAuthority`.

---

## Conventions de code

- **Formatage** : Google Java Format via `fmt-maven-plugin` — appliqué automatiquement à chaque build (`./mvnw compile`)
- **Lombok** : `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder` sur les entités ; `@RequiredArgsConstructor` sur les services et controllers
- **Entités** : toutes étendent `BaseEntity` (id UUID auto-généré, createdAt, updatedAt) via `@SuperBuilder`
- **Soft delete** : champ `deletedAt` (`LocalDateTime`, null = actif) — jamais de suppression physique
- **Réponses API** : toujours `CustomResponse(status, statusCode, message, data)` — le champ `data` accepte un objet ou une `Page<?>` (auto-convertie avec métadonnées de pagination)
- **Exceptions** : hiérarchie `CustomException` capturée par `ApiExceptionHandler` → réponse JSON normalisée
- **Mappers** : classes statiques `*Mapper` avec méthode `toResponse(Entity)` — pas de MapStruct
- **Services** : interface dans `service/interfaces/` + implémentation dans `service/impl/` ; `@Transactional` sur les méthodes d'écriture, `@Transactional(readOnly = true)` sur les lectures
- **Messages de réponse** : constantes dans `ResponseMessageConstants` — convention `DOMAINE_OPERATION_SUCCES|FAILURE`
- **Enums vs String** : utiliser des enums pour les états de cycle de vie (TicketStatus, PaymentStatus, etc.) — sécurité compile-time. Utiliser String pour les listes configurables UI (catégorie, priorité) — seedées dans `la_code_list`

### Pattern contrôleur type

```java
@RestController
@RequestMapping("/v1/module")
@RequiredArgsConstructor
@Tag(name = "...", description = "...")
public class ModuleController {

  private final ModuleService moduleService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(summary = "...", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({ @ApiResponse(responseCode = "201", ...), ... })
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateRequest request,
      HttpServletRequest httpRequest) throws CustomException {

    RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    ModuleResponse response = moduleService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.CREATED,
            ResponseMessageConstants.MODULE_CREATE_SUCCESS,
            response));
  }
}
```

---

## Base de données

- **Schéma** : `administrative`
- **Migrations** : Flyway (`src/main/resources/db/migration/V*__*.sql`)
- `ddl-auto: none` — Flyway est **la seule** source de vérité pour le schéma
- Toute nouvelle table ou colonne = nouvelle migration versionnée (ne jamais modifier une migration existante)

### Toutes les migrations

| Version | Fichier | Tables / colonnes |
|---------|---------|-------------------|
| V001–V044 | *(voir `src/main/resources/db/migration/`)* | Fondations : schéma, users, OTP, partners, code_list, agencies, tenants, contracts, properties, payments, expenses, sub_roles, hotels, tickets, bailleurs, amenities, reservations, documents |
| V045 | `create_techniciens.sql` | Table `techniciens` (agency_id XOR hotel_id, available, soft delete, CHECK constraint structure) |
| V046 | `add_technicien_to_tickets.sql` | Colonnes `technicien_id UUID` (FK → `techniciens.id`) + `intervention_price DECIMAL(15,2)` sur `tickets` |
| V047 | `seed_technicien_profession_code_lists.sql` | Seed `la_code_list` TECHNICIEN_PROFESSION (12 valeurs : PLOMBIER, ELECTRICIEN … AUTRE) |
| V048 | `update_ticket_category_code_lists.sql` | Mise à jour code lists catégories tickets |
| V049 | `create_property_favorites.sql` | Table `property_favorites` (user_id FK, property_id FK, contrainte unique) |
| V050 | `add_property_id_to_tenants.sql` | Colonne `property_id UUID` (nullable, FK → `properties.id`) sur `tenants` — bien ciblé à la soumission du dossier |
| V051 | `add_rent_to_own_contract_type.sql` | Seed `la_code_list` (CONTRACT_TYPE : RENT_TO_OWN = Location-vente) + colonne `monthly_installment NUMERIC(15,2)` sur `contracts` |
| V052 | `drop_transaction_type_check_constraint.sql` | Suppression contrainte CHECK statique `properties_transaction_type_check` — bloquait SHORT_STAY et autres types hôteliers ajoutés en V034 |
| V053 | `add_description_to_property_amenities.sql` | Colonne `description VARCHAR(500)` sur `property_amenities` — description libre pour chaque commodité d'un bien |
| V054 | `simplify_bailleur_application.sql` | `description TEXT` sur `bailleur_applications`, supprime `conflict_*`, supprime table `bailleur_application_properties` |
| V055 | `add_id_docs_and_nullable_email_to_bailleur.sql` | `email` nullable sur `bailleur_applications` (clients sans email) + colonnes `id_doc_recto_url TEXT` et `id_doc_verso_url TEXT` (pièce d'identité recto/verso) |
| V056 | `create_management_contracts.sql` | Table `management_contracts` (mandats agence↔bailleur, mêmes statuts que contracts, FKs agency/owner/created_by/terminated_by) |
| V057 | `fix_tenant_employment_status_constraint.sql` | Corrige contrainte orpheline `tenants_employment_status_check`, ajoute `EMPLOYED` en code_list, migre `EMPLOYEE` → `EMPLOYED` |
| V058 | `create_property_visit_requests_and_availabilities.sql` | Tables `property_visit_requests`, `agency_visit_availabilities`, `visit_slot_occupancy` — module réservation de visites |
| V059 | `seed_visit_request_code_lists.sql` | Seed `la_code_list` (VISIT_REQUEST_STATUS : PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED) |
| V060 | `make_agency_description_not_null.sql` | `NOT NULL` sur `agencies.description` + backfill |
| V061 | `add_unit_count_to_properties.sql` | Colonne `unit_count INTEGER NOT NULL DEFAULT 1` sur `properties` + contrainte `CHECK (unit_count >= 1)` — pool de chambres identiques pour les biens hôteliers |
| V062 | `backfill_hotel_id_on_properties.sql` | Backfill `hotel_id` sur `properties` depuis `users.hotel_id` (données pré-V044) |

Prochaine version disponible : **V063**

---

## Endpoints opérationnels — référence rapide

### Auth & Admin

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/auth/login` | Public | Login email |
| `POST` | `/v1/auth/login/phone` | Public | Login téléphone |
| `POST` | `/v1/auth/register/send-otp` | Public | OTP inscription |
| `POST` | `/v1/auth/register/verify-otp` | Public | Vérifier OTP |
| `POST` | `/v1/auth/register/complete` | Public | Finaliser inscription |
| `POST` | `/v1/auth/logout` | Public | Logout |
| `POST` | `/v1/auth/forgot-password` | Public | Email reset |
| `POST` | `/v1/auth/forgot-password/send-otp` | Public | OTP reset SMS |
| `POST` | `/v1/auth/forgot-password/verify-otp` | Public | Vérifier OTP reset |
| `POST` | `/v1/auth/forgot-password/reset` | Public | Reset via OTP |
| `POST` | `/v1/auth/reset-password` | `ADMIN` | Reset forcé |
| `GET` | `/v1/auth/roles` | `ADMIN` | Liste rôles Keycloak |
| `POST` | `/v1/auth/users/{keycloakId}/roles` | `ADMIN` | Assigner rôle |
| `DELETE` | `/v1/auth/users/{keycloakId}/roles` | `ADMIN` | Retirer rôle |
| `POST` | `/v1/admin/users` | `SUPER_ADMIN` | Créer admin |
| `GET` | `/v1/admin/users` | `ADMIN` | Lister admins |
| `PUT` | `/v1/admin/users/{userId}/role` | `SUPER_ADMIN` | Changer rôle admin |
| `DELETE` | `/v1/admin/users/{userId}` | `SUPER_ADMIN` | Supprimer admin |
| `POST` | `/v1/users/me/avatar` | Authentifié | Mettre à jour avatar |
| `GET` | `/v1/admin/clients` | `ADMIN` | Liste tous les clients (`?agencyId=` ou `?hotelId=` optionnel) |

> **Notes clés — Auth mobile** : `phone` format `^\+[1-9]\d{7,14}$` (préfixe international obligatoire). `RegisterCompleteRequest` : `title` obligatoire (`M.`/`Mme`), `email` **optionnel** (`@Email` sans `@NotBlank`), `password` min 8 chars. `LoginResponse` contient `access_token` + `refresh_token` + `expires_in` (300s). `POST /v1/auth/forgot-password` retourne toujours 200 OK (anti-énumération). `forgot-password/verify-otp` : OTP vérifié **NON consommé** — appeler `/reset` ensuite.

### Sous-rôles (User Sub-Roles)

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/admin/users/{userId}/sub-roles` | `SUPER_ADMIN` | Assigner des sous-rôles |
| `GET` | `/v1/admin/users/{userId}/sub-roles` | `ADMIN` | Consulter les sous-rôles |
| `DELETE` | `/v1/admin/users/{userId}/sub-roles/{role}` | `SUPER_ADMIN` | Révoquer un sous-rôle |

### Agencies & Hotels (liste authentifiée 📱)

> Ces endpoints **requièrent un JWT valide** — ils ne sont pas publics.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/agencies` | `CLIENT/OWNER/PARTNER/ADMIN` | Liste paginée des agences actives (filtre optionnel `?city=`, tri `name` ASC) — étape 1 du flux bailleur mobile |
| `GET` | `/v1/hotels` | `CLIENT/OWNER/PARTNER/ADMIN` | Liste paginée des hôtels actifs (filtre optionnel `?city=`) |

### Agency Team

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/agency/team` | `PARTNER` | Lister les membres actifs |
| `POST` | `/v1/agency/team` | `PARTNER` | Inviter un nouveau membre (crée compte PARTNER + envoie email) |
| `GET` | `/v1/agency/team/inactive` | `PARTNER` | Lister les membres inactifs (soft-deleted) |
| `PATCH` | `/v1/agency/team/{userId}/activate` | `PARTNER` + `DIRECTEUR_AGENCE` | Réactiver un membre retiré |
| `DELETE` | `/v1/agency/team/{userId}` | `PARTNER` + `DIRECTEUR_AGENCE` | Retirer un membre (soft delete) |
| `POST` | `/v1/agency/team/{userId}/sub-roles` | `PARTNER` | Assigner des sous-rôles AGENCE (additif) |
| `GET` | `/v1/agency/team/{userId}/sub-roles` | `PARTNER` | Consulter les sous-rôles d'un membre |
| `DELETE` | `/v1/agency/team/{userId}/sub-roles/{role}` | `PARTNER` | Révoquer un sous-rôle AGENCE |
| `GET` | `/v1/agency/clients` | `PARTNER` | Clients de mon agence (via contrats) |

### Hotel Team

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/hotel/clients` | `PARTNER` | Clients de mon hôtel (via réservations) |
| `GET` | `/v1/hotel/team` | `PARTNER` | Lister membres actifs |
| `POST` | `/v1/hotel/team` | `PARTNER` | Inviter un nouveau membre |
| `POST` | `/v1/hotel/team/{userId}/sub-roles` | `PARTNER` | Assigner des sous-rôles HOTEL |
| `GET` | `/v1/hotel/team/{userId}/sub-roles` | `PARTNER` | Consulter les sous-rôles d'un membre |
| `GET` | `/v1/hotel/team/inactive` | `PARTNER` | Lister les membres inactifs |
| `PATCH` | `/v1/hotel/team/{userId}/activate` | `PARTNER` | Réactiver un membre |
| `DELETE` | `/v1/hotel/team/{userId}` | `PARTNER` | Retirer un membre (soft delete) |
| `DELETE` | `/v1/hotel/team/{userId}/sub-roles/{role}` | `PARTNER` | Révoquer un sous-rôle |

### Partner

> **`POST /v1/partner/apply`** : le champ `description` est **obligatoire** (`@NotBlank`, 10–1 000 caractères). Il est persisté sur l'agence/hôtel créé lors de l'approbation et retourné dans `GET /v1/agencies`.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/partner/apply` | Public | Soumettre candidature (`description` obligatoire 10–1 000 chars) |
| `GET` | `/v1/partner/admin/applications` | `ADMIN` | Liste paginée |
| `GET` | `/v1/partner/admin/applications/{id}` | `ADMIN` | Détail |
| `PATCH` | `/v1/partner/admin/applications/{id}/decision` | `ADMIN` | Décision |

### Tenant

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/tenants/profile` | `CLIENT` | Soumettre dossier |
| `GET` | `/v1/tenants/profile` | Authentifié | Voir mon dossier |
| `PATCH` | `/v1/tenants/profile` | Authentifié | Mettre à jour |
| `GET` | `/v1/tenants` | `ADMIN/PARTNER` | Liste paginée |
| `GET` | `/v1/tenants/{id}` | `ADMIN/PARTNER` | Détail |
| `PATCH` | `/v1/tenants/{id}/qualify` | `ADMIN/PARTNER` | Qualifier |
| `PATCH` | `/v1/tenants/{id}/reject` | `ADMIN/PARTNER` | Rejeter |
| `DELETE` | `/v1/tenants/{id}` | `ADMIN` | Archiver |

### Property

> **Sécurité** : `GET /v1/properties` et `GET /v1/properties/**` sont publics (pas de JWT requis). Toutes les autres méthodes (POST, PUT, PATCH, DELETE) exigent un JWT Keycloak valide.
> **`coverPhotoUrl`** : champ présent dans `PropertyResponse` — URL de la photo de couverture (null si aucun média uploadé).
> **`unitCount`** : nombre d'unités disponibles pour un bien hôtelier (défaut : `1`). Permet un pool de chambres identiques (même config, même prix). Une réservation est acceptée si `overlappingConfirmed < unitCount`. Pour les biens immobiliers classiques, laisser à `1`.
> **Disponibilité** : `GET /v1/properties/{id}/availability?checkIn=&checkOut=` retourne `{ propertyId, unitCount, confirmedOverlaps, availableUnits, checkIn, checkOut }`. Endpoint **public**, à appeler avant `POST /v1/reservations`. Seules les réservations `CONFIRMED` bloquent les unités.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/properties` | Public | Liste paginée + filtres (inclut `coverPhotoUrl`) |
| `GET` | `/v1/properties/{id}` | Public | Détail (inclut `coverPhotoUrl`) |
| `GET` | `/v1/properties/{id}/availability` | Public | Disponibilité pour `?checkIn=&checkOut=` (format `YYYY-MM-DD`) |
| `GET` | `/v1/properties/mine` | `PARTNER/OWNER` | Mes biens (inclut `coverPhotoUrl`) |
| `POST` | `/v1/properties` | `PARTNER/OWNER` | Créer brouillon |
| `PUT` | `/v1/properties/{id}` | `PARTNER/OWNER` | Mettre à jour |
| `PATCH` | `/v1/properties/{id}/submit` | `PARTNER/OWNER` | Soumettre en modération |
| `DELETE` | `/v1/properties/{id}` | `PARTNER/OWNER` | Archiver |
| `PATCH` | `/v1/properties/{id}/status` | `ADMIN` | Modérer (approuver/rejeter) |
| `POST` | `/v1/properties/{id}/media/upload` | `PARTNER/OWNER` | Upload direct média |
| `POST` | `/v1/properties/{id}/media` | `PARTNER/OWNER` | Lier média pré-uploadé |
| `GET` | `/v1/properties/{id}/media` | Public | Liste médias |
| `PATCH` | `/v1/properties/{id}/media/{mediaId}/cover` | `PARTNER/OWNER` | Photo couverture |
| `DELETE` | `/v1/properties/{id}/media/{mediaId}` | `PARTNER/OWNER` | Supprimer média |
| `POST` | `/v1/properties/{id}/documents` | `PARTNER/OWNER` | Ajouter document |
| `GET` | `/v1/properties/{id}/documents` | Authentifié | Liste documents |
| `PATCH` | `/v1/properties/{id}/documents/{docId}/verify` | `ADMIN` | Vérifier document |
| `DELETE` | `/v1/properties/{id}/documents/{docId}` | `PARTNER/OWNER` | Supprimer document |
| `PATCH` | `/v1/properties/{id}/boost` | `ADMIN` | Activer boost |
| `DELETE` | `/v1/properties/{id}/boost` | `ADMIN` | Retirer boost |
| `PATCH` | `/v1/properties/{id}/expiration` | `ADMIN` | Fixer expiration |
| `DELETE` | `/v1/properties/{id}/expiration` | `ADMIN` | Retirer expiration |

### Property Visits - Client

> **Statuts :** `PENDING → CONFIRMED / REJECTED` · `PENDING → CANCELLED` (par le client) · `CONFIRMED → COMPLETED`  
> **`DELETE /v1/property-visits/{visitRequestId}`** : retourne `204 No Content` — seules les demandes `PENDING` sont annulables (400 sinon). Libère automatiquement le créneau.  
> **`GET /v1/property-visits/{visitRequestId}`** : CLIENT/OWNER = propriétaire uniquement (403 sinon).  
> **`GET /v1/property-visits/available-slots/{propertyId}`** : accès public (no JWT). Param optionnel `?daysAhead=30`.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/property-visits/available-slots/{propertyId}` | Public | Créneaux disponibles pour les 30 prochains jours (`?daysAhead=N`) |
| `POST` | `/v1/property-visits` | `CLIENT/OWNER` | Créer une demande de visite |
| `GET` | `/v1/property-visits/mine` | `CLIENT/OWNER` | Mes demandes (paginé, tri `createdAt` DESC) |
| `GET` | `/v1/property-visits/{visitRequestId}` | `CLIENT/OWNER` | Détail — 403 si pas demandeur |
| `DELETE` | `/v1/property-visits/{visitRequestId}` | `CLIENT/OWNER` | Annuler `PENDING` → `CANCELLED` · 204 No Content |


### Property Visits - Agency

> **Configuration des créneaux** : définit les jours et horaires de visite par bien. `timeSlots` : map `{dayOfWeek → [slots]}` (0=Dimanche…6=Samedi). `maxVisitsPerSlot` (défaut 3) = capacité max par créneau. `POST /config` est idempotent (remplace la config existante).  
> **Dates fermées** : `PUT /config/{propertyId}/blackout-dates` remplace intégralement les blackout dates (envoyer `[]` pour tout supprimer).  
> **Confirmation** : l’agence peut confirmer une date/créneau différents de ceux demandés (proposition alternative).  
> **`assign-agent`** : `agentId` = UUID **base de données** (`user.id`, pas Keycloak ID) — utiliser `GET /v1/agency/team`.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/agency/property-visits/config` | `PARTNER` | Configurer les créneaux de visite d’un bien (idempotent) |
| `GET` | `/v1/agency/property-visits/config/{propertyId}` | `PARTNER` | Récupérer la configuration de disponibilité |
| `PUT` | `/v1/agency/property-visits/config/{propertyId}/blackout-dates` | `PARTNER` | Remplacer les dates d’indisponibilité |
| `GET` | `/v1/agency/property-visits` | `PARTNER` | Demandes reçues (paginé, tri `createdAt` ASC) |
| `PATCH` | `/v1/agency/property-visits/{visitRequestId}/confirm` | `PARTNER` | `PENDING → CONFIRMED` · body `confirmedDate` + `confirmedTimeSlot` requis |
| `PATCH` | `/v1/agency/property-visits/{visitRequestId}/reject` | `PARTNER` | `PENDING → REJECTED` · body `reason` requis |
| `PATCH` | `/v1/agency/property-visits/{visitRequestId}/assign-agent/{agentId}` | `PARTNER` | Assigner un agent (ne change pas le statut) |


### Payment & Expense

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/payments` | `PARTNER/ADMIN` | Liste paginée + filtres |
| `GET` | `/v1/payments/late` | `PARTNER/ADMIN` | Loyers en retard |
| `GET` | `/v1/payments/{id}` | `PARTNER/ADMIN` | Détail |
| `POST` | `/v1/payments` | `PARTNER/ADMIN` | Enregistrer paiement |
| `PATCH` | `/v1/payments/{id}/status` | `PARTNER/ADMIN` | Mettre à jour statut |
| `DELETE` | `/v1/payments/{id}` | `PARTNER/ADMIN` | Supprimer |
| `GET` | `/v1/expenses` | `PARTNER/ADMIN` | Liste paginée + filtres |
| `GET` | `/v1/expenses/{id}` | `PARTNER/ADMIN` | Détail |
| `POST` | `/v1/expenses` | `PARTNER/ADMIN` | Enregistrer dépense |
| `DELETE` | `/v1/expenses/{id}` | `PARTNER/ADMIN` | Supprimer |

### Contract

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/contracts` | `PARTNER/OWNER/ADMIN` | Créer contrat (DRAFT) |
| `GET` | `/v1/contracts` | `PARTNER/OWNER/ADMIN` | Liste paginée (filtrable par statut) |
| `GET` | `/v1/contracts/{id}` | `PARTNER/OWNER/ADMIN` | Détail |
| `PUT` | `/v1/contracts/{id}` | `PARTNER/OWNER/ADMIN` | Modifier (DRAFT uniquement) |
| `PATCH` | `/v1/contracts/{id}/submit` | `PARTNER/OWNER/ADMIN` | Soumettre → PENDING_SIGNATURE + PDF |
| `PATCH` | `/v1/contracts/{id}/activate` | `ADMIN` | Activer → ACTIVE + 1er loyer LEASE |
| `PATCH` | `/v1/contracts/{id}/terminate` | `PARTNER/OWNER/ADMIN` | Résilier → TERMINATED |
| `PATCH` | `/v1/contracts/{id}/cancel` | `PARTNER/OWNER/ADMIN` | Annuler → CANCELLED |

### Ticketing

> `attachments[]` optionnel dans `POST /v1/tickets` (URLs presign/ticket-attachment, type INCIDENT_PHOTO par défaut). CLIENT : création + `/mine` + détail de ses propres tickets uniquement (403 sinon).

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/tickets` | `PARTNER/CLIENT/OWNER` | Créer un ticket (+ `attachments[]` optionnel) |
| `GET` | `/v1/tickets` | `PARTNER/ADMIN` | Liste paginée + filtres (agence du caller) |
| `GET` | `/v1/tickets/mine` | `CLIENT/OWNER/PARTNER` | Mes tickets déclarés (paginé, tri createdAt DESC) |
| `GET` | `/v1/tickets/{id}` | `PARTNER/ADMIN` ou `CLIENT/OWNER` (reporter uniquement) | Détail — CLIENT/OWNER : 403 si pas reporter |
| `PATCH` | `/v1/tickets/{id}/status` | `PARTNER/ADMIN` | Mettre à jour le statut |
| `PATCH` | `/v1/tickets/{id}/assign` | `PARTNER/ADMIN` | Assigner à un agent |
| `PATCH` | `/v1/tickets/{id}/schedule` | `PARTNER/ADMIN` | Planifier intervention (technicienId ou nom/tel libre + interventionPrice) |
| `PATCH` | `/v1/tickets/{id}/repair-cost` | `PARTNER/ADMIN` | Renseigner le coût de réparation |
| `POST` | `/v1/tickets/{id}/messages` | Authentifié | Ajouter un message |
| `GET` | `/v1/tickets/{id}/messages` | Authentifié | Lister les messages |
| `GET` | `/v1/tickets/{id}/attachments` | Authentifié | Lister les pièces jointes |

### Favoris

> 📱 **CLIENT / OWNER** — Biens immobiliers mis en favori depuis l'app mobile. Retourne des `PropertyResponse` complets, triés par date d'ajout décroissante. L'ajout est **idempotent** (pas de 409 si déjà favori).

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/favorites/{propertyId}` | `CLIENT/OWNER` | Ajouter un bien aux favoris (idempotent) |
| `DELETE` | `/v1/favorites/{propertyId}` | `CLIENT/OWNER` | Retirer un bien des favoris |
| `GET` | `/v1/favorites/mine` | `CLIENT/OWNER` | Liste paginée des biens favoris |

### Techniciens

> Prestataires externes gérés par une agence **ou** un hôtel. N'ont pas de compte sur la plateforme. Assignables aux tickets SAV via `technicienId`.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/technicians` | `PARTNER` | Créer un technicien |
| `GET` | `/v1/technicians` | `PARTNER` | Liste paginée (filtre `?available=`) |
| `GET` | `/v1/technicians/{id}` | `PARTNER` | Détail |
| `PUT` | `/v1/technicians/{id}` | `PARTNER` | Mettre à jour |
| `PATCH` | `/v1/technicians/{id}/availability` | `PARTNER` | Basculer disponibilité |
| `DELETE` | `/v1/technicians/{id}` | `PARTNER` | Supprimer (soft delete) |

### Mandat de gestion (Agence ↔ Bailleur)

> Exclusif agence↔bailleur (jamais visible clients). Requiert lien `bailleur_agency_links` approuvé. Un seul ACTIVE/PENDING_SIGNATURE par couple. Référence auto : `MAN-{YEAR}-{UUID6}`. Type `MANDATE` bloqué dans `POST /v1/contracts`.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/mandates` | `PARTNER` | Créer mandat (DRAFT) — `ownerId` + `startDate` obligatoires |
| `GET` | `/v1/mandates` | `PARTNER` | Liste paginée de l'agence (filtre `?status=`) |
| `GET` | `/v1/mandates/{id}` | `PARTNER` | Détail |
| `PATCH` | `/v1/mandates/{id}/submit` | `PARTNER` | DRAFT → PENDING_SIGNATURE |
| `PATCH` | `/v1/mandates/{id}/activate` | `ADMIN` | PENDING_SIGNATURE → ACTIVE |
| `PATCH` | `/v1/mandates/{id}/terminate` | `PARTNER` | ACTIVE → TERMINATED (body `terminationReason` obligatoire) |
| `PATCH` | `/v1/mandates/{id}/cancel` | `PARTNER` | DRAFT/PENDING_SIGNATURE → CANCELLED |

### Bailleur

> Flux : CLIENT choisit agence → `POST /v1/bailleur/apply` (JWT CLIENT, champs auto-extraits du compte, obligatoires : `agencyId`/`idType`/`idNumber`/`description` min 10 mots, optionnels : `idDocRectoUrl`/`idDocVersoUrl`) → agence approuve → rôle OWNER ajouté. `POST /v1/properties` avec `ownerId` tiers : vérifie lien `bailleur_agency_links` (400 sinon) — utiliser `GET /v1/bailleur/agency/bailleurs`.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/bailleur/apply` | `CLIENT` 📱 | Soumettre une demande d'adhésion bailleur |
| `GET` | `/v1/bailleur/my-applications` | `OWNER` 📱 | Mes demandes d'adhésion (filtrable par statut) |
| `GET` | `/v1/bailleur/my-agencies` | `OWNER` 📱 | Agences auxquelles je suis lié |
| `GET` | `/v1/bailleur/agency/applications` | `PARTNER` + `DIRECTEUR_AGENCE` | Liste paginée des demandes reçues |
| `GET` | `/v1/bailleur/agency/applications/{id}` | `PARTNER` + `DIRECTEUR_AGENCE` | Détail d'une demande |
| `PATCH` | `/v1/bailleur/agency/applications/{id}/decision` | `PARTNER` + `DIRECTEUR_AGENCE` | Approuver / rejeter |
| `GET` | `/v1/bailleur/agency/bailleurs` | `PARTNER` + `DIRECTEUR_AGENCE` | Bailleurs approuvés liés à l'agence (retourne `id` à utiliser comme `ownerId` dans `POST /v1/properties`) |
| `GET` | `/v1/bailleur/admin/applications` | `ADMIN` · `SUPER_ADMIN` | Vue globale toutes agences |

### Reservation

> Statuts : `PENDING→CONFIRMED→COMPLETED|NO_SHOW` · `PENDING|CONFIRMED→CANCELLED`. `DELETE` et `cancel` : body `{"reason":"..."}` obligatoire. `DELETE` réservé au client sur `PENDING` uniquement. `GET /{id}` : CLIENT = propriétaire (403 sinon), PARTNER = hôtel uniquement.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/reservations` | `CLIENT` | Soumettre une réservation (`checkInDate`/`checkOutDate` futures, `guestCount` ≥ 1) |
| `GET` | `/v1/reservations/mine` | `CLIENT` | Mes réservations (paginé, tri `createdAt` DESC) |
| `GET` | `/v1/reservations/{id}` | `CLIENT/PARTNER` | Détail (accès restreint au propriétaire ou à l'hôtelier concerné) |
| `DELETE` | `/v1/reservations/{id}` | `CLIENT` | Annuler — `PENDING` uniquement · body JSON `{ "reason": "..." }` requis |
| `GET` | `/v1/reservations/hotel` | `PARTNER` (hôtel) | Réservations de mon hôtel (filtre `?status=` optionnel) |
| `PATCH` | `/v1/reservations/{id}/confirm` | `PARTNER` (hôtel) | Confirmer (`PENDING → CONFIRMED`) |
| `PATCH` | `/v1/reservations/{id}/cancel` | `PARTNER` (hôtel) | Annuler (`PENDING|CONFIRMED → CANCELLED`) · body `{ "reason": "..." }` requis |
| `PATCH` | `/v1/reservations/{id}/complete` | `PARTNER` (hôtel) | Marquer séjour terminé (`CONFIRMED → COMPLETED`) |
| `PATCH` | `/v1/reservations/{id}/no-show` | `PARTNER` (hôtel) | Marquer no-show (`CONFIRMED → NO_SHOW`) |
| `GET` | `/v1/reservations` | `ADMIN/SUPER_ADMIN` | Toutes les réservations (filtre `?status=` optionnel) |

### Gestion admin des partenaires (back-office)

> Endpoints réservés aux rôles `ADMIN` et `SUPER_ADMIN`. Permettent de lister, suspendre, réactiver et gérer les abonnements des agences et hôtels partenaires. La suspension désactive `is_active` — les membres ne peuvent plus se connecter.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/admin/partners/agencies` | `ADMIN` | Liste paginée de toutes les agences |
| `PATCH` | `/v1/admin/partners/agencies/{id}/suspend` | `ADMIN` | Suspendre une agence |
| `PATCH` | `/v1/admin/partners/agencies/{id}/activate` | `ADMIN` | Réactiver une agence |
| `PATCH` | `/v1/admin/partners/agencies/{id}/subscription` | `ADMIN` | Mettre à jour l'abonnement (plan + date expiration) |
| `GET` | `/v1/admin/partners/hotels` | `ADMIN` | Liste paginée de tous les hôtels |
| `PATCH` | `/v1/admin/partners/hotels/{id}/suspend` | `ADMIN` | Suspendre un hôtel |
| `PATCH` | `/v1/admin/partners/hotels/{id}/activate` | `ADMIN` | Réactiver un hôtel |
| `PATCH` | `/v1/admin/partners/hotels/{id}/subscription` | `ADMIN` | Mettre à jour l'abonnement |
| `GET` | `/v1/admin/partners/clients` | `ADMIN` | Liste paginée des clients |
| `GET` | `/v1/admin/agencies/{agencyId}/members` | `ADMIN` | Membres actifs d'une agence (audit/support) |
| `GET` | `/v1/admin/agencies/{agencyId}/members/inactive` | `ADMIN` | Membres inactifs (soft-deleted) d'une agence |
| `GET` | `/v1/admin/hotels/{hotelId}/members` | `ADMIN` | Membres actifs d'un hôtel (audit/support) |
| `GET` | `/v1/admin/hotels/{hotelId}/members/inactive` | `ADMIN` | Membres inactifs (soft-deleted) d'un hôtel |

### Code List (référentiels)

> `GET /v1/code-list/type/{type}` est **public** (pas de JWT). `ROLE_UBAX_INTERNAL` est restreint : utiliser `GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL` (ADMIN requis).

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/code-list/type/{type}` | Public | Valeurs d'un type (selects frontend) |
| `GET` | `/v1/code-list/admin/type/{type}` | `ADMIN` | Valeurs d'un type — accès types restreints (`ROLE_UBAX_INTERNAL`) |
| `GET` | `/v1/code-list` | Authentifié | Liste paginée de tous les code lists |
| `GET` | `/v1/code-list/{id}` | Authentifié | Détail d'un code list |
| `POST` | `/v1/code-list` | `ADMIN` | Créer une valeur |
| `PUT` | `/v1/code-list/{id}` | `ADMIN` | Modifier une valeur |

**Types disponibles (endpoint public) :**

| Type | Valeurs |
|------|---------|
| `ROLE_AGENCE` | `DIRECTEUR_AGENCE` · `COMMERCIAL` · `COMPTABLE_AGENCE` · `AGENT_SAV` · `AGENT_IMMOBILIER` |
| `ROLE_HOTEL` | `GERANT_HOTEL` · `RECEPTIONNISTE` · `COMPTABLE_HOTEL` · `RESPONSABLE_HEBERGEMENT` |
| `ROLE_UBAX_INTERNAL` ⚠️ | `DIRECTEUR_GENERAL` · `SUPPORT_CLIENT` · `OPERATIONS` · `FINANCE` · `COMMERCIAL` — **ADMIN uniquement** |
| `ID_TYPE` | `CNI` · `PASSEPORT` · `PERMIS_CONDUIRE` · `TITRE_SEJOUR` · `CARTE_CONSULAIRE` |
| `PROPERTY_AMENITY` | `POOL` · `GENERATOR` · `WATER_TANK` · `AC` · `SECURITY` · `PARKING` · `ELEVATOR` · `GARDEN` · `FURNISHED` · `PETS` |
| `TECHNICIEN_PROFESSION` | `PLOMBIER` · `ELECTRICIEN` · `SERRURIER` · `MENUISIER` · `MACON` · `PEINTRE` · `CLIMATISATION` · `VITRERIE` · `JARDINAGE` · `NETTOYAGE` · `DESINSECTISATION` · `AUTRE` |
| `TICKET_PRIORITY` | `LOW` · `NORMAL` · `HIGH` · `URGENT` |
| `TICKET_ATTACHMENT_TYPE` | `INCIDENT_PHOTO` · `INCIDENT_VIDEO` · `INTERVENTION_REPORT` · `INVOICE` · `OTHER` |
| `CONTRACT_TYPE` | `LEASE` · `SALE` · `RENT_TO_OWN` |
| `EMPLOYMENT_STATUS` | `EMPLOYED` · `SELF_EMPLOYED` · `STUDENT` · `RETIRED` · `UNEMPLOYED` · `OTHER` |
| `PARTNER_TYPE` | `AGENCY` · `HOTEL` |

### Dashboard & Storage

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/dashboard/agency` | `PARTNER/ADMIN` | KPIs agence par sous-rôle (filtre `?from=&to=` optionnel) |
| `GET` | `/v1/dashboard/admin` | `ADMIN/SUPER_ADMIN` | KPIs globaux plateforme (agences actives, hôtels, clients, owners, réservations, biens en modération, tickets ouverts) |
| `POST` | `/v1/storage/upload` | Authentifié | Upload direct multipart |
| `POST` | `/v1/storage/upload/agency-logo` | Authentifié | Upload logo agence |
| `GET` | `/v1/storage/presign` | Authentifié | URL présignée générique |
| `GET` | `/v1/storage/presign/property-media` | Authentifié | URL présignée média bien |
| `GET` | `/v1/storage/presign/property-document` | Authentifié | URL présignée doc bien |
| `GET` | `/v1/storage/presign/tenant-document` | Authentifié | URL présignée KYC |
| `GET` | `/v1/storage/presign/agency-logo` | Authentifié | URL présignée logo |
| `GET` | `/v1/storage/presign/ticket-attachment` | Authentifié | URL présignée ticket |
| `GET` | `/v1/storage/presign/bailleur-document` | Authentifié | URL présignée pièce d'identité bailleur (recto ou verso) |

---

## Infrastructure locale (Docker)

```
docker/docker-compose.yml   # PostgreSQL, Keycloak, MinIO, pgAdmin, Prometheus, Grafana
```

```bash
docker compose -f docker/docker-compose.yml up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Variables d'environnement clés

| Variable | Défaut local | Description |
|----------|-------------|-------------|
| `KEYCLOAK_CLIENT_ID` | – | **Requis** – client Keycloak |
| `KEYCLOAK_CLIENT_SECRET` | – | **Requis** – secret client |
| `BOOTSTRAP_ENABLED` | `true` | Créer le super admin au 1er démarrage |
| `SECURITY_ENABLED` | `true` | Mettre à `false` uniquement en dev |
| `DB_HOST/PORT/NAME` | `localhost/5433/db-ubax` | PostgreSQL |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Object storage |

---

## Tests

| Couche | Emplacement | Outillage |
|--------|-------------|-----------|
| Unitaires | `src/test/java/.../unit/` | JUnit 5 + Mockito |
| Intégration | `src/test/java/.../integration/` | Testcontainers (PostgreSQL + Keycloak) |

```bash
./mvnw test                    # tous les tests
./mvnw test -pl . -Dtest=*IT   # intégration uniquement
```

---

## Buckets MinIO

`users-avatars`, `agencies-logos`, `properties-media`, `property-documents`,
`tenant-documents`, `documents-generated`, `ticket-attachments`, `partner-documents`,
`bailleur-documents`

---

## Observabilité

- Actuator : `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- Prometheus + Grafana inclus dans le docker-compose
