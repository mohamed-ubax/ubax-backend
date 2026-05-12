package com.africa.ubaxplatform.bailleur.controller;

import com.africa.ubaxplatform.auth.codeList.AgenceRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.repository.UserSubRoleRepository;
import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.africa.ubaxplatform.bailleur.dto.BailleurAgencyResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplicationResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplyRequest;
import com.africa.ubaxplatform.bailleur.dto.BailleurDecisionRequest;
import com.africa.ubaxplatform.bailleur.service.interfaces.BailleurService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur de gestion des bailleurs et de leurs demandes d'adhésion à une agence.
 *
 * <p><b>Flux mobile CLIENT → OWNER :</b>
 *
 * <ol>
 *   <li>CLIENT browse les agences : {@code GET /v1/agencies}
 *   <li>CLIENT soumet une demande : {@code POST /v1/bailleur/apply} (JWT CLIENT requis)
 *   <li>DIRECTEUR_AGENCE approuve : {@code PATCH /v1/bailleur/agency/applications/{id}/decision}
 *   <li>Approbation → rôle {@code UBAX_OWNER} ajouté au compte Keycloak + lien agence créé
 *   <li>OWNER rafraîchit son token → formulaire de saisie de bien débloqué
 * </ol>
 *
 * <p>Endpoints mobile OWNER :
 *
 * <ul>
 *   <li>{@code GET /v1/bailleur/my-applications} – suivi statut de la demande
 *   <li>{@code GET /v1/bailleur/my-agencies} – agences liées (pour rattacher un bien)
 * </ul>
 *
 * <p>Endpoints agence ({@code UBAX_PARTNER} + sous-rôle {@code DIRECTEUR_AGENCE}) :
 *
 * <ul>
 *   <li>{@code GET /v1/bailleur/agency/applications} – liste paginée des demandes reçues
 *   <li>{@code GET /v1/bailleur/agency/applications/{id}} – détail d'une demande
 *   <li>{@code PATCH /v1/bailleur/agency/applications/{id}/decision} – approuver / rejeter
 * </ul>
 *
 * <p>Endpoints admin ({@code UBAX_ADMIN}) :
 *
 * <ul>
 *   <li>{@code GET /v1/bailleur/admin/applications} – vue globale toutes agences confondues
 * </ul>
 */
@RestController
@RequestMapping("/v1/bailleur")
@RequiredArgsConstructor
@Tag(
    name = "Bailleur",
    description =
        "Gestion des demandes d'adhésion bailleur à une agence. "
            + "Les endpoints `/my-applications`, `/my-agencies` et `/apply` sont utilisés par l'application mobile.")
@Slf4j
public class BailleurController {

  private final BailleurService bailleurService;
  private final RequestHeaderParser requestHeaderParser;
  private final UserRepository userRepository;
  private final UserSubRoleRepository subRoleRepo;

  // ── Public ─────────────────────────────────────────────────────

