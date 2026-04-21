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
 * <h2>Convention de lecture des accès</h2>
 *
 * <ul>
 *   <li>🌐 <b>Public</b> – aucune authentification requise
 *   <li>🔑 <b>Authentifié</b> – tout utilisateur avec un JWT valide
 *   <li>🏢 <b>Partenaire</b> – rôle Keycloak {@code PARTNER} (agence ou hôtel)
 *   <li>🛡 <b>Admin</b> – rôles Keycloak {@code ADMIN | SUPER_ADMIN}
 *   <li>👑 <b>Super Admin</b> – rôle Keycloak {@code SUPER_ADMIN} uniquement
 * </ul>
 *
 * <h2>Système de rôles à deux niveaux</h2>
 *
 * <p><b>Niveau 1 — Rôles Keycloak (JWT)</b> : 5 rôles realm préfixés {@code UBAX_} : {@code
 * SUPER_ADMIN, ADMIN, PARTNER, OWNER, CLIENT}
 *
 * <p><b>Niveau 2 — Sous-rôles applicatifs (DB {@code user_sub_roles})</b> :
 *
 * <ul>
 *   <li>{@code scope=UBAX_INTERNAL} → {@code UbaxAdminRole} : DIRECTEUR_GENERAL · SUPPORT_CLIENT ·
 *       OPERATIONS · FINANCE · COMMERCIAL
 *   <li>{@code scope=AGENCE} → {@code AgenceRole} : DIRECTEUR_AGENCE · COMMERCIAL ·
 *       COMPTABLE_AGENCE · AGENT_SAV
 *   <li>{@code scope=HOTEL} → {@code HotelRole} : GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL ·
 *       RESPONSABLE_HEBERGEMENT
 * </ul>
 *
 * <h2>Format de réponse standard</h2>
 *
 * <p>Toutes les réponses sont enveloppées dans {@code CustomResponse} :
 *
 * <pre>
 * {
 *   "status":     "SUCCESS | BAD_REQUEST | UNAUTHORIZED | NOT_FOUND | CONFLICT | INTERNAL_SERVER_ERROR",
 *   "statusCode": 200 | 201 | 400 | 401 | 403 | 404 | 409 | 500,
 *   "message":    "DOMAIN_OPERATION_SUCCESS | DOMAIN_OPERATION_FAILURE",
 *   "data":       &lt;DTO spécifique ou page paginée&gt;
 * }
 * </pre>
 *
 * <p>Pour les listes paginées, {@code data} contient :
 *
 * <pre>
 * {
 *   "results":     [...],
 *   "total-items": 42,
 *   "total-pages": 3,
 *   "page":        0,
 *   "perPage":     20,
 *   "isFirst":     true,
 *   "isLast":      false
 * }
 * </pre>
 */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "UBAX Platform API",
            version = "1.0",
            description =
                "API REST de la plateforme immobilière **UBAX** (Afrique).\n\n"
                    + "## Authentification\n"
                    + "Toutes les routes protégées nécessitent un **Bearer JWT** obtenu via "
                    + "`POST /v1/auth/login`. Le token est émis par **Keycloak** (realm `ubax-plateform`).\n\n"
                    + "## Rôles Keycloak (Niveau 1)\n"
                    + "| Rôle realm Keycloak | Enum `UserRole` | Accès |\n"
                    + "|---|---|---|\n"
                    + "| `UBAX_SUPER_ADMIN` | `SUPER_ADMIN` | Accès total — configuration, gestion admins |\n"
                    + "| `UBAX_ADMIN` | `ADMIN` | Opérations courantes back-office UBAX |\n"
                    + "| `UBAX_PARTNER` | `PARTNER` | Partenaire agence **ou** hôtel |\n"
                    + "| `UBAX_OWNER` | `OWNER` | Propriétaire particulier |\n"
                    + "| `UBAX_CLIENT` | `CLIENT` | Acheteur / locataire |\n\n"
                    + "## Sous-rôles applicatifs (Niveau 2 — table `user_sub_roles`)\n"
                    + "| Scope | Sous-rôles autorisés | Pour qui |\n"
                    + "|---|---|---|\n"
                    + "| `UBAX_INTERNAL` | DIRECTEUR_GENERAL · SUPPORT_CLIENT · OPERATIONS · FINANCE · COMMERCIAL | ADMIN / SUPER_ADMIN |\n"
                    + "| `AGENCE` | DIRECTEUR_AGENCE · COMMERCIAL · COMPTABLE_AGENCE · AGENT_SAV | PARTNER agence |\n"
                    + "| `HOTEL` | GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL · RESPONSABLE_HEBERGEMENT | PARTNER hôtel |",
            contact = @Contact(name = "UBAX Platform", email = "tech@ubax.africa")),
    tags = {
      @Tag(
          name = "Authentication",
          description = "🌐 **Public** – Connexion email, déconnexion, mot de passe oublié"),
      @Tag(
          name = "Mobile",
          description = "🌐 **Public** – Inscription et connexion via téléphone (OTP SMS)"),
      @Tag(
          name = "Administration Plateforme",
          description =
              "👑 **SUPER_ADMIN** · 🛡 **ADMIN** – Gestion des comptes administrateurs internes UBAX"),
      @Tag(
          name = "User Sub-Roles",
          description =
              "👑 **SUPER_ADMIN / ADMIN** – Gestion des sous-rôles applicatifs (table `user_sub_roles`).\n\n"
                  + "Ces sous-rôles affinent les accès à l'intérieur d'un rôle Keycloak et ne sont **pas portés par le JWT**.\n\n"
                  + "**Scopes :** `UBAX_INTERNAL` (admins) · `AGENCE` (partenaires agence) · `HOTEL` (partenaires hôtel)\n\n"
                  + "**UBAX_INTERNAL :** DIRECTEUR_GENERAL · SUPPORT_CLIENT · OPERATIONS · FINANCE · COMMERCIAL\n\n"
                  + "**AGENCE :** DIRECTEUR_AGENCE · COMMERCIAL · COMPTABLE_AGENCE · AGENT_SAV\n\n"
                  + "**HOTEL :** GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL · RESPONSABLE_HEBERGEMENT"),
      @Tag(
          name = "Partner",
          description =
              "🌐 **Public** (soumission) · 🛡 **ADMIN** (décision) – Candidatures partenaires agences/hôtels"),
      @Tag(
          name = "Tenant",
          description =
              "🔑 **Authentifié** (profil) · 🏢 **PARTNER / ADMIN** (qualification) – Dossiers locataires KYC"),
      @Tag(
          name = "Property",
          description =
              "🌐 **Public** (liste, détail publiés) · 🏢 **PARTNER | OWNER** (CRUD biens) · 🛡 **ADMIN** (modération)\n\n"
                  + "**Workflow :** `DRAFT → PENDING → PUBLISHED → RESERVED → SOLD / ARCHIVED`"),
      @Tag(
          name = "Storage",
          description =
              "🔑 **Authentifié** – Upload de fichiers vers MinIO.\n\n"
                  + "**Deux stratégies :**\n"
                  + "1. `POST /upload` – multipart direct (mobile, ≤ 50 Mo)\n"
                  + "2. `GET /presign` – presigned URL PUT (web, gros fichiers, aucun transit backend)\n\n"
                  + "**Buckets :** `properties-media` · `property-documents` · `tenant-documents` "
                  + "· `agencies-logos` · `users-avatars` · `ticket-attachments` · `partner-documents`"),
      @Tag(
          name = "Payment",
          description =
              "🏢 **PARTNER · ADMIN · SUPER_ADMIN** – Gestion des paiements (loyers, cautions, commissions, ventes).\n\n"
                  + "**Statuts :** `PENDING → PAID | PARTIAL | LATE | CANCELLED`\n\n"
                  + "**Calcul automatique à la création :** `PAID` si `paidDate` + `amountPaid ≥ amount` · "
                  + "`PARTIAL` si paiement incomplet · `LATE` si échéance dépassée · sinon `PENDING`"),
      @Tag(
          name = "Expense",
          description =
              "🏢 **PARTNER · ADMIN · SUPER_ADMIN** – Dépenses comptables de l'agence.\n\n"
                  + "**Catégories :** `MAINTENANCE · MARKETING · SALARY · UTILITIES · TAX · OTHER`\n\n"
                  + "**Centre de coût :** `AGENCY_GENERAL` (agence entière) · `PROPERTY_SPECIFIC` (bien précis — `propertyId` obligatoire)"),
      @Tag(
          name = "Dashboard",
          description =
              "🏢 **PARTNER · ADMIN · SUPER_ADMIN** – Tableau de bord analytique de l'espace agence.\n\n"
                  + "**KPIs financiers :** `totalRevenue` · `totalExpenses` · `netRevenue` · `overdueAmount` · `recoveryRate`\n\n"
                  + "**KPIs portfolio :** `totalProperties` · `publishedProperties` · `reservedProperties` · "
                  + "`activeContracts` · `pendingPaymentsCount` · `latePaymentsCount` · `paidPaymentsCount`\n\n"
                  + "**Décompositions :** `revenueByType` (par type de paiement) · `expensesByCategory` (par catégorie de dépense)\n\n"
                  + "**Accès par sous-rôle agence :** DIRECTEUR_AGENCE (vision globale) · COMMERCIAL (prospects/biens) · "
                  + "COMPTABLE_AGENCE (finances, solde masqué pour les autres) · AGENT_SAV (tickets/interventions)"),
      @Tag(
          name = "CodeList",
          description =
              "🌐 **Public** (lecture) · 🛡 **ADMIN** (écriture) – Référentiels dynamiques (types, statuts, etc.)")
    })
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT obtenu via `POST /v1/auth/login`. Format : `Authorization: Bearer <token>`")
public class OpenApiConfig {}
