# ubax-platform — Backend API

Backend de la plateforme **UBAX** : marketplace immobilière et hôtelière (Afrique de l'Ouest).  
Permet aux agences et hôtels partenaires de publier des annonces, gérer des locataires, des contrats, des paiements et leur équipe interne.

---

## Stack technique

| Composant | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.4.5 |
| PostgreSQL | 16 |
| Keycloak | 24 |
| MinIO | RELEASE.2024 |
| Flyway | 10.x |

- **Port** : `9999` · **Context path** : `/api`
- **Realm Keycloak** : `ubax-plateform`
- **Swagger UI** : `http://localhost:9999/api/swagger-ui.html`

---

## Démarrage rapide

```bash
# 1. Lancer l'infrastructure (PostgreSQL, Keycloak, MinIO, pgAdmin)
docker compose -f docker/docker-compose.yml up -d

# 2. Lancer l'application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Variables d'environnement requises dans `docker/.env` :

```env
KEYCLOAK_CLIENT_ID=ubax-backend
KEYCLOAK_CLIENT_SECRET=<secret>
DB_HOST=localhost
DB_PORT=5433
DB_NAME=db-ubax
MINIO_ENDPOINT=http://localhost:9000
```

---

## Modules API — État actuel

### ✅ Modules opérationnels (controllers + services)

| Module | Base URL | Endpoints | Description |
|--------|----------|-----------|-------------|
| **Auth** | `/v1/auth` | 13 | Login (email/phone), inscription OTP, logout, reset password |
| **Admin** | `/v1/admin/users` | 4 | Gestion des comptes administrateurs |
| **Agency Team** | `/v1/agency/team` | 4 | Équipe interne agence (sous-rôles : DG, Commercial, Comptable, SAV) |
| **Partner** | `/v1/partner` | 4 | Candidatures partenaires (agences / hôtels) |
| **Tenant** | `/v1/tenants` | 8 | Dossiers locataires KYC |
| **Property** | `/v1/properties` | 20 | Biens immobiliers, médias, documents, modération, boost |
| **Payment** | `/v1/payments` | 6 | Paiements (loyers, dépôts, commissions, retards) |
| **Expense** | `/v1/expenses` | 4 | Dépenses de l'agence |
| **Dashboard** | `/v1/dashboard` | 1 | KPIs agence (DG, Commercial, Comptable, SAV) |
| **Storage** | `/v1/storage` | 7 | Upload direct et URLs présignées MinIO |
| **Code List** | `/v1/code-list` | 5 | Référentiels métier (types, catégories…) |
| **User Profile** | `/v1/users` | 1 | Avatar utilisateur |

**Total : ~77 endpoints opérationnels**

### ⏳ Modules en attente (entités présentes, pas encore de controllers)

| Module | Entités disponibles | Endpoints à créer |
|--------|--------------------|--------------------|
| **Contract** | `Contract` | CRUD + signature numérique |
| **Ticketing** | `Ticket`, `TicketAttachment`, `TicketMessage` | CRUD tickets + assignation + interventions |
| **Document** | `Document` | Génération PDF (contrat, facture, reçu) |
| **Notification** | — | Notifications in-app (cloche) |

---

## Sécurité & Rôles

### Rôles Keycloak (vérifiés via JWT)

| Rôle Keycloak | `UserRole` enum | Périmètre |
|---------------|-----------------|-----------|
| `UBAX_SUPER_ADMIN` | `SUPER_ADMIN` | Accès total à toute la plateforme |
| `UBAX_ADMIN` | `ADMIN` | Administration courante |
| `UBAX_PARTNER` | `PARTNER` | Espace partenaire (agence, hôtel) |
| `UBAX_CUSTOMER` | `CUSTOMER` | Espace client |

### Sous-rôles internes partenaire (`PartnerRole` — stockés en base)

Ces rôles affinent l'accès **à l'intérieur** de l'espace partenaire. Ils ne sont **pas** dans le JWT — vérification via DB.

**Agence immobilière :**

| `PartnerRole` | Tableau de bord | Périmètre |
|---------------|-----------------|-----------|
| `DIRECTEUR_AGENCE` | DG | Accès complet + gestion de l'équipe |
| `COMMERCIAL` | Commercial | Prospects, rendez-vous, biens |
| `COMPTABLE_AGENCE` | Comptable | Finances (solde masqué) |
| `AGENT_SAV` | SAV | Tickets et interventions |

**Hôtel :**

| `PartnerRole` | Périmètre |
|---------------|-----------|
| `GERANT_HOTEL` | Accès complet hôtel |
| `RECEPTIONNISTE` | Réservations, check-in/out |
| `COMPTABLE_HOTEL` | Facturation et revenus |
| `RESPONSABLE_HEBERGEMENT` | Espaces et chambres |

### Pattern double-contrôle dans les contrôleurs

```java
// 1. Vérification JWT (Keycloak)
RequestUser caller = RoleGuard.requireAnyRole(parser, request, UserRole.PARTNER);

// 2. Vérification rôle interne DB
User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
RoleGuard.checkPartnerRole(dbUser, PartnerRole.DIRECTEUR_AGENCE);
```

---

## Endpoints détaillés

### Auth — `/v1/auth`

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `POST` | `/login` | Public | Connexion email + mot de passe |
| `POST` | `/login/phone` | Public | Connexion téléphone + mot de passe |
| `POST` | `/register/send-otp` | Public | Envoi OTP d'inscription |
| `POST` | `/register/verify-otp` | Public | Vérification OTP |
| `POST` | `/register/complete` | Public | Finalisation de l'inscription |
| `POST` | `/logout` | Public | Déconnexion (invalide le refresh token) |
| `POST` | `/forgot-password` | Public | Envoi email de réinitialisation |
| `POST` | `/forgot-password/send-otp` | Public | Envoi OTP de réinitialisation SMS |
| `POST` | `/forgot-password/verify-otp` | Public | Vérification OTP réinitialisation |
| `POST` | `/forgot-password/reset` | Public | Réinitialisation mot de passe via OTP |
| `POST` | `/reset-password` | `ADMIN` | Réinitialisation forcée (admin) |
| `GET` | `/roles` | `ADMIN` | Liste des rôles Keycloak |
| `POST` | `/users/{keycloakId}/roles` | `ADMIN` | Assigner un rôle à un utilisateur |
| `DELETE` | `/users/{keycloakId}/roles` | `ADMIN` | Retirer un rôle |

### Property — `/v1/properties`

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `GET` | `/` | Public | Liste paginée avec filtres (ville, type, prix…) |
| `GET` | `/{id}` | Public | Détail d'un bien |
| `GET` | `/mine` | `PARTNER/AGENCY` | Mes biens |
| `POST` | `/` | `PARTNER/AGENCY` | Créer un brouillon |
| `PUT` | `/{id}` | `PARTNER/AGENCY` | Mettre à jour |
| `PATCH` | `/{id}/submit` | `PARTNER/AGENCY` | Soumettre pour modération |
| `DELETE` | `/{id}` | `PARTNER/AGENCY` | Archiver |
| `PATCH` | `/{id}/status` | `ADMIN` | Modération (approuver / rejeter) |
| `POST` | `/{id}/media/upload` | `PARTNER/AGENCY` | Upload direct d'un média |
| `POST` | `/{id}/media` | `PARTNER/AGENCY` | Lier un média pré-uploadé |
| `GET` | `/{id}/media` | Public | Liste des médias |
| `PATCH` | `/{id}/media/{mediaId}/cover` | `PARTNER/AGENCY` | Définir la photo de couverture |
| `DELETE` | `/{id}/media/{mediaId}` | `PARTNER/AGENCY` | Supprimer un média |
| `POST` | `/{id}/documents` | `PARTNER/AGENCY` | Ajouter un document légal |
| `GET` | `/{id}/documents` | Authentifié | Liste des documents |
| `PATCH` | `/{id}/documents/{docId}/verify` | `ADMIN` | Vérifier un document |
| `DELETE` | `/{id}/documents/{docId}` | `PARTNER/AGENCY` | Supprimer un document |
| `PATCH` | `/{id}/boost` | `ADMIN` | Activer / prolonger le boost |
| `DELETE` | `/{id}/boost` | `ADMIN` | Retirer le boost |
| `PATCH` | `/{id}/expiration` | `ADMIN` | Définir une date d'expiration |
| `DELETE` | `/{id}/expiration` | `ADMIN` | Retirer l'expiration |

### Payment — `/v1/payments`

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `GET` | `/` | `PARTNER/AGENCY` | Liste paginée (filtres : statut, type, bien, période) |
| `GET` | `/late` | `PARTNER/AGENCY` | Loyers en retard |
| `GET` | `/{id}` | `PARTNER/AGENCY` | Détail d'un paiement |
| `POST` | `/` | `PARTNER/AGENCY` | Enregistrer un paiement |
| `PATCH` | `/{id}/status` | `PARTNER/AGENCY` | Mettre à jour le statut |
| `DELETE` | `/{id}` | `PARTNER/AGENCY` | Supprimer un paiement |

### Expense — `/v1/expenses`

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `GET` | `/` | `PARTNER/AGENCY` | Liste paginée (filtres : catégorie, bien, période) |
| `GET` | `/{id}` | `PARTNER/AGENCY` | Détail d'une dépense |
| `POST` | `/` | `PARTNER/AGENCY` | Enregistrer une dépense |
| `DELETE` | `/{id}` | `PARTNER/AGENCY` | Supprimer une dépense |

### Agency Team — `/v1/agency/team`

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `GET` | `/` | `PARTNER` | Lister les membres de l'équipe |
| `POST` | `/` | `PARTNER` + `DIRECTEUR_AGENCE` | Rattacher un utilisateur existant |
| `PUT` | `/{userId}/role` | `PARTNER` + `DIRECTEUR_AGENCE` | Modifier le rôle interne |
| `DELETE` | `/{userId}` | `PARTNER` + `DIRECTEUR_AGENCE` | Retirer un membre |

### Tenant — `/v1/tenants`

| Méthode | Chemin | Auth | Description |
|---------|--------|------|-------------|
| `POST` | `/profile` | `CLIENT` | Soumettre mon dossier locataire |
| `GET` | `/profile` | Authentifié | Consulter mon dossier |
| `PATCH` | `/profile` | Authentifié | Mettre à jour mon dossier |
| `GET` | `/` | `ADMIN/AGENCY` | Liste paginée des dossiers |
| `GET` | `/{id}` | `ADMIN/AGENCY` | Détail d'un dossier |
| `PATCH` | `/{id}/qualify` | `ADMIN/AGENCY` | Qualifier un dossier |
| `PATCH` | `/{id}/reject` | `ADMIN/AGENCY` | Rejeter un dossier |
| `DELETE` | `/{id}` | `ADMIN` | Archiver un dossier |

---

## Base de données

- **Schéma** : `administrative`
- **Migrations** : Flyway (`src/main/resources/db/migration/V*__*.sql`)
- `ddl-auto: none` — Flyway est la seule source de vérité

| Version | Description |
|---------|-------------|
| V001 | Création du schéma `administrative` |
| V002 | Table `users` + `user_roles` |
| V003 | Table `otp_verifications` |
| V004 | Table `partner_applications` |
| V005 | Table `application_status_logs` |
| V006 | Colonne `storage_slug` sur `partner_applications` |
| V007 | Table `la_code_list` |
| V008 | Table `agencies` |
| V009 | Colonne `agency_id` sur `users` |
| V010 | Table `tenants` |
| V011 | Table `contracts` |
| V012 | Suppression colonne rôle obsolète sur `users` |
| V013 | Table `properties` + `property_media` + `property_documents` |
| V014 | Table `payments` |
| V015 | Table `expenses` |
| V016 | Colonne `partner_role` sur `users` |

---

## Buckets MinIO

| Bucket | Usage |
|--------|-------|
| `users-avatars` | Avatars utilisateurs |
| `agencies-logos` | Logos des agences |
| `properties-media` | Photos / vidéos des biens |
| `property-documents` | Documents légaux des biens |
| `tenant-documents` | Pièces KYC des locataires |
| `documents-generated` | Contrats / factures / reçus PDF générés |
| `ticket-attachments` | Photos incidents et rapports d'intervention |
| `partner-documents` | RCCM, DFE, bail des candidatures partenaires |

---

## Tests

```bash
./mvnw test                   # tous les tests
./mvnw test -Dtest=*IT        # intégration uniquement (Testcontainers)
```

---

## Observabilité

- Health : `GET /actuator/health`
- Métriques Prometheus : `GET /actuator/prometheus`
- Grafana : `http://localhost:3000`
