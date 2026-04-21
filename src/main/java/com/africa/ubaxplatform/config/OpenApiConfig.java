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
 *   <li>🏢 <b>Partenaire</b> – rôles {@code AGENCY | OWNER | AGENT | PARTNER}
 *   <li>🛡 <b>Admin</b> – rôles {@code ADMIN | SUPER_ADMIN}
 *   <li>👑 <b>Super Admin</b> – rôle {@code SUPER_ADMIN} uniquement
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
                    + "## Rôles disponibles\n"
                    + "| Rôle Keycloak | Valeur enum | Accès |\n"
                    + "|---|---|---|\n"
                    + "| `UBAX_SUPER_ADMIN` | `SUPER_ADMIN` | Total – configuration, gestion admins |\n"
                    + "| `UBAX_ADMIN` | `ADMIN` | Opérations courantes plateforme |\n"
                    + "| `UBAX_PARTNER` | `PARTNER` | Espace partenaire |\n"
                    + "| `UBAX_AGENCY` | `AGENCY` | Agence immobilière |\n"
                    + "| `UBAX_OWNER` | `OWNER` | Propriétaire particulier |\n"
                    + "| `UBAX_AGENT` | `AGENT` | Agent rattaché à une agence |\n"
                    + "| `UBAX_CLIENT` | `CLIENT` | Acheteur / locataire |",
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
          name = "Partner",
          description =
              "🌐 **Public** (soumission) · 🛡 **ADMIN** (décision) – Candidatures partenaires agences/hôtels"),
      @Tag(
          name = "Tenant",
          description =
              "🔑 **Authentifié** (profil) · 🏢 **AGENCY / ADMIN** (qualification) – Dossiers locataires KYC"),
      @Tag(
          name = "Property",
          description =
              "🌐 **Public** (liste, détail publiés) · 🏢 **AGENCY | OWNER | AGENT | PARTNER** (CRUD biens) · 🛡 **ADMIN** (modération)\n\n"
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
              "🏢 **AGENCY · AGENT · PARTNER · ADMIN · SUPER_ADMIN** – Gestion des paiements (loyers, cautions, commissions, ventes).\n\n"
                  + "**Statuts :** `PENDING → PAID | PARTIAL | LATE | CANCELLED`\n\n"
                  + "**Calcul automatique à la création :** `PAID` si `paidDate` + `amountPaid ≥ amount` · "
                  + "`PARTIAL` si paiement incomplet · `LATE` si échéance dépassée · sinon `PENDING`"),
      @Tag(
          name = "Expense",
          description =
              "🏢 **AGENCY · AGENT · PARTNER · ADMIN · SUPER_ADMIN** – Dépenses comptables de l'agence.\n\n"
                  + "**Catégories :** `MAINTENANCE · MARKETING · SALARY · UTILITIES · TAX · OTHER`\n\n"
                  + "**Centre de coût :** `AGENCY_GENERAL` (agence entière) · `PROPERTY_SPECIFIC` (bien précis — `propertyId` obligatoire)"),
      @Tag(
          name = "Code List",
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
