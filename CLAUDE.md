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
├── auth/           Authentification, inscription, OTP, gestion des admins
├── partner/        Cycle de vie des candidatures partenaires (agences / hôtels)
├── tenant/         Gestion des locataires
├── property/       Propriétés, médias, documents
├── contract/       Contrats
├── document/       Génération PDF (contrat, facture, reçu) via Thymeleaf
├── storage/        MinIO – upload/presigned URLs
├── notification/   Email (JavaMail + Thymeleaf) · SMS (LAfricaMobile)
├── ticketing/      Tickets de support
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

### Sous-rôles opérationnels (UbaxAdminRole)
Portés par un attribut applicatif (pas de rôle Keycloak distinct) :
- `FINANCE_MANAGER` – finances, commissions, rapports
- `SUPPORT_MANAGER` – tickets et réclamations
- `PARTNER_MANAGER` – onboarding agences/hôtels
- `CONTENT_MODERATOR` – modération des annonces

### Vérification des rôles
Utiliser `RoleGuard` pour les contrôles programmatiques dans les contrôleurs :
```java
RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
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
