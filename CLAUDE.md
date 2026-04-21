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
├── auth/           ✅ Auth, inscription OTP, reset password, admins, équipe agence
│   ├── codeList/       UserRole, UbaxAdminRole, PartnerRole, OtpPurpose
│   ├── controller/     AuthController, AdminController, UserProfileController,
│   │                   AgencyTeamController
│   ├── dto/
│   ├── entity/         User, Agency, OtpVerification, UserPreferences
│   ├── mapper/         UserMapper, AgencyMapper, AgencyTeamMapper
│   ├── repository/     UserRepository, AgencyRepository, OtpVerificationRepository
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
├── payment/        ✅ Paiements et dépenses agence
│   ├── codeList/       PaymentStatus, PaymentType, PaymentMethod,
│   │                   ExpenseCategory, CostCenter
│   ├── controller/     PaymentController, ExpenseController
│   ├── entity/         Payment, Expense
│   └── service/
├── dashboard/      ✅ Analytics & KPIs par rôle (DG, Commercial, Comptable, SAV)
│   ├── controller/     DashboardController
│   ├── dto/            AgencyDashboardResponse, ExpenseBreakdownItem,
│   │                   RevenueBreakdownItem
│   └── service/
├── storage/        ✅ MinIO – upload direct et URLs présignées
│   ├── controller/     StorageController
│   └── service/
├── contract/       ⏳ Entité présente — controller/service à créer
│   ├── codeList/       ContractStatus
│   └── entity/         Contract
├── ticketing/      ⏳ Entités présentes — controller/service à créer
│   ├── codeList/       TicketStatus
│   ├── entity/         Ticket, TicketAttachment, TicketMessage
│   └── mapper/         TicketMapper
├── document/       ⏳ Entité présente — génération PDF Thymeleaf, pas d'API REST
│   ├── codeList/       DocumentStatus, DocumentType, RefType
│   ├── entity/         Document
│   └── generator/      ContractGenerator, InvoiceGenerator, ReceiptGenerator
├── notification/   ⏳ Email (JavaMail) et SMS (LAfricaMobile) — pas d'entité in-app
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

Routes publiques actuelles : `/api-docs/**`, `/swagger-ui/**`, `/v1/auth/**` (sauf reset-password), `/v1/partner/apply`, `GET /v1/properties/**`, `GET /v1/code-list/type/**`.

### Rôles Keycloak (`UserRole` enum)

| UserRole | Realm Keycloak | Périmètre |
|----------|----------------|-----------|
| `SUPER_ADMIN` | `UBAX_SUPER_ADMIN` | Accès total, gestion admins, config |
| `ADMIN` | `UBAX_ADMIN` | Administration courante |
| `PARTNER` | `UBAX_PARTNER` | Partenaire (agence **ou** hôtel) – sous-rôles internes via `PartnerRole` |
| `CLIENT` | `UBAX_CLIENT` | Locataire / acheteur |
| `OWNER` | `UBAX_OWNER` | Propriétaire individuel |

### Sous-rôles back-office (`UbaxAdminRole` — attribut applicatif, hors JWT)

- `DIRECTEUR_GENERAL` – vision globale, accès complet back-office
- `SUPPORT_CLIENT` – tickets, réclamations partenaires et clients
- `OPERATIONS` – onboarding partenaires, modération des annonces
- `FINANCE` – abonnements, commissions, revenus, rapports financiers
- `COMMERCIAL` – acquisition partenaires, suivi commercial

### Sous-rôles internes partenaire (`PartnerRole` — colonne `users.partner_role`, hors JWT)

**Agence immobilière :**

| PartnerRole | Tableau de bord | Périmètre |
|-------------|-----------------|-----------|
| `DIRECTEUR_AGENCE` | DG | Accès complet + gestion équipe |
| `COMMERCIAL` | Commercial | Prospects, rendez-vous, biens |
| `COMPTABLE_AGENCE` | Comptable | Finances (solde masqué) |
| `AGENT_SAV` | SAV | Tickets et interventions |

**Hôtel :**

| PartnerRole | Périmètre |
|-------------|-----------|
| `GERANT_HOTEL` | Accès complet hôtel |
| `RECEPTIONNISTE` | Réservations, check-in/out |
| `COMPTABLE_HOTEL` | Facturation et revenus |
| `RESPONSABLE_HEBERGEMENT` | Espaces et chambres |

### Vérification des rôles dans les contrôleurs

