package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.UserSubRoleResponse;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des sous-rôles de l'équipe d'une agence immobilière.
 *
 * <p>Accessible par tout PARTNER appartenant à la même agence, y compris pour
 * s'auto-assigner des sous-rôles. Les rôles sont cumulatifs : assigner ne remplace
 * pas les rôles existants.
 *
 * <p>Scope concerné : {@code AGENCE} — valeurs : DIRECTEUR_AGENCE, COMMERCIAL,
 * COMPTABLE_AGENCE, AGENT_SAV.
 */
@RestController
@RequestMapping("/v1/agency/team")
@RequiredArgsConstructor
@Tag(
    name = "Agency Team",
    description =
        "🏢 **PARTNER (même agence)** — Gestion des sous-rôles internes agence.\n\n"
            + "**Rôles disponibles :** DIRECTEUR_AGENCE · COMMERCIAL · COMPTABLE_AGENCE · AGENT_SAV\n\n"
            + "Un membre peut cumuler plusieurs sous-rôles. L'auto-assignation est autorisée.")
public class AgencyTeamController {

  private final UserRoleService userRoleService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping("/{userId}/sub-roles")
  @Operation(
      summary = "Assigner des sous-rôles agence à un membre",
      description =
          "🛡 **Rôle requis :** `PARTNER` de la même agence.\n\n"
              + "Assigne un ou plusieurs sous-rôles AGENCE à un membre de votre équipe. "
              + "L'auto-assignation est autorisée (`userId` = votre propre ID). "
              + "Les rôles existants sont préservés (assignation additive).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Sous-rôles assignés"),
    @ApiResponse(responseCode = "400", description = "Rôles invalides pour le scope AGENCE"),
    @ApiResponse(responseCode = "403", description = "Pas le même agence ou pas PARTNER"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
  })
  public ResponseEntity<CustomResponse> assignSubRoles(
      @PathVariable UUID userId,
      @RequestBody @NotEmpty(message = "Au moins un rôle est requis") List<String> roles,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    String callerKeycloakId = authentication.getName();
    List<UserSubRoleResponse> result =
        userRoleService.assignPartnerSubRoles(callerKeycloakId, userId, roles, RoleScope.AGENCE);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.USER_UPDATE_SUCCESS,
                result));
  }

  @GetMapping("/{userId}/sub-roles")
  @Operation(
      summary = "Consulter les sous-rôles agence d'un membre",
      description =
          "🛡 **Rôle requis :** `PARTNER` de la même agence.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste des sous-rôles retournée")
  public ResponseEntity<CustomResponse> getSubRoles(
      @PathVariable UUID userId,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    String callerKeycloakId = authentication.getName();
    List<UserSubRoleResponse> result =
        userRoleService.getPartnerSubRoles(callerKeycloakId, userId, RoleScope.AGENCE);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            result));
  }

  @DeleteMapping("/{userId}/sub-roles/{role}")
  @Operation(
      summary = "Révoquer un sous-rôle agence d'un membre",
      description =
          "🛡 **Rôle requis :** `PARTNER` de la même agence.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Sous-rôle révoqué"),
    @ApiResponse(responseCode = "404", description = "Sous-rôle introuvable")
  })
  public ResponseEntity<CustomResponse> revokeSubRole(
      @PathVariable UUID userId,
      @PathVariable String role,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    String callerKeycloakId = authentication.getName();
    userRoleService.revokePartnerSubRole(callerKeycloakId, userId, role, RoleScope.AGENCE);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }
}