  /** Soumission d'une demande d'adhésion bailleur (CLIENT authentifié). */
  @PostMapping("/apply")
  @Operation(
      summary = "Soumettre une demande d'adhésion bailleur",
      description =
          "📱 **Mobile** · 🛡 **Rôle requis :** `CLIENT`\n\n"
              + "Soumet une demande d'adhésion à une agence UBAX depuis le compte CLIENT connecté.\n\n"
              + "Les informations d'identité (nom, prénom, téléphone, email) sont automatiquement extraites du compte CLIENT — "
              + "seuls l'agence cible, la pièce d'identité et la liste des biens sont requis dans le corps.\n\n"
              + "**Vérifications automatiques :**\n"
              + "- Contrôle que les biens du bailleur ne sont pas déjà gérés par une autre agence\n"
              + "- Vérification géographique par Haversine (rayon configurable via `GEO_CONFLICT_RADIUS_METERS`)\n"
              + "- Biens sans coordonnées : `conflictDetected = true` → revue manuelle obligatoire\n\n"
              + "La demande est créée avec le statut `PENDING` en attente de décision du `DIRECTEUR_AGENCE`.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Demande soumise avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurApplicationResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – CLIENT requis",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Agence introuvable", content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "Conflit détecté – un bien de ce bailleur est déjà géré par une autre agence",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> apply(
      @RequestBody @Valid BailleurApplyRequest request, HttpServletRequest httpRequest)
      throws CustomException {

    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT);
    BailleurApplicationResponse response = bailleurService.apply(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.BAILLEUR_APPLICATION_SUBMIT_SUCCESS,
                response));
  }

  // ── Espace agence ──────────────────────────────────────────────

  /** Liste paginée des demandes reçues par l'agence du DIRECTEUR_AGENCE connecté. */
  @GetMapping("/agency/applications")
  @Operation(
      summary = "Lister les demandes bailleur reçues par l'agence",
      description =
          "🛡 **Rôles requis :** `PARTNER` + sous-rôle `DIRECTEUR_AGENCE`\n\n"
              + "Retourne la liste paginée des demandes d'adhésion bailleur adressées à l'agence de l'utilisateur connecté.\n\n"
              + "Les demandes avec `conflictDetected = true` nécessitent une vérification manuelle avant approbation.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée des demandes",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurApplicationResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – PARTNER + DIRECTEUR_AGENCE requis",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> listByAgency(
      @ParameterObject
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    var dbUser =
        userRepository
            .findByKeycloakId(caller.getSub())
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));
    RoleGuard.checkAgenceRole(dbUser, subRoleRepo, AgenceRole.DIRECTEUR_AGENCE);

    UUID agencyId = dbUser.getAgency().getId();
    var result = bailleurService.listByAgency(agencyId, pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.BAILLEUR_APPLICATION_GET_LIST_SUCCESS,
            result));
  }

  /** Détail d'une demande bailleur avec la liste des biens décrits. */
  @GetMapping("/agency/applications/{id}")
  @Operation(
      summary = "Détail d'une demande bailleur",
      description =
          "🛡 **Rôles requis :** `PARTNER` + sous-rôle `DIRECTEUR_AGENCE`\n\n"
              + "Retourne le détail complet de la demande, incluant les biens décrits et le flag de conflit géographique.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail de la demande",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurApplicationResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – PARTNER + DIRECTEUR_AGENCE requis",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Demande introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getById(
      @Parameter(description = "Identifiant UUID de la demande", required = true) @PathVariable
          UUID id,
      HttpServletRequest httpRequest)
      throws CustomException {

    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    var dbUser =
        userRepository
            .findByKeycloakId(caller.getSub())
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));
    RoleGuard.checkAgenceRole(dbUser, subRoleRepo, AgenceRole.DIRECTEUR_AGENCE);

    BailleurApplicationResponse response = bailleurService.getById(id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.BAILLEUR_APPLICATION_GET_SUCCESS,
            response));
  }

  /** Approuve ou rejette une demande bailleur en attente. */
  @PatchMapping("/agency/applications/{id}/decision")
  @Operation(
      summary = "Statuer sur une demande bailleur",
      description =
          "🛡 **Rôles requis :** `PARTNER` + sous-rôle `DIRECTEUR_AGENCE`\n\n"
              + "Applique une décision sur une demande en statut `PENDING`.\n\n"
              + "**Si `decision = APPROVE` :**\n"
              + "- Si le bailleur est **nouveau** : création du compte Keycloak (username = téléphone) + rôle `UBAX_OWNER` + envoi SMS avec mot de passe temporaire\n"
              + "- Si le bailleur **existe déjà** : lien bailleur ↔ agence créé sans recréer de compte\n\n"
              + "**Si `decision = REJECT` :** `comment` recommandé pour informer le bailleur.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Décision appliquée",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurApplicationResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Corps de requête invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – PARTNER + DIRECTEUR_AGENCE requis",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Demande introuvable", content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "Demande déjà traitée (statut != PENDING)",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> processDecision(
      @Parameter(description = "Identifiant UUID de la demande", required = true) @PathVariable
          UUID id,
      @RequestBody @Valid BailleurDecisionRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    var dbUser =
        userRepository
            .findByKeycloakId(caller.getSub())
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));
    RoleGuard.checkAgenceRole(dbUser, subRoleRepo, AgenceRole.DIRECTEUR_AGENCE);

    BailleurApplicationResponse response =
        bailleurService.processDecision(id, request, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.BAILLEUR_APPLICATION_DECISION_SUCCESS,
            response));
  }

  // ── Espace bailleur (OWNER) ────────────────────────────────────

  /** Mes demandes d'adhésion soumises (vue OWNER). */
  @GetMapping("/my-applications")
  @Operation(
      summary = "Mes demandes d'adhésion bailleur",
      description =
          "📱 **Mobile** · 🛡 **Rôle requis :** `OWNER`\n\n"
              + "Retourne la liste paginée des demandes soumises par le bailleur connecté (filtrage par email du JWT).\n\n"
              + "Filtrable par `status` : `PENDING` | `APPROVED` | `REJECTED` | `CANCELLED`.\n\n"
              + "Utiliser ce endpoint pour afficher le statut de la demande en attente dans l'app mobile.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée des demandes",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurApplicationResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – OWNER requis",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> getMyApplications(
      @Parameter(description = "Filtrer par statut (optionnel)") @RequestParam(required = false)
          BailleurApplicationStatus status,
      @ParameterObject
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.OWNER);

    var result = bailleurService.getMyApplications(caller.getSub(), status, pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.BAILLEUR_MY_APPLICATIONS_SUCCESS,
            result));
  }

  /** Mes agences partenaires approuvées (vue OWNER). */
  @GetMapping("/my-agencies")
  @Operation(
      summary = "Mes agences partenaires",
      description =
          "📱 **Mobile** · 🛡 **Rôle requis :** `OWNER`\n\n"
              + "Retourne la liste des agences auxquelles le bailleur connecté est lié (demandes approuvées).\n\n"
              + "L'approbation d'une demande déclenche automatiquement l'ajout du rôle `UBAX_OWNER` sur le compte "
              + "et la création du lien agence. Ce endpoint permet à l'app mobile de déterminer à quelle agence "
              + "rattacher le formulaire de saisie de bien.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des agences partenaires",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurAgencyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – OWNER requis",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> getMyAgencies(HttpServletRequest httpRequest)
      throws CustomException {

    RequestUser caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.OWNER);

    List<BailleurAgencyResponse> result = bailleurService.getMyAgencies(caller.getSub());

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.BAILLEUR_MY_AGENCIES_SUCCESS,
            result));
  }

  // ── Espace admin ──────────────────────────────────────────────

  /** Vue globale de toutes les demandes bailleur, toutes agences confondues. */
  @GetMapping("/admin/applications")
  @Operation(
      summary = "Vue globale des demandes bailleur (admin)",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Liste paginée de toutes les demandes bailleur, toutes agences confondues.",
      tags = {"Bailleur"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée des demandes",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BailleurApplicationResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> listAll(
      @ParameterObject
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    var result = bailleurService.listAll(pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.BAILLEUR_APPLICATION_GET_LIST_SUCCESS,
            result));
  }
}
