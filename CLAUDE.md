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
├── auth/           ✅ Auth, inscription OTP, reset password, admins, sous-rôles
│   ├── codeList/       UserRole, UbaxAdminRole, AgenceRole, HotelRole, RoleScope
│   ├── controller/     AuthController, AdminController, UserProfileController,
│   │                   AgencyTeamController, UserSubRoleController
│   ├── dto/
│   ├── entity/         User, Agency, Hotel, OtpVerification, UserSubRole
│   ├── mapper/         UserMapper, AgencyMapper
│   ├── repository/     UserRepository, AgencyRepository, HotelRepository,
│   │                   OtpVerificationRepository, UserSubRoleRepository
│   └── service/
├── partner/        ✅ Candidatures partenaires (agences / hôtels)
│   ├── codeList/       ApplicationStatus
│   ├── controller/     PartnerController
│   ├── entity/         PartnerApplication, ApplicationStatusLog
│   └── service/
├── tenant/         ✅ Dossiers locataires (KYC)
│   ├── codeList/       TenantStatus
│   ├── controller/     TenantController
│   ├── entity/         Tenant
│   └── service/
├── property/       ✅ Biens, médias, documents, modération, boost
│   ├── codeList/       PropertyStatus
│   ├── controller/     PropertyController
│   ├── entity/         Property, PropertyMedia, PropertyDocument
│   ├── scheduler/      PropertySchedulerJob
│   └── service/
├── payment/        ✅ Paiements, dépenses agence + scheduler loyers automatiques
│   ├── codeList/       PaymentStatus, PaymentType, PaymentMethod,
│   │                   ExpenseCategory, CostCenter
│   ├── controller/     PaymentController, ExpenseController
│   ├── entity/         Payment, Expense
│   ├── scheduler/      PaymentSchedulerJob (markOverduePayments 01h00, generateUpcomingRentPayments 06h00)
│   └── service/
├── dashboard/      ✅ Analytics & KPIs par rôle (DG, Commercial, Comptable, SAV)
│   ├── controller/     DashboardController
│   ├── dto/            AgencyDashboardResponse, ExpenseBreakdownItem,
│   │                   RevenueBreakdownItem
│   └── service/
├── storage/        ✅ MinIO – upload direct et URLs présignées
│   ├── controller/     StorageController
│   └── service/
├── bailleur/       ✅ Demandes d'adhésion bailleur, liens bailleur↔agence, création compte OWNER
│   ├── codeList/       BailleurApplicationStatus (PENDING, APPROVED, REJECTED, CANCELLED)
│   ├── controller/     BailleurController
│   ├── dto/            BailleurApplyRequest, BailleurPropertyRequest, BailleurDecisionRequest,
│   │                   BailleurApplicationResponse, BailleurPropertyResponse
│   ├── entity/         BailleurApplication, BailleurApplicationProperty, BailleurAgencyLink
│   ├── repository/     BailleurApplicationRepository, BailleurApplicationPropertyRepository,
│   │                   BailleurAgencyLinkRepository
│   └── service/
├── reservation/    ✅ Réservations hôtelières (client + hôtel + admin)
│   ├── codeList/       ReservationStatus (PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW)
│   ├── controller/     ReservationController
│   ├── dto/            CreateReservationRequest, CancelReservationRequest, ReservationResponse
│   ├── entity/         Reservation
│   ├── mapper/         ReservationMapper
│   ├── repository/     ReservationRepository
│   └── service/
├── ticketing/      ✅ Tickets SAV complets — entités, controller, service, repos, DTOs
│   ├── codeList/       TicketStatus (enum), MessageType (enum)
│   ├── controller/     TicketController
│   ├── dto/            CreateTicketRequest, AddTicketMessageRequest, AssignTicketRequest,
│   │                   ScheduleInterventionRequest (technicienId + interventionPrice),
│   │                   UpdateRepairCostRequest, UpdateTicketStatusRequest,
│   │                   TicketResponse (technicienId, technicienProfession, interventionPrice),
│   │                   TicketMessageResponse, TicketAttachmentResponse
│   ├── entity/         Ticket (+ technicien FK + interventionPrice), TicketAttachment, TicketMessage
│   ├── mapper/         TicketMapper
│   ├── repository/     TicketRepository, TicketMessageRepository, TicketAttachmentRepository
│   └── service/        TicketService (interface) + TicketServiceImpl
├── technicien/     ✅ Prestataires externes SAV — gérés par agence ou hôtel (pas de compte plateforme)
│   ├── controller/     TechnicienController (6 endpoints /v1/technicians)
│   ├── dto/            CreateTechnicienRequest, UpdateTechnicienRequest, TechnicienResponse
│   ├── entity/         Technicien (soft delete, available, agency XOR hotel)
│   ├── repository/     TechnicienRepository
│   └── service/        TechnicienService (interface) + TechnicienServiceImpl
├── contract/       ✅ Baux et contrats — cycle de vie complet + génération premier loyer
│   ├── codeList/       ContractStatus (DRAFT→PENDING_SIGNATURE→ACTIVE→TERMINATED|CANCELLED)
│   ├── controller/     ContractController (8 endpoints /v1/contracts)
│   ├── dto/            CreateContractRequest, TerminateContractRequest, ContractResponse
│   ├── entity/         Contract
│   ├── mapper/         ContractMapper
│   ├── repository/     ContractRepository
│   └── service/        ContractService (interface) + ContractServiceImpl
│                       → crée le 1er loyer à activate(), génère PDF à submit()
├── document/       ✅ Génération PDF Thymeleaf → MinIO — pas d'API REST
│   ├── codeList/       DocumentStatus, DocumentType, RefType
│   ├── entity/         Document
│   ├── generator/      ContractGenerator, InvoiceGenerator, ReceiptGenerator (@Service)
│   ├── repository/     DocumentRepository
│   └── service/        DocumentService (interface) + DocumentServiceImpl
│                       → pipeline : entité → Thymeleaf → PDF → MinIO bucket documents-generated
├── notification/   ✅ Email (JavaMail) et SMS (LAfricaMobile) — pas d'entité in-app
│   ├── config/         NotificationConfig
│   └── service/        EmailService, SmsService
├── common/         BaseEntity, exceptions, CustomResponse, RoleGuard, utils
│   ├── base/           BaseEntity (id UUID, createdAt, updatedAt)
│   ├── codelist/       LaCodeList (référentiels métier)
│   ├── constants/      Constants, ResponseMessageConstants
│   ├── exception/      CustomException, BadRequestException, ConflictException,
│   │                   NotFoundException, UnAuthorizedException, StorageException
│   ├── response/       CustomResponse
│   └── util/           RoleGuard, RequestHeaderParser, KeycloakJwtRolesConverter,
│                       OtpUtils
└── config/         SecurityConfig, OpenApiConfig, CustomEntryPoint/AccessDenied
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
| V001 | `create_schema.sql` | Schéma `administrative` |
| V002 | `create_users.sql` | `users`, `user_roles` |
| V003 | `create_otp_verifications.sql` | `otp_verifications` |
| V004 | `create_partner_applications.sql` | `partner_applications` |
| V005 | `create_application_status_logs.sql` | `application_status_logs` |
| V006 | `add_storage_slug_to_partner_applications.sql` | `storage_slug` sur `partner_applications` |
| V007 | `create_la_code_list.sql` | `la_code_list` |
| V008 | `create_agencies.sql` | `agencies` |
| V009 | `add_agency_id_to_users.sql` | `agency_id` sur `users` |
| V010 | `create_tenants.sql` | `tenants` |
| V011 | `create_contracts.sql` | `contracts` |
| V012 | `drop_stale_role_column_from_users.sql` | Suppression colonne obsolète |
| V013 | `create_properties.sql` | `properties`, `property_media`, `property_documents` |
| V014 | `create_payments.sql` | `payments` |
| V015 | `create_expenses.sql` | `expenses` |
| V016 | `create_user_sub_roles.sql` | `user_sub_roles` (id, user_id, role, scope, created_at) |
| V017 | `create_hotels.sql` | `hotels` + `hotel_id` sur `users` |
| V018 | `create_tickets.sql` | `tickets`, `ticket_messages`, `ticket_attachments` |
| V019 | `seed_ticket_code_lists.sql` | Seed `la_code_list` (TICKET_CATEGORY, TICKET_PRIORITY, TICKET_ATTACHMENT_TYPE) |
| V020 | `seed_sub_roles_code_lists.sql` | Seed `la_code_list` (ROLE_AGENCE, ROLE_HOTEL, ROLE_UBAX_INTERNAL) |
| V021 | `fix_user_sub_roles_scope_constraint.sql` | Correction contrainte `chk_user_sub_roles_scope` (valeurs majuscules) |
| V022 | `create_bailleur_agency_links.sql` | `bailleur_agency_links` (contrainte unique bailleur↔agence) |
| V023 | `seed_bailleur_system_config.sql` | Seed `la_code_list` (GEO_CONFLICT_RADIUS_METERS = 50 m) |
| V024 | `add_coordinates_to_partner_applications.sql` | `latitude`, `longitude` sur `partner_applications` |
| V025 | `create_bailleur_applications.sql` | `bailleur_applications`, `bailleur_application_properties` |
| V026 | `seed_id_type_code_list.sql` | Seed `la_code_list` (ID_TYPE : CNI, PASSEPORT, PERMIS_CONDUIRE, TITRE_SEJOUR, CARTE_CONSULAIRE) |
| V027–V034 | *(migrations hôtel, villes, représentant légal…)* | Voir fichiers SQL |
| V035 | `create_property_amenities.sql` | Table `property_amenities`, migration booléens → table, suppression colonnes booléennes |
| V036 | `seed_property_amenity_code_lists.sql` | Seed `la_code_list` (PROPERTY_AMENITY : POOL, GENERATOR, WATER_TANK, AC, SECURITY, PARKING, ELEVATOR, GARDEN, FURNISHED, PETS) |
| V037 | `add_updated_at_to_property_amenities.sql` | Colonne `updated_at` manquante sur `property_amenities` (BaseEntity) |
| V038 | `add_verified_by_id_to_property_documents.sql` | Colonne `verified_by_id` manquante sur `property_documents` (serveurs déployés avant correction V013) |
| V039 | `create_reservations.sql` | Table `reservations` (statuts PENDING → CONFIRMED → COMPLETED \| CANCELLED \| NO_SHOW) |
| V040 | `create_documents.sql` | Table `documents` (ref_id, ref_type, doc_type, status PENDING\|GENERATED\|SENT\|FAILED, file_url, generated_by FK users) |
| V041 | `make_payment_recorded_by_nullable.sql` | `recorded_by` nullable sur `payments` (paiements système sans acteur humain) |
| V042 | `drop_property_type_check_constraint.sql` | Suppression contrainte CHECK statique `properties_property_type_check` — bloquait HOTEL_SUITE et autres types hôteliers ajoutés en V034 |
| V043 | `add_user_id_to_bailleur_applications.sql` | Colonne `user_id UUID` (nullable, FK → `users.id`) sur `bailleur_applications` — lie la demande au compte CLIENT authentifié |
| V044 | `add_hotel_id_to_properties.sql` | Colonne `hotel_id UUID` (FK → `hotels.id`) sur `properties` — filtre biens par hôtel |
| V045 | `create_techniciens.sql` | Table `techniciens` (agency_id XOR hotel_id, available, soft delete, CHECK constraint structure) |
| V046 | `add_technicien_to_tickets.sql` | Colonnes `technicien_id UUID` (FK → `techniciens.id`) + `intervention_price DECIMAL(15,2)` sur `tickets` |
| V047 | `seed_technicien_profession_code_lists.sql` | Seed `la_code_list` (TECHNICIEN_PROFESSION : PLOMBIER, ELECTRICIEN, SERRURIER, MENUISIER, MACON, PEINTRE, CLIMATISATION, VITRERIE, JARDINAGE, NETTOYAGE, DESINSECTISATION, AUTRE) |

