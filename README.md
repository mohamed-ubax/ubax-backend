# ubax-platform

Backend de la plateforme **UBAX** — marketplace immobilière et hôtelière pour l'Afrique (Sénégal).

## Stack technique

| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 21 | Langage |
| Spring Boot | 3.4.5 | Framework |
| PostgreSQL | 16 | Base de données relationnelle |
| Keycloak | 24 | Identity Provider (OAuth2 / OIDC) |
| MinIO | RELEASE.2024 | Object storage (médias, documents) |
| Flyway | – | Migrations SQL |
| Swagger / OpenAPI | 3 | Documentation API |
| Docker Compose | – | Infrastructure locale |

## Prérequis

- Java 21+
- Maven 3.9+ (ou utiliser le wrapper `./mvnw`)
- Docker & Docker Compose

## Démarrage rapide

```bash
# 1. Démarrer l'infrastructure (PostgreSQL, Keycloak, MinIO, pgAdmin, Prometheus, Grafana)
docker compose -f docker/docker-compose.yml up -d

# 2. Copier et configurer les variables d'environnement
cp docker/.env.example docker/.env
# → renseigner KEYCLOAK_CLIENT_ID, KEYCLOAK_CLIENT_SECRET

# 3. Lancer l'application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

L'API est disponible sur `http://localhost:9999/api`.  
Swagger UI : `http://localhost:9999/api/swagger-ui.html`

## Variables d'environnement clés

| Variable | Description |
|----------|-------------|
| `KEYCLOAK_CLIENT_ID` | Client Keycloak (**requis**) |
| `KEYCLOAK_CLIENT_SECRET` | Secret client (**requis**) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Connexion PostgreSQL (défaut : `localhost/5433/db-ubax`) |
| `MINIO_ENDPOINT` | URL MinIO (défaut : `http://localhost:9000`) |
| `BOOTSTRAP_ENABLED` | `true` = créer le super admin au 1er démarrage |
| `SECURITY_ENABLED` | `false` uniquement en dev pour désactiver la vérification JWT |

## Architecture

```
src/main/java/com/africa/ubaxplatform/
├── auth/        Auth, utilisateurs, sous-rôles (UserSubRole), équipe agence
├── partner/     Candidatures partenaires (agences / hôtels)
├── tenant/      Dossiers locataires (KYC)
├── property/    Biens immobiliers, médias, documents, modération
├── payment/     Paiements (loyers, dépôts, commissions) et dépenses
├── dashboard/   KPIs analytiques par rôle agence
├── storage/     Upload MinIO (multipart + URL présignée)
├── ticketing/   Tickets de support (entités prêtes)
├── contract/    Contrats (entité prête)
├── document/    Génération PDF Thymeleaf
├── notification/ Email (JavaMail) et SMS (LAfricaMobile)
└── common/      Exceptions, réponses, RoleGuard, utils
```

## Système d'autorisation à deux niveaux

### Niveau 1 — Rôles Keycloak (JWT)

Cinq rôles realm `UBAX_*` extraits du token à chaque requête :

| Enum `UserRole` | Realm Keycloak | Périmètre |
|-----------------|----------------|-----------|
| `SUPER_ADMIN` | `UBAX_SUPER_ADMIN` | Accès total |
| `ADMIN` | `UBAX_ADMIN` | Back-office UBAX |
| `PARTNER` | `UBAX_PARTNER` | Agence ou hôtel partenaire |
| `OWNER` | `UBAX_OWNER` | Propriétaire individuel |
| `CLIENT` | `UBAX_CLIENT` | Locataire / acheteur |

### Niveau 2 — Sous-rôles applicatifs (table `user_sub_roles`)

Les sous-rôles affinent l'accès à l'intérieur d'un rôle Keycloak. Ils ne transitent pas par le JWT.

| Scope | Sous-rôles | Pour qui |
|-------|-----------|----------|
| `UBAX_INTERNAL` | DIRECTEUR_GENERAL · SUPPORT_CLIENT · OPERATIONS · FINANCE · COMMERCIAL | ADMIN / SUPER_ADMIN |
| `AGENCE` | DIRECTEUR_AGENCE · COMMERCIAL · COMPTABLE_AGENCE · AGENT_SAV | PARTNER agence |
| `HOTEL` | GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL · RESPONSABLE_HEBERGEMENT | PARTNER hôtel |

Gestion via `POST/GET/DELETE /v1/admin/users/{userId}/sub-roles`.

### Pattern de vérification dans les contrôleurs

```java
// Niveau 1 — JWT
RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

// Niveau 2 — DB
User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
RoleGuard.checkAgenceRole(dbUser, subRoleRepo, AgenceRole.DIRECTEUR_AGENCE);
```

## Base de données

- Schéma PostgreSQL : `administrative`
- Migrations Flyway : `src/main/resources/db/migration/V*__*.sql`
- `ddl-auto: none` — Flyway est la seule source de vérité

Migrations actuelles : **V001 → V019** (prochaine : V020)

## Conventions de code

- **Google Java Format** appliqué automatiquement à chaque `./mvnw compile`
- **Lombok** : `@SuperBuilder` sur les entités, `@RequiredArgsConstructor` sur les services
- **Réponses** : toujours `CustomResponse(status, statusCode, message, data)`
- **Exceptions** : hiérarchie `CustomException` → `ApiExceptionHandler` → JSON normalisé
- **Services** : interface dans `service/interfaces/` + implémentation dans `service/impl/`
- **Enums** pour les états de workflow (compile-time safety)
- **String + `la_code_list`** pour les listes configurables UI (catégorie, priorité…)

## Tests

```bash
./mvnw test                   # tous les tests
./mvnw test -Dtest=*IT        # intégration uniquement (Testcontainers)
```

## Observabilité

- Health : `GET /actuator/health`
- Métriques Prometheus : `GET /actuator/prometheus`
- Grafana inclus dans le docker-compose : `http://localhost:3000`

## Buckets MinIO

`users-avatars` · `agencies-logos` · `properties-media` · `property-documents`  
`tenant-documents` · `documents-generated` · `ticket-attachments` · `partner-documents`
