package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AgencyTeamMemberResponse;
import com.africa.ubaxplatform.auth.dto.InviteTeamMemberRequest;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.auth.dto.UpdateTeamMemberRoleRequest;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.AgencyTeamService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur de gestion de l'équipe d'une agence partenaire.
 *
 * <p>Endpoints disponibles :
 *
 * <ul>
 *   <li>{@code GET /v1/agency/team} – lister les membres de l'équipe <br>
 *       Rôles : {@code PARTNER} (tout membre de l'agence)
 *   <li>{@code POST /v1/agency/team} – rattacher un utilisateur existant à l'agence <br>
 *       Rôles : {@code PARTNER} + rôle interne {@code DIRECTEUR_AGENCE}
 *   <li>{@code PUT /v1/agency/team/{userId}/role} – modifier le rôle d'un membre <br>
 *       Rôles : {@code PARTNER} + rôle interne {@code DIRECTEUR_AGENCE}
 *   <li>{@code DELETE /v1/agency/team/{userId}} – retirer un membre de l'équipe <br>
 *       Rôles : {@code PARTNER} + rôle interne {@code DIRECTEUR_AGENCE}
 * </ul>
 *
 * <p>Le double contrôle Keycloak ({@code UBAX_PARTNER}) + rôle interne DB ({@code
 * DIRECTEUR_AGENCE}) est appliqué pour toutes les opérations d'écriture.
 */
@RestController
@RequestMapping("/v1/agency/team")
@RequiredArgsConstructor
@Tag(
    name = "Agency Team",
    description =
        "Gestion de l'équipe d'une agence partenaire (membres, rôles internes). "
            + "Lecture ouverte aux membres de l'agence ; écriture réservée au `DIRECTEUR_AGENCE`.")
public class AgencyTeamController {

  private final AgencyTeamService agencyTeamService;
  private final UserRepository userRepository;
  private final RequestHeaderParser requestHeaderParser;

  // ── Lecture ────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les membres de l'équipe",
      description =
          "Retourne la liste de tous les membres actifs de l'agence à laquelle appartient "
              + "l'utilisateur connecté, triée par prénom croissant.\n\n"
              + "**Rôles autorisés :** `PARTNER` (tout membre de l'agence, quel que soit son rôle interne)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste retournée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AgencyTeamMemberResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token JWT absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — PARTNER requis ou utilisateur non rattaché à une agence",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> listTeam(HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    List<AgencyTeamMemberResponse> members = agencyTeamService.listTeam(caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AGENCY_TEAM_GET_LIST_SUCCESS,
            members));
  }

  // ── Écriture (DIRECTEUR_AGENCE uniquement) ─────────────────────

  @PostMapping
  @Operation(
      summary = "Rattacher un utilisateur à l'équipe",
      description =
          "Associe un compte UBAX existant (identifié par son email) à l'agence de l'appelant "
              + "et lui affecte le rôle interne spécifié. "
              + "L'utilisateur cible ne doit pas déjà être rattaché à une autre agence.\n\n"
              + "**Rôles autorisés :** `PARTNER` + rôle interne `DIRECTEUR_AGENCE`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Membre ajouté avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AgencyTeamMemberResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Corps de requête invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token JWT absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — PARTNER + DIRECTEUR_AGENCE requis",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Aucun compte UBAX trouvé pour l'email fourni",
        content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "L'utilisateur est déjà rattaché à une agence",
        content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InviteTeamMemberRequest.class)))
  public ResponseEntity<CustomResponse> inviteMember(
      @RequestBody @Valid InviteTeamMemberRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    requireDirecteur(caller);
    AgencyTeamMemberResponse response = agencyTeamService.inviteMember(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.AGENCY_TEAM_INVITE_SUCCESS,
                response));
  }

  @PutMapping("/{userId}/role")
  @Operation(
      summary = "Modifier le rôle interne d'un membre",
      description =
          "Met à jour le rôle interne ({@link PartnerRole}) d'un membre de l'équipe. "
              + "Le membre cible doit appartenir à la même agence que l'appelant.\n\n"
              + "**Rôles autorisés :** `PARTNER` + rôle interne `DIRECTEUR_AGENCE`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Rôle mis à jour avec succès"),
    @ApiResponse(
        responseCode = "400",
        description = "Corps de requête invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token JWT absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou membre hors périmètre agence",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Membre introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> updateMemberRole(
      @Parameter(description = "UUID du membre à modifier", required = true) @PathVariable
          UUID userId,
      @RequestBody @Valid UpdateTeamMemberRoleRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    requireDirecteur(caller);
    AgencyTeamMemberResponse response =
        agencyTeamService.updateMemberRole(caller.getSub(), userId, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AGENCY_TEAM_ROLE_UPDATE_SUCCESS,
            response));
  }

  @DeleteMapping("/{userId}")
  @Operation(
      summary = "Retirer un membre de l'équipe",
      description =
          "Supprime le rattachement de l'utilisateur à l'agence et efface son rôle interne. "
              + "Le compte UBAX de l'utilisateur n'est pas supprimé. "
              + "Un directeur ne peut pas se retirer lui-même.\n\n"
              + "**Rôles autorisés :** `PARTNER` + rôle interne `DIRECTEUR_AGENCE`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Membre retiré avec succès"),
    @ApiResponse(
        responseCode = "400",
        description = "Tentative de se retirer soi-même",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token JWT absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou membre hors périmètre agence",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Membre introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> removeMember(
      @Parameter(description = "UUID du membre à retirer", required = true) @PathVariable
          UUID userId,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    requireDirecteur(caller);
    agencyTeamService.removeMember(caller.getSub(), userId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AGENCY_TEAM_REMOVE_SUCCESS,
            null));
  }

  // ── Helper : vérification rôle interne DB ─────────────────────

  /**
   * Vérifie que l'appelant est bien {@link PartnerRole#DIRECTEUR_AGENCE} en chargeant son entité
   * depuis la base de données.
   */
  private void requireDirecteur(RequestUser caller) throws CustomException {
    User dbUser =
        userRepository
            .findByKeycloakId(caller.getSub())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Utilisateur introuvable"),
                        ResponseMessageConstants.USER_NOT_FOUND));
    RoleGuard.checkPartnerRole(dbUser, PartnerRole.DIRECTEUR_AGENCE);
  }
}