Prochaine version disponible : **V048**

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

### Sous-rôles (User Sub-Roles)

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/admin/users/{userId}/sub-roles` | `SUPER_ADMIN` | Assigner des sous-rôles |
| `GET` | `/v1/admin/users/{userId}/sub-roles` | `ADMIN` | Consulter les sous-rôles |
| `DELETE` | `/v1/admin/users/{userId}/sub-roles/{role}` | `SUPER_ADMIN` | Révoquer un sous-rôle |

### Agencies

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/agencies` | Authentifié 📱 | Liste paginée des agences actives (filtre optionnel `?city=`) — utilisé avant `POST /v1/bailleur/apply` |

### Agency Team

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/agency/team` | `PARTNER` | Lister membres |
| `POST` | `/v1/agency/team` | `PARTNER` + `DIRECTEUR_AGENCE` | Ajouter membre |
| `PUT` | `/v1/agency/team/{userId}/role` | `PARTNER` + `DIRECTEUR_AGENCE` | Changer rôle |
| `DELETE` | `/v1/agency/team/{userId}` | `PARTNER` + `DIRECTEUR_AGENCE` | Retirer membre |
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

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/partner/apply` | Public | Soumettre candidature |
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

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/properties` | Public | Liste paginée + filtres (inclut `coverPhotoUrl`) |
| `GET` | `/v1/properties/{id}` | Public | Détail (inclut `coverPhotoUrl`) |
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

> **Création avec images** : `POST /v1/tickets` accepte un champ `attachments[]` (optionnel). Chaque entrée est une URL pré-uploadée via `GET /v1/storage/presign/ticket-attachment`. Les fichiers sont sauvegardés comme `TicketAttachment` (type `INCIDENT_PHOTO` par défaut) dans la même transaction.
> **Accès CLIENT** : un CLIENT peut créer un ticket, lister ses propres tickets (`/mine`) et consulter le détail de ses tickets uniquement (vérification reporter — 403 si pas déclarant).

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

### Bailleur

> **Flux mobile** : `CLIENT` connecté → choisit une agence (`GET /v1/agencies`) → soumet une demande (`POST /v1/bailleur/apply`) → agence approuve → rôle `UBAX_OWNER` ajouté automatiquement → token refreshé → formulaire de saisie de bien débloqué.
> **`POST /v1/bailleur/apply`** : requiert JWT `CLIENT`. Les champs `firstName`, `lastName`, `phone`, `email` sont extraits automatiquement du compte — seuls `agencyId`, `idType`, `idNumber` et `properties` sont requis dans le body.

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/bailleur/apply` | `CLIENT` 📱 | Soumettre une demande d'adhésion bailleur |
| `GET` | `/v1/bailleur/my-applications` | `OWNER` 📱 | Mes demandes d'adhésion (filtrable par statut) |
| `GET` | `/v1/bailleur/my-agencies` | `OWNER` 📱 | Agences auxquelles je suis lié |
| `GET` | `/v1/bailleur/agency/applications` | `PARTNER` + `DIRECTEUR_AGENCE` | Liste paginée des demandes reçues |
| `GET` | `/v1/bailleur/agency/applications/{id}` | `PARTNER` + `DIRECTEUR_AGENCE` | Détail d'une demande |
| `PATCH` | `/v1/bailleur/agency/applications/{id}/decision` | `PARTNER` + `DIRECTEUR_AGENCE` | Approuver / rejeter |
| `GET` | `/v1/bailleur/admin/applications` | `ADMIN` · `SUPER_ADMIN` | Vue globale toutes agences |

