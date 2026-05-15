package com.africa.ubaxplatform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger UI de la plateforme UBAX.
 *
 * <p>Légende des accès :
 *
 * <ul>
 *   <li>🌐 Public – aucune authentification requise
 *   <li>🔑 Authentifié – JWT valide (tout rôle)
 *   <li>📱 Mobile – CLIENT connecté (app mobile)
 *   <li>🏢 PARTNER – agence ou hôtel
 *   <li>🛡 ADMIN – back-office UBAX
 *   <li>👑 SUPER_ADMIN – accès total
 * </ul>
 */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "UBAX Platform API",
            version = "1.0",
            description =
                "API REST de la plateforme immobilière et hôtelière **UBAX** (Afrique).\n\n"
                    + "## Authentification\n"
                    + "Toutes les routes protégées nécessitent un **Bearer JWT** obtenu via "
                    + "`POST /v1/auth/login`. Le token est émis par **Keycloak** "
                    + "(realm `ubax-plateform`).\n\n"
                    + "## Système de rôles à deux niveaux\n\n"
                    + "**Niveau 1 — Rôles Keycloak (portés par le JWT)**\n\n"
                    + "| Rôle realm | Enum Java | Description |\n"
                    + "|---|---|---|\n"
                    + "| `UBAX_SUPER_ADMIN` | `SUPER_ADMIN` | Accès total — config, admins |\n"
                    + "| `UBAX_ADMIN` | `ADMIN` | Opérations back-office UBAX |\n"
                    + "| `UBAX_PARTNER` | `PARTNER` | Agence ou hôtel partenaire |\n"
                    + "| `UBAX_OWNER` | `OWNER` | Propriétaire particulier |\n"
                    + "| `UBAX_CLIENT` | `CLIENT` | Acheteur / locataire |\n\n"
                    + "**Niveau 2 — Sous-rôles applicatifs (table `user_sub_roles`, hors JWT)**\n\n"
                    + "| Scope | Sous-rôles | Code list |\n"
                    + "|---|---|---|\n"
                    + "| `UBAX_INTERNAL` | DIRECTEUR_GENERAL · SUPPORT_CLIENT · OPERATIONS · FINANCE · COMMERCIAL | `GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL` |\n"
                    + "| `AGENCE` | DIRECTEUR_AGENCE · COMMERCIAL · COMPTABLE_AGENCE · AGENT_SAV | `GET /v1/code-list/type/ROLE_AGENCE` |\n"
                    + "| `HOTEL` | GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL · RESPONSABLE_HEBERGEMENT | `GET /v1/code-list/type/ROLE_HOTEL` |\n\n"
                    + "## Format de réponse standard\n\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"status\":     \"SUCCESS | BAD_REQUEST | UNAUTHORIZED | NOT_FOUND | CONFLICT\",\n"
                    + "  \"statusCode\": 200,\n"
                    + "  \"message\":    \"DOMAIN_OPERATION_SUCCESS\",\n"
                    + "  \"data\":       { ... }\n"
                    + "}\n"
                    + "```\n\n"
                    + "Pour les listes paginées, `data` contient : "
                    + "`results` · `total-items` · `total-pages` · `page` · `perPage` · `isFirst` · `isLast`",
            contact = @Contact(name = "UBAX Platform", email = "tech@ubax.africa")),
    tags = {

      // ── 1. AUTHENTIFICATION ──────────────────────────────────────────────────
      @Tag(
          name = "Authentification",
          description =
              "🌐 **Public** (login, OTP, mot de passe oublié) · 🛡 **ADMIN/SUPER_ADMIN** (reset forcé, gestion rôles)\n\n"
                  + "**Flux inscription mobile (3 étapes) :**\n"
                  + "1. `POST /v1/auth/register/send-otp` — envoi OTP SMS\n"
                  + "2. `POST /v1/auth/register/verify-otp` — vérification\n"
                  + "3. `POST /v1/auth/register/complete` — finalisation → compte CLIENT créé\n\n"
                  + "**Flux récupération mot de passe (3 étapes) :**\n"
                  + "1. `POST /v1/auth/forgot-password/send-otp`\n"
                  + "2. `POST /v1/auth/forgot-password/verify-otp`\n"
                  + "3. `POST /v1/auth/forgot-password/reset`"),

      // ── 2. PROFIL UTILISATEUR ────────────────────────────────────────────────
      @Tag(
          name = "Profil Utilisateur",
          description =
              "🔑 **Authentifié** — Consultation du profil et gestion de l'avatar.\n\n"
                  + "| Endpoint | Description |\n"
                  + "|---|---|\n"
                  + "| `GET /v1/users/keycloak/{keycloakId}` | Profil par Keycloak ID |\n"
                  + "| `GET /v1/users/{userId}` | Profil par ID interne |\n"
                  + "| `POST /v1/users/me/avatar` | Upload/remplacement avatar (multipart) |\n\n"
                  + "> L'avatar est stocké dans le bucket MinIO `users-avatars`."),

      // ── 3. ADMINISTRATION UBAX ───────────────────────────────────────────────
      @Tag(
          name = "Administration UBAX",
          description =
              "👑 **SUPER_ADMIN** (création, suppression, sous-rôles) · 🛡 **ADMIN** (lecture, modération)\n\n"
                  + "Regroupe : **Comptes admins** · **Sous-rôles UBAX_INTERNAL** · "
                  + "**Gestion partenaires** · **Clients** · **Membres structures partenaires**\n\n"
                  + "**Code list sous-rôles internes :** `GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL`\n"
                  + "→ DIRECTEUR_GENERAL · SUPPORT_CLIENT · OPERATIONS · FINANCE · COMMERCIAL\n\n"
                  + "**Abonnement partenaire :** `PATCH /v1/admin/partners/agencies/{id}/subscription`\n"
                  + "→ champs : `subscriptionPlan`, `subscriptionExpiresAt`"),

      // ── 4. CANDIDATURES PARTENAIRES ──────────────────────────────────────────
      @Tag(
          name = "Partner",
          description =
              "🌐 **Public** (soumission) · 🛡 **ADMIN** (traitement)\n\n"
                  + "**Workflow :** `PENDING → APPROVED | REJECTED`\n\n"
                  + "La soumission (`POST /v1/partner/apply`) est **multipart** et supporte :\n"
                  + "- Candidature **agence immobilière** ou **hôtel**\n"
                  + "- Géolocalisation (`latitude`, `longitude`) pour détection de conflit géographique\n"
                  + "- Documents partenaires stockés dans le bucket `partner-documents`\n\n"
                  + "À l'approbation, le compte PARTNER est créé automatiquement dans Keycloak."),

      // ── 5. AGENCES ───────────────────────────────────────────────────────────
      @Tag(
          name = "Agences",
          description =
              "🔑 **Authentifié** — Liste des agences immobilières actives.\n\n"
                  + "Utilisé principalement par les **clients mobiles** avant de soumettre une "
                  + "demande d'adhésion bailleur (`POST /v1/bailleur/apply`).\n\n"
                  + "Filtre disponible : `?city=Dakar`"),

      // ── 6. ÉQUIPE AGENCE ─────────────────────────────────────────────────────
      @Tag(
          name = "Agency Team",
          description =
              "🏢 **PARTNER** (même agence) — Gestion de l'équipe et des sous-rôles agence.\n\n"
                  + "**Code list sous-rôles :** `GET /v1/code-list/type/ROLE_AGENCE`\n\n"
                  + "| Sous-rôle | Tableau de bord | Accès |\n"
                  + "|---|---|---|\n"
                  + "| `DIRECTEUR_AGENCE` | DG | Accès complet + gestion équipe |\n"
                  + "| `COMMERCIAL` | Commercial | Prospects, rendez-vous, biens |\n"
                  + "| `COMPTABLE_AGENCE` | Comptable | Finances (solde masqué pour les autres) |\n"
                  + "| `AGENT_SAV` | SAV | Tickets et interventions |\n\n"
                  + "> Les sous-rôles sont cumulatifs. L'auto-assignation est autorisée."),

      // ── 7. BAILLEUR ──────────────────────────────────────────────────────────
      @Tag(
          name = "Bailleur",
          description =
              "📱 **CLIENT** (apply, suivi) · 🏢 **PARTNER + DIRECTEUR_AGENCE** (décision) · 🛡 **ADMIN** (vue globale)\n\n"
                  + "**Flux mobile complet :**\n"
                  + "1. `GET /v1/agencies` — choisir une agence\n"
                  + "2. `POST /v1/bailleur/apply` — soumettre la demande *(JWT CLIENT)*\n"
                  + "   → body : `agencyId`, `idType`, `idNumber`, `properties[]`\n"
                  + "   → `firstName`, `lastName`, `phone`, `email` extraits automatiquement du JWT\n"
                  + "3. L'agence approuve → rôle `UBAX_OWNER` ajouté → rafraîchir le token\n"
                  + "4. `GET /v1/bailleur/my-applications` — suivi du statut\n\n"
                  + "**Code list type pièce d'identité :** `GET /v1/code-list/type/ID_TYPE`\n"
                  + "→ CNI · PASSEPORT · PERMIS_CONDUIRE · TITRE_SEJOUR · CARTE_CONSULAIRE\n\n"
                  + "**Statuts :** `PENDING → APPROVED | REJECTED | CANCELLED`"),

      // ── 8. HÔTELS ────────────────────────────────────────────────────────────
      @Tag(
          name = "Hôtels",
          description =
              "🔑 **Authentifié** — Liste des hôtels partenaires actifs.\n\n"
                  + "Filtre disponible : `?city=Abidjan`"),

      // ── 9. ÉQUIPE HÔTEL ──────────────────────────────────────────────────────
      @Tag(
          name = "Hotel Team",
          description =
              "🏨 **PARTNER** (même hôtel) — Gestion de l'équipe et des sous-rôles hôtel.\n\n"
                  + "**Code list sous-rôles :** `GET /v1/code-list/type/ROLE_HOTEL`\n\n"
                  + "| Sous-rôle | Accès |\n"
                  + "|---|---|\n"
                  + "| `GERANT_HOTEL` | Accès complet hôtel + gestion équipe |\n"
                  + "| `RECEPTIONNISTE` | Réservations, check-in/out |\n"
                  + "| `COMPTABLE_HOTEL` | Facturation et revenus |\n"
                  + "| `RESPONSABLE_HEBERGEMENT` | Espaces, chambres, biens |\n\n"
                  + "> Les sous-rôles sont cumulatifs. L'auto-assignation est autorisée."),

      // ── 10. RÉSERVATIONS ─────────────────────────────────────────────────────
      @Tag(
          name = "Reservation",
          description =
              "📱 **CLIENT** (réserver, annuler) · 🏨 **PARTNER hôtel** (confirmer, clôturer) · 🛡 **ADMIN** (vue globale)\n\n"
                  + "**Workflow :**\n"
                  + "`PENDING` → `CONFIRMED` → `COMPLETED`\n"
                  + "`PENDING | CONFIRMED` → `CANCELLED`\n"
                  + "`CONFIRMED` → `NO_SHOW`\n\n"
                  + "Le CLIENT ne peut annuler que ses réservations en statut `PENDING`."),

      // ── 11. LOCATAIRES (KYC) ─────────────────────────────────────────────────
      @Tag(
          name = "Tenant",
          description =
              "📱 **CLIENT** (profil KYC) · 🏢 **PARTNER / ADMIN** (qualification)\n\n"
                  + "**Workflow :** `PENDING → QUALIFIED | REJECTED | ARCHIVED`\n\n"
                  + "Le dossier KYC est obligatoire pour signer un contrat de location.\n\n"
                  + "**Code list type pièce d'identité :** `GET /v1/code-list/type/ID_TYPE`\n"
                  + "→ CNI · PASSEPORT · PERMIS_CONDUIRE · TITRE_SEJOUR · CARTE_CONSULAIRE\n\n"
                  + "Documents KYC stockés dans le bucket `tenant-documents` "
                  + "(presigned URL : `GET /v1/storage/presign/tenant-document`)."),

      // ── 12. BIENS IMMOBILIERS ────────────────────────────────────────────────
      @Tag(
          name = "Property",
          description =
              "🌐 **Public** (liste publiée, détail) · 🏢 **PARTNER / OWNER** (CRUD) · 🛡 **ADMIN** (modération)\n\n"
                  + "**Workflow :** `DRAFT → PENDING → PUBLISHED → RESERVED → SOLD | ARCHIVED`\n\n"
                  + "**Code list équipements :** `GET /v1/code-list/type/PROPERTY_AMENITY`\n"
                  + "→ POOL · GENERATOR · WATER_TANK · AC · SECURITY · PARKING · "
                  + "ELEVATOR · GARDEN · FURNISHED · PETS\n\n"
                  + "**Médias :** upload direct `POST /v1/properties/{id}/media/upload` "
                  + "ou presigned URL `GET /v1/storage/presign/property-media`.\n"
                  + "Bucket : `properties-media` — documents légaux : `property-documents`.\n\n"
                  + "**`coverPhotoUrl`** exposé dans `PropertyResponse` — null si aucun média cover défini."),

      // ── 13. CONTRATS ─────────────────────────────────────────────────────────
      @Tag(
          name = "Contracts",
          description =
              "🏢 **PARTNER / OWNER / ADMIN** (création, modification) · 👑 **ADMIN** (activation)\n\n"
                  + "**Workflow :**\n"
                  + "`DRAFT` → `PENDING_SIGNATURE` *(submit — génère le PDF)* → `ACTIVE` *(activate — génère le 1er loyer)* "
                  + "→ `TERMINATED | CANCELLED`\n\n"
                  + "- Seuls les contrats en `DRAFT` peuvent être modifiés (`PUT /v1/contracts/{id}`)\n"
                  + "- L'activation (`PATCH /v1/contracts/{id}/activate`) est réservée à **ADMIN** — "
                  + "elle déclenche la génération du 1er paiement de type `LEASE`\n"
                  + "- Le PDF est généré via Thymeleaf et stocké dans le bucket `documents-generated`"),

      // ── 14. PAIEMENTS ────────────────────────────────────────────────────────
      @Tag(
          name = "Payment",
          description =
              "🏢 **PARTNER / ADMIN / SUPER_ADMIN** — Gestion des paiements (loyers, cautions, commissions).\n\n"
                  + "**Statuts :** `PENDING → PAID | PARTIAL | LATE | CANCELLED`\n\n"
                  + "Calcul automatique à la création :\n"
                  + "- `PAID` si `paidDate` présente et `amountPaid ≥ amount`\n"
                  + "- `PARTIAL` si paiement incomplet\n"
                  + "- `LATE` si échéance dépassée et non payé\n"
                  + "- `PENDING` sinon\n\n"
                  + "**Types de paiement (enum) :** LEASE · DEPOSIT · COMMISSION · SALE · OTHER\n\n"
                  + "**Méthodes (enum) :** CASH · BANK_TRANSFER · MOBILE_MONEY · CHECK · OTHER\n\n"
                  + "> Le scheduler `generateUpcomingRentPayments` génère automatiquement les loyers à venir chaque jour à 06h00."),

      // ── 15. DÉPENSES ─────────────────────────────────────────────────────────
      @Tag(
          name = "Expense",
          description =
              "🏢 **PARTNER / ADMIN / SUPER_ADMIN** — Dépenses comptables de l'agence.\n\n"
                  + "**Catégories (enum) :** MAINTENANCE · MARKETING · SALARY · UTILITIES · TAX · OTHER\n\n"
                  + "**Centre de coût (enum) :**\n"
                  + "- `AGENCY_GENERAL` — charge globale de l'agence\n"
                  + "- `PROPERTY_SPECIFIC` — imputable à un bien (`propertyId` obligatoire)"),

      // ── 16. TICKETS SAV ──────────────────────────────────────────────────────
      @Tag(
          name = "Ticketing",
          description =
              "📱 **CLIENT / OWNER / PARTNER** (créer) · 🏢 **PARTNER / ADMIN** (gérer) "
                  + "· 📱 **CLIENT / OWNER** (consulter les leurs uniquement)\n\n"
                  + "**Workflow :** `OPEN → IN_ANALYSIS → TECHNICIAN_SENT → RESOLVED → CLOSED | CANCELLED`\n\n"
                  + "**Code lists à appeler avant création :**\n"
                  + "- `GET /v1/code-list/type/TICKET_PRIORITY` → LOW · NORMAL · HIGH · URGENT\n"
                  + "- `GET /v1/code-list/type/TECHNICIEN_PROFESSION` → catégories d'incident :\n"
                  + "  PLOMBIER · ELECTRICIEN · SERRURIER · MENUISIER · MACON · PEINTRE · "
                  + "CLIMATISATION · VITRERIE · JARDINAGE · NETTOYAGE · DESINSECTISATION · AUTRE\n\n"
                  + "**Images à la création :** `POST /v1/tickets` accepte un champ `attachments[]` "
                  + "(URLs pré-uploadées via `GET /v1/storage/presign/ticket-attachment`). "
                  + "Bucket : `ticket-attachments`.\n\n"
                  + "**Code list types pièces jointes :** `GET /v1/code-list/type/TICKET_ATTACHMENT_TYPE`\n"
                  + "→ INCIDENT_PHOTO · INCIDENT_VIDEO · INTERVENTION_REPORT · INVOICE · OTHER\n\n"
                  + "**Accès CLIENT :** `GET /v1/tickets/mine` (tous ses tickets) + "
                  + "`GET /v1/tickets/{id}` (le sien uniquement — 403 si pas reporter)"),

      // ── 17. TECHNICIENS SAV ──────────────────────────────────────────────────
      @Tag(
          name = "Techniciens SAV",
          description =
              "🏢 **PARTNER** (agence ou hôtel) — Prestataires externes SAV.\n\n"
                  + "Les techniciens n'ont pas de compte sur la plateforme. "
                  + "Ils sont assignables aux tickets via `technicienId` dans `PATCH /v1/tickets/{id}/schedule`.\n\n"
                  + "**Code list professions :** `GET /v1/code-list/type/TECHNICIEN_PROFESSION`\n"
                  + "→ PLOMBIER · ELECTRICIEN · SERRURIER · MENUISIER · MACON · PEINTRE · "
                  + "CLIMATISATION · VITRERIE · JARDINAGE · NETTOYAGE · DESINSECTISATION · AUTRE\n\n"
                  + "Filtre disponibilité : `GET /v1/technicians?available=true`\n\n"
                  + "Un technicien appartient à une agence **ou** un hôtel (pas les deux). "
                  + "Suppression en soft delete (`deletedAt`)."),

      // ── 18. TABLEAU DE BORD ──────────────────────────────────────────────────
      @Tag(
          name = "Dashboard",
          description =
              "🏢 **PARTNER / ADMIN / SUPER_ADMIN** — KPIs analytiques.\n\n"
                  + "**Dashboard agence** (`GET /v1/dashboard/agency`) — adapté au sous-rôle du caller :\n\n"
                  + "| Sous-rôle | Vue |\n"
                  + "|---|---|\n"
                  + "| `DIRECTEUR_AGENCE` | Vision globale — tous les KPIs |\n"
                  + "| `COMMERCIAL` | Biens, prospects, contrats |\n"
                  + "| `COMPTABLE_AGENCE` | Finances détaillées (solde masqué pour les autres) |\n"
                  + "| `AGENT_SAV` | Tickets par statut, interventions |\n\n"
                  + "**Filtres optionnels :** `?startDate=2025-01-01&endDate=2025-12-31`\n\n"
                  + "**Dashboard admin global** (`GET /v1/dashboard/admin`) — "
                  + "ADMIN/SUPER_ADMIN uniquement."),

      // ── 19. STOCKAGE FICHIERS ────────────────────────────────────────────────
      @Tag(
          name = "Storage",
          description =
              "🔑 **Authentifié** — Upload vers MinIO.\n\n"
                  + "**Deux stratégies d'upload :**\n"
                  + "1. **Multipart direct** : `POST /v1/storage/upload` (mobile, ≤ 50 Mo)\n"
                  + "2. **Presigned URL PUT** : `GET /v1/storage/presign/*` → upload direct client→MinIO "
                  + "(gros fichiers, aucun transit backend)\n\n"
                  + "**Endpoints presign par contexte :**\n\n"
                  + "| Endpoint | Bucket | Usage |\n"
                  + "|---|---|---|\n"
                  + "| `GET /v1/storage/presign/property-media` | `properties-media` | Photos/vidéos biens |\n"
                  + "| `GET /v1/storage/presign/property-document` | `property-documents` | Documents légaux biens |\n"
                  + "| `GET /v1/storage/presign/tenant-document` | `tenant-documents` | KYC locataire |\n"
                  + "| `GET /v1/storage/presign/agency-logo` | `agencies-logos` | Logo agence |\n"
                  + "| `GET /v1/storage/presign/ticket-attachment` | `ticket-attachments` | Photos incidents SAV |\n"
                  + "| `GET /v1/storage/presign/read` | (privé) | Lecture URL presignée GET |\n\n"
                  + "> Après upload via presigned URL, passer l'URL au endpoint métier correspondant."),

      // ── 20. RÉFÉRENTIELS ─────────────────────────────────────────────────────
      @Tag(
          name = "CodeList",
          description =
              "🌐 **Public** (lecture par type) · 🛡 **ADMIN** (types restreints + écriture)\n\n"
                  + "**Tous les types disponibles :**\n\n"
                  + "| Type | Accès | Valeurs |\n"
                  + "|---|---|---|\n"
                  + "| `ROLE_AGENCE` | Public | DIRECTEUR_AGENCE · COMMERCIAL · COMPTABLE_AGENCE · AGENT_SAV |\n"
                  + "| `ROLE_HOTEL` | Public | GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL · RESPONSABLE_HEBERGEMENT |\n"
                  + "| `ROLE_UBAX_INTERNAL` | **ADMIN uniquement** | DIRECTEUR_GENERAL · SUPPORT_CLIENT · OPERATIONS · FINANCE · COMMERCIAL |\n"
                  + "| `ID_TYPE` | Public | CNI · PASSEPORT · PERMIS_CONDUIRE · TITRE_SEJOUR · CARTE_CONSULAIRE |\n"
                  + "| `PROPERTY_AMENITY` | Public | POOL · GENERATOR · WATER_TANK · AC · SECURITY · PARKING · ELEVATOR · GARDEN · FURNISHED · PETS |\n"
                  + "| `TECHNICIEN_PROFESSION` | Public | PLOMBIER · ELECTRICIEN · SERRURIER · MENUISIER · MACON · PEINTRE · CLIMATISATION · VITRERIE · JARDINAGE · NETTOYAGE · DESINSECTISATION · AUTRE |\n"
                  + "| `TICKET_PRIORITY` | Public | LOW · NORMAL · HIGH · URGENT |\n"
                  + "| `TICKET_ATTACHMENT_TYPE` | Public | INCIDENT_PHOTO · INCIDENT_VIDEO · INTERVENTION_REPORT · INVOICE · OTHER |\n\n"
                  + "> Pour `ROLE_UBAX_INTERNAL` : utiliser `GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL` (JWT ADMIN requis).")
    })
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT obtenu via `POST /v1/auth/login`. Format : `Authorization: Bearer <token>`")
public class OpenApiConfig {}