```java
// Rôle Keycloak (JWT) — toujours en premier
RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);

// Rôle interne partenaire (DB) — charger l'entité User d'abord
User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
RoleGuard.checkPartnerRole(dbUser, PartnerRole.DIRECTEUR_AGENCE);
RoleGuard.checkHasAgency(dbUser);
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
| V016 | `add_partner_role_to_users.sql` | `partner_role` sur `users` |
| V017 | `create_hotels.sql` | Table `hotels` + colonne `hotel_id` sur `users` |

Prochaine version disponible : **V018**

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

### Agency Team

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/agency/team` | `PARTNER` | Lister membres |
| `POST` | `/v1/agency/team` | `PARTNER` + `DIRECTEUR_AGENCE` | Ajouter membre |
| `PUT` | `/v1/agency/team/{userId}/role` | `PARTNER` + `DIRECTEUR_AGENCE` | Changer rôle |
| `DELETE` | `/v1/agency/team/{userId}` | `PARTNER` + `DIRECTEUR_AGENCE` | Retirer membre |

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
| `GET` | `/v1/tenants` | `ADMIN/AGENCY` | Liste paginée |
| `GET` | `/v1/tenants/{id}` | `ADMIN/AGENCY` | Détail |
| `PATCH` | `/v1/tenants/{id}/qualify` | `ADMIN/AGENCY` | Qualifier |
| `PATCH` | `/v1/tenants/{id}/reject` | `ADMIN/AGENCY` | Rejeter |
| `DELETE` | `/v1/tenants/{id}` | `ADMIN` | Archiver |

### Property

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/properties` | Public | Liste paginée + filtres |
| `GET` | `/v1/properties/{id}` | Public | Détail |
| `GET` | `/v1/properties/mine` | `PARTNER/AGENCY` | Mes biens |
| `POST` | `/v1/properties` | `PARTNER/AGENCY` | Créer brouillon |
| `PUT` | `/v1/properties/{id}` | `PARTNER/AGENCY` | Mettre à jour |
| `PATCH` | `/v1/properties/{id}/submit` | `PARTNER/AGENCY` | Soumettre en modération |
| `DELETE` | `/v1/properties/{id}` | `PARTNER/AGENCY` | Archiver |
| `PATCH` | `/v1/properties/{id}/status` | `ADMIN` | Modérer (approuver/rejeter) |
| `POST` | `/v1/properties/{id}/media/upload` | `PARTNER/AGENCY` | Upload direct média |
| `POST` | `/v1/properties/{id}/media` | `PARTNER/AGENCY` | Lier média pré-uploadé |
| `GET` | `/v1/properties/{id}/media` | Public | Liste médias |
| `PATCH` | `/v1/properties/{id}/media/{mediaId}/cover` | `PARTNER/AGENCY` | Photo couverture |
| `DELETE` | `/v1/properties/{id}/media/{mediaId}` | `PARTNER/AGENCY` | Supprimer média |
| `POST` | `/v1/properties/{id}/documents` | `PARTNER/AGENCY` | Ajouter document |
| `GET` | `/v1/properties/{id}/documents` | Authentifié | Liste documents |
| `PATCH` | `/v1/properties/{id}/documents/{docId}/verify` | `ADMIN` | Vérifier document |
| `DELETE` | `/v1/properties/{id}/documents/{docId}` | `PARTNER/AGENCY` | Supprimer document |
| `PATCH` | `/v1/properties/{id}/boost` | `ADMIN` | Activer boost |
| `DELETE` | `/v1/properties/{id}/boost` | `ADMIN` | Retirer boost |
| `PATCH` | `/v1/properties/{id}/expiration` | `ADMIN` | Fixer expiration |
| `DELETE` | `/v1/properties/{id}/expiration` | `ADMIN` | Retirer expiration |

### Payment & Expense

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/payments` | `PARTNER/AGENCY` | Liste paginée + filtres |
| `GET` | `/v1/payments/late` | `PARTNER/AGENCY` | Loyers en retard |
| `GET` | `/v1/payments/{id}` | `PARTNER/AGENCY` | Détail |
| `POST` | `/v1/payments` | `PARTNER/AGENCY` | Enregistrer paiement |
| `PATCH` | `/v1/payments/{id}/status` | `PARTNER/AGENCY` | Mettre à jour statut |
| `DELETE` | `/v1/payments/{id}` | `PARTNER/AGENCY` | Supprimer |
| `GET` | `/v1/expenses` | `PARTNER/AGENCY` | Liste paginée + filtres |
| `GET` | `/v1/expenses/{id}` | `PARTNER/AGENCY` | Détail |
| `POST` | `/v1/expenses` | `PARTNER/AGENCY` | Enregistrer dépense |
| `DELETE` | `/v1/expenses/{id}` | `PARTNER/AGENCY` | Supprimer |

### Dashboard & Storage

| Méthode | Chemin | Rôle | Description |
|---------|--------|------|-------------|
| `GET` | `/v1/dashboard/agency` | `PARTNER/AGENCY/ADMIN` | KPIs agence par rôle |
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