### Reservation

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `POST` | `/v1/reservations` | `CLIENT` | Soumettre une réservation |
| `GET` | `/v1/reservations/mine` | `CLIENT` | Mes réservations |
| `GET` | `/v1/reservations/{id}` | `CLIENT/PARTNER` | Détail |
| `DELETE` | `/v1/reservations/{id}` | `CLIENT` | Annuler (PENDING uniquement) |
| `GET` | `/v1/reservations/hotel` | `PARTNER` (hôtel) | Réservations de mon hôtel |
| `PATCH` | `/v1/reservations/{id}/confirm` | `PARTNER` (hôtel) | Confirmer |
| `PATCH` | `/v1/reservations/{id}/cancel` | `PARTNER` (hôtel) | Annuler côté hôtel |
| `PATCH` | `/v1/reservations/{id}/complete` | `PARTNER` (hôtel) | Marquer séjour terminé |
| `PATCH` | `/v1/reservations/{id}/no-show` | `PARTNER` (hôtel) | Marquer no-show |
| `GET` | `/v1/reservations` | `ADMIN/SUPER_ADMIN` | Toutes les réservations |

### Dashboard & Storage

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/dashboard/agency` | `PARTNER/ADMIN` | KPIs agence par sous-rôle |
| `POST` | `/v1/storage/upload` | Authentifié | Upload direct multipart |
| `POST` | `/v1/storage/upload/agency-logo` | Authentifié | Upload logo agence |
| `GET` | `/v1/storage/presign` | Authentifié | URL présignée générique |
| `GET` | `/v1/storage/presign/property-media` | Authentifié | URL présignée média bien |
| `GET` | `/v1/storage/presign/property-document` | Authentifié | URL présignée doc bien |
| `GET` | `/v1/storage/presign/tenant-document` | Authentifié | URL présignée KYC |
| `GET` | `/v1/storage/presign/agency-logo` | Authentifié | URL présignée logo |
| `GET` | `/v1/storage/presign/ticket-attachment` | Authentifié | URL présignée ticket |

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
`tenant-documents`, `documents-generated`, `ticket-attachments`, `partner-documents`

---

## Observabilité

- Actuator : `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- Prometheus + Grafana inclus dans le docker-compose
