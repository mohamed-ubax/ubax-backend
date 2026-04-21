# CLAUDE.md – ubax-platform

## Vue d'ensemble

**ubax-platform** est le backend de la plateforme UBAX (Afrique) : marketplace immobilière/hôtelière permettant à des agences et hôtels partenaires de publier des annonces, gérer des locataires et des contrats.

- **Stack** : Spring Boot 3.4.5 · Java 21 · PostgreSQL · Keycloak · MinIO
- **Port** : `9999` (local) · Context path : `/api`
- **Realm Keycloak** : `ubax-plateform`

---

## Architecture des modules

```
src/main/java/com/africa/ubaxplatform/
├── auth/           Authentification, inscription, OTP, gestion des admins,
│                   gestion de l'équipe agence (AgencyTeamController/Service)
├── partner/        Cycle de vie des candidatures partenaires (agences / hôtels)
├── tenant/         Gestion des locataires
├── property/       Propriétés, médias, documents
├── contract/       Contrats
├── document/       Génération PDF (contrat, facture, reçu) via Thymeleaf
├── storage/        MinIO – upload/presigned URLs
├── notification/   Email (JavaMail + Thymeleaf) · SMS (LAfricaMobile)
├── ticketing/      Tickets de support
├── payment/        Paiements (loyers, dépôts, commissions) et dépenses agence
├── dashboard/      Analytics & KPIs par rôle (DG, Commercial, Comptable, SAV)
├── common/         BaseEntity, exceptions, CustomResponse, RoleGuard, utils
└── config/         SecurityConfig, OpenApiConfig, CustomEntryPoint/AccessDenied
```

---

## Sécurité & Rôles

### Deux filter chains Spring Security
| Ordre | Périmètre | Comportement |
|-------|-----------|--------------|
| 1 | Routes publiques (`WHITELIST`) | Aucun JWT requis |
| 2 | Toutes les autres routes | JWT Keycloak obligatoire |

### Hiérarchie des rôles
| Rôle (UserRole) | Realm Keycloak | Périmètre |
|-----------------|----------------|-----------|
| `SUPER_ADMIN`   | `UBAX_SUPER_ADMIN` | Accès total, gestion admins, config |
| `ADMIN`         | `UBAX_ADMIN`       | Opérations courantes |
| `PARTNER`       | `UBAX_PARTNER`     | Espace partenaire |
| `CUSTOMER`      | `UBAX_CUSTOMER`    | Espace client |

### Sous-rôles opérationnels back-office (UbaxAdminRole)
Portés par un attribut applicatif (pas de rôle Keycloak distinct) :
- `FINANCE_MANAGER` – finances, commissions, rapports
- `SUPPORT_MANAGER` – tickets et réclamations
- `PARTNER_MANAGER` – onboarding agences/hôtels
- `CONTENT_MODERATOR` – modération des annonces

### Sous-rôles internes partenaire (PartnerRole)
Stockés dans `users.partner_role` (colonne DB, non présents dans le JWT Keycloak).
Vérifié via `RoleGuard.checkPartnerRole(dbUser, PartnerRole.DIRECTEUR_AGENCE)` après
chargement de l'entité `User` depuis la base.

**Agence immobilière :**
| PartnerRole | Périmètre |
|-------------|-----------|
| `DIRECTEUR_AGENCE` | Accès complet + gestion de l'équipe |
| `COMMERCIAL` | Prospects, rendez-vous, biens |
| `COMPTABLE_AGENCE` | Finances (solde masqué) |
| `AGENT_SAV` | Tickets support |

**Hôtel :**
| PartnerRole | Périmètre |
|-------------|-----------|
| `GERANT_HOTEL` | Accès complet hôtel |
| `RECEPTIONNISTE` | Réservations, check-in/out |
| `COMPTABLE_HOTEL` | Facturation et revenus |
| `RESPONSABLE_HEBERGEMENT` | Espaces et chambres |

### Vérification des rôles
Utiliser `RoleGuard` pour les contrôles programmatiques dans les contrôleurs :
```java
// Rôles Keycloak (JWT)
RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

// Rôle interne partenaire (DB) — charger le User d'abord
User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
RoleGuard.checkPartnerRole(dbUser, PartnerRole.DIRECTEUR_AGENCE);
RoleGuard.checkHasAgency(dbUser);
```
Le converter `KeycloakJwtRolesConverter` extrait les rôles realm Keycloak du JWT.

---

## Base de données

- **Schéma** : `administrative`
- **Migrations** : Flyway (`src/main/resources/db/migration/V*__*.sql`)
- `ddl-auto: none` – Flyway est la seule source de vérité pour le schéma
- Toute nouvelle table/colonne = nouvelle migration versionnée

---

## Infrastructure locale (Docker)

```
docker/docker-compose.yml   # PostgreSQL, Keycloak, MinIO, pgAdmin, Prometheus, Grafana
```

Variables d'environnement dans `docker/.env` (modifié sur la branche courante).

Commandes utiles :
```bash
# Démarrer l'infra
docker compose -f docker/docker-compose.yml up -d

# Build + run de l'application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

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

## Conventions de code

- **Formatage** : Google Java Format via `fmt-maven-plugin` (s'applique à chaque build)
- **Lombok** : `@Data`, `@Builder`, `@RequiredArgsConstructor` partout
- **Réponses API** : toujours `CustomResponse` (body / status / message / data)
- **Exceptions** : hiérarchie `CustomException` → handlers dans `ApiExceptionHandler`
- **Mappers** : classes dédiées `*Mapper` (pas de MapStruct, mapping manuel)
- **Interfaces / Implémentations** : `service/interfaces/` + `service/impl/`

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

## Migrations Flyway (dernières versions)

| Version | Fichier | Description |
|---------|---------|-------------|
| V013 | create_properties.sql | Table `properties` et médias/docs |
| V014 | create_payments.sql | Table `payments` |
| V015 | create_expenses.sql | Table `expenses` |
| V016 | add_partner_role_to_users.sql | Colonne `partner_role` sur `users` |

---

## Buckets MinIO

`users-avatars`, `agencies-logos`, `properties-media`, `property-documents`,
`tenant-documents`, `documents-generated`, `ticket-attachments`, `partner-documents`

---

## Observabilité

- Actuator : `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- Prometheus + Grafana inclus dans le docker-compose

---

## Swagger / OpenAPI

- URL locale : `http://localhost:9999/api/swagger-ui.html`
- Auth : Bearer Token (JWT Keycloak)