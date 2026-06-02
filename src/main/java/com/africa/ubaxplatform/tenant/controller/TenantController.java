package com.africa.ubaxplatform.tenant.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.dto.TenantCreateRequest;
import com.africa.ubaxplatform.tenant.dto.TenantResponse;
import com.africa.ubaxplatform.tenant.dto.TenantUpdateRequest;
import com.africa.ubaxplatform.tenant.service.interfaces.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur de gestion des dossiers locataires.
 *
 * <p>Endpoints client (rôle {@code CLIENT} requis) :
 *
 * <ul>
 *   <li>{@code POST /v1/tenants/profile} – soumettre son dossier
 *   <li>{@code GET /v1/tenants/profile} – consulter son dossier
 *   <li>{@code PATCH /v1/tenants/profile} – mettre à jour son dossier
 * </ul>
 *
 * <p>Endpoints admin/agent ({@code ADMIN}, {@code SUPER_ADMIN} ou {@code AGENCY} requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/tenants} – liste paginée avec filtre par statut
 *   <li>{@code GET /v1/tenants/{id}} – détail d'un dossier
 *   <li>{@code PATCH /v1/tenants/{id}/qualify} – qualifier un dossier
 *   <li>{@code PATCH /v1/tenants/{id}/reject} – rejeter un dossier
 *   <li>{@code DELETE /v1/tenants/{id}} – suppression logique
 * </ul>
 */
@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(
    name = "Tenant",
    description =
        "Gestion des dossiers locataires (KYC, qualification, cycle de vie). "
            + "Accès client sur `/profile`, accès admin/agent sur les autres endpoints.")
public class TenantController {

  private final TenantService tenantService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Client : gestion de mon dossier ───────────────────────────

  @PostMapping("/profile")
  @Operation(
      summary = "Soumettre un dossier locataire",
      description =
          "Crée le dossier locataire (KYC) de l'utilisateur connecté pour un bien ciblé.\n\n"
              + "**`propertyId` est obligatoire** — le dossier est rattaché au bien immobilier visé dès la création. "
              + "Un seul dossier par client (unicité sur `userId`). "
              + "Pour cibler un autre bien ultérieurement, utiliser `PATCH /profile` avec `propertyId`.\n\n"
              + "Le statut initial est **INCOMPLETE**. Il passe automatiquement en **PENDING_REVIEW** "
              + "dès que `idDocumentUrl` + `idDocumentNumber` + `idDocumentExpiry` sont renseignés.\n\n"
              + "**Rôles autorisés :** `CLIENT`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Dossier créé avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TenantResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Données invalides ou `propertyId` manquant",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token JWT absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — CLIENT requis",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Bien (`propertyId`) introuvable",
        content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "Un dossier locataire existe déjà pour cet utilisateur",
        content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TenantCreateRequest.class)))
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid TenantCreateRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT);
    TenantResponse response = tenantService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.TENANT_CREATE_SUCCESS,
                response));
  }

  @GetMapping("/profile")
  @Operation(
      summary = "Consulter mon dossier locataire",
      description =
          "Retourne le dossier locataire de l'utilisateur connecté, incluant le statut de qualification et les URLs des documents KYC.\n\n"
              + "**Rôles autorisés :** tout utilisateur authentifié",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier retourné avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "404",
        description = "Aucun dossier locataire trouvé pour cet utilisateur")
  })
  public ResponseEntity<CustomResponse> getMyProfile(HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller = RoleGuard.requireAuthenticated(requestHeaderParser, httpRequest);
    TenantResponse response = tenantService.getMyProfile(caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_GET_SUCCESS,
            response));
  }

  @PatchMapping("/profile")
  @Operation(
      summary = "Mettre à jour mon dossier locataire",
      description =
          "Mise à jour partielle du dossier locataire (seuls les champs non-null sont modifiés).\n\n"
              + "**Cas d'usage principaux :**\n"
              + "- Renseigner les URLs des documents KYC après upload MinIO (bucket `tenant-documents`)\n"
              + "- **Changer le bien ciblé** : passer `propertyId` pour pointer vers un autre bien "
              + "(le client n'a qu'un seul dossier réutilisable — inutile d'en recréer un)\n\n"
              + "Le statut passe automatiquement de **INCOMPLETE** ou **REJECTED** à **PENDING_REVIEW** "
              + "dès que `idDocumentUrl` + `idDocumentNumber` + `idDocumentExpiry` sont tous renseignés.\n\n"
              + "**Rôles autorisés :** tout utilisateur authentifié",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier mis à jour avec succès"),
    @ApiResponse(responseCode = "400", description = "Données invalides"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "404",
        description = "Dossier introuvable ou bien (`propertyId`) introuvable")
  })
  public ResponseEntity<CustomResponse> updateMyProfile(
      @RequestBody @Valid TenantUpdateRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller = RoleGuard.requireAuthenticated(requestHeaderParser, httpRequest);
    TenantResponse response = tenantService.updateMyProfile(caller.getSub(), request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_UPDATE_SUCCESS,
            response));
  }

  // ── Admin / Agent : gestion des dossiers ──────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les dossiers locataires",
      description =
          "Retourne une page de dossiers locataires, filtrables par statut, bien et/ou présence de contrat. "
              + "Pagination par défaut : 20 éléments triés par `createdAt` décroissant.\n\n"
              + "**Filtres disponibles :**\n"
              + "- `status` — filtre par statut (`INCOMPLETE`, `PENDING_REVIEW`, `QUALIFIED`, `REJECTED`, `BLACKLISTED`)\n"
              + "- `propertyId` — filtre par bien ciblé (lien direct `property_id` sur le dossier)\n"
              + "- `withoutContract` — si `true` + `propertyId` fourni : retourne **uniquement** les dossiers "
              + "sans aucun contrat pour ce bien (candidatures en attente de décision agence)\n\n"
              + "**Cas d'usage typique :**\n"
              + "`GET /v1/tenants?propertyId=xxx&withoutContract=true` → tous les candidats à statuer pour le bien `xxx`\n\n"
              + "**Comportement par rôle :**\n"
              + "- `ADMIN` / `SUPER_ADMIN` : tous les dossiers non archivés\n"
              + "- `PARTNER` agence : uniquement les dossiers dont le bien ciblé appartient à son agence\n"
              + "- `PARTNER` hôtel : accès refusé (403)\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`, `PARTNER` (agence uniquement)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — ADMIN, SUPER_ADMIN ou AGENCY requis")
  })
  public ResponseEntity<CustomResponse> list(
      @Parameter(
              description = "Filtre par statut du dossier. Omis = tous les statuts.",
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {
                        "INCOMPLETE",
                        "PENDING_REVIEW",
                        "QUALIFIED",
                        "REJECTED",
                        "BLACKLISTED"
                      }))
          @RequestParam(required = false)
          TenantStatus status,
      @Parameter(description = "Filtre par bien immobilier (UUID). Omis = tous les biens.")
          @RequestParam(required = false)
          UUID propertyId,
      @Parameter(
              description =
                  "Si `true` et `propertyId` fourni : retourne uniquement les dossiers sans contrat pour ce bien.")
          @RequestParam(required = false)
          Boolean withoutContract,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.PARTNER);
    Page<TenantResponse> page =
        tenantService.list(caller.getSub(), status, propertyId, withoutContract, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_GET_LIST_SUCCESS,
            page));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'un dossier locataire",
      description =
          "Retourne le dossier locataire complet identifié par son UUID, incluant tous les champs KYC, le statut et l'historique de qualification.\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`, `AGENCY`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier retourné avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — ADMIN, SUPER_ADMIN ou AGENCY requis"),
    @ApiResponse(responseCode = "404", description = "Dossier locataire introuvable")
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.PARTNER);
    TenantResponse response = tenantService.getById(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_GET_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/qualify")
  @Operation(
      summary = "Qualifier un dossier locataire",
      description =
          "Valide le dossier locataire : passe le statut en **QUALIFIED** et positionne `isQualified = true`. "
              + "Prérequis : le dossier doit être en statut **PENDING_REVIEW** (dossier complet soumis). "
              + "Cette validation est nécessaire pour déclencher la génération et la signature d'un bail.\n\n"
              + "**Contrainte PARTNER :** un partenaire agence ne peut qualifier que les dossiers liés à un contrat de **son agence** (403 sinon).\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`, `PARTNER` (agence — dossiers de son agence uniquement)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier qualifié avec succès"),
    @ApiResponse(
        responseCode = "400",
        description = "Le dossier n'est pas en statut PENDING_REVIEW"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — ADMIN, SUPER_ADMIN ou AGENCY requis"),
    @ApiResponse(responseCode = "404", description = "Dossier locataire introuvable")
  })
  public ResponseEntity<CustomResponse> qualify(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.PARTNER);
    TenantResponse response = tenantService.qualify(id, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/reject")
  @Operation(
      summary = "Rejeter un dossier locataire",
      description =
          "Refuse le dossier locataire et enregistre le motif de refus communiqué au locataire. "
              + "Le statut passe en **REJECTED** et `isQualified` est remis à `false`. "
              + "Le motif (`reason`) est obligatoire.\n\n"
              + "**Contrainte PARTNER :** un partenaire agence ne peut rejeter que les dossiers liés à un contrat de **son agence** (403 sinon).\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`, `PARTNER` (agence — dossiers de son agence uniquement)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier rejeté avec succès"),
    @ApiResponse(responseCode = "400", description = "Motif de refus manquant ou invalide"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — ADMIN, SUPER_ADMIN ou AGENCY requis"),
    @ApiResponse(responseCode = "404", description = "Dossier locataire introuvable")
  })
  public ResponseEntity<CustomResponse> reject(
      @PathVariable UUID id,
      @Parameter(
              description = "Motif du refus communiqué au locataire (obligatoire)",
              required = true)
          @RequestParam
          String reason,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.PARTNER);
    TenantResponse response = tenantService.reject(id, reason, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_UPDATE_SUCCESS,
            response));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Archiver un dossier locataire (soft delete)",
      description =
          "Suppression logique : positionne `deletedAt` à l'horodatage courant sans effacer l'enregistrement. "
              + "Un dossier archivé n'est plus visible dans les listes et ne peut plus être modifié, "
              + "mais son historique de contrats associés est conservé en base.\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier archivé avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — ADMIN ou SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Dossier locataire introuvable")
  })
  public ResponseEntity<CustomResponse> delete(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    tenantService.delete(id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_DELETE_SUCCESS,
            null));
  }

  @GetMapping("/archived")
  @Operation(
      summary = "Lister les dossiers locataires archivés",
      description =
          "Retourne les dossiers supprimés logiquement (deletedAt IS NOT NULL).\n\n"
              + "- **ADMIN / SUPER_ADMIN** : tous les dossiers archivés.\n"
              + "- **PARTNER agence** : uniquement les dossiers archivés liés à son agence.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste paginée des dossiers archivés"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  })
  public ResponseEntity<CustomResponse> listArchived(
      @PageableDefault(size = 20, sort = "deletedAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_GET_ARCHIVED_LIST_SUCCESS,
            tenantService.listArchived(caller.getSub(), pageable)));
  }

  @GetMapping("/archived/{id}")
  @Operation(
      summary = "Détail d'un dossier locataire archivé",
      description = "Retourne le détail complet d'un dossier archivé.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier archivé trouvé"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
    @ApiResponse(responseCode = "404", description = "Dossier archivé introuvable")
  })
  public ResponseEntity<CustomResponse> getArchivedById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_GET_ARCHIVED_SUCCESS,
            tenantService.getArchivedById(caller.getSub(), id)));
  }

  @PatchMapping("/{id}/restore")
  @Operation(
      summary = "Restaurer un dossier locataire archivé",
      description =
          "Remet `deletedAt` à null — le dossier redevient visible dans les listes actives.\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dossier restauré avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
    @ApiResponse(responseCode = "404", description = "Dossier archivé introuvable")
  })
  public ResponseEntity<CustomResponse> restore(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TENANT_RESTORE_SUCCESS,
            tenantService.restore(id)));
  }
}
