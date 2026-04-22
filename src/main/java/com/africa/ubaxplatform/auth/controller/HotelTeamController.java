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
 * Gestion des sous-rôles de l'équipe d'un hôtel partenaire.
 *
 * <p>Accessible par tout PARTNER appartenant au même hôtel, y compris pour s'auto-assigner des
 * sous-rôles. Les rôles sont cumulatifs.
 *
 * <p>Scope concerné : {@code HOTEL} — valeurs : GERANT_HOTEL, RECEPTIONNISTE, COMPTABLE_HOTEL,
 * RESPONSABLE_HEBERGEMENT.
 */
@RestController
@RequestMapping("/v1/hotel/team")
@RequiredArgsConstructor
@Tag(
    name = "Hotel Team",
    description =
        "🏨 **PARTNER (même hôtel)** — Gestion des sous-rôles internes hôtel.\n\n"
            + "**Rôles disponibles :** GERANT_HOTEL · RECEPTIONNISTE · COMPTABLE_HOTEL · RESPONSABLE_HEBERGEMENT\n\n"
            + "Un membre peut cumuler plusieurs sous-rôles. L'auto-assignation est autorisée.")
public class HotelTeamController {

  private final UserRoleService userRoleService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping("/{userId}/sub-roles")
  @Operation(
      summary = "Assigner des sous-rôles hôtel à un membre",
      description =
          "🛡 **Rôle requis :** `PARTNER` du même hôtel.\n\n"
              + "Assigne un ou plusieurs sous-rôles HOTEL à un membre de votre équipe. "
              + "L'auto-assignation est autorisée (`userId` = votre propre ID). "
              + "Les rôles existants sont préservés (assignation additive).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Sous-rôles assignés"),
    @ApiResponse(responseCode = "400", description = "Rôles invalides pour le scope HOTEL"),
    @ApiResponse(responseCode = "403", description = "Pas le même hôtel ou pas PARTNER"),
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
        userRoleService.assignPartnerSubRoles(callerKeycloakId, userId, roles, RoleScope.HOTEL);
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
      summary = "Consulter les sous-rôles hôtel d'un membre",
      description = "🛡 **Rôle requis :** `PARTNER` du même hôtel.",
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
        userRoleService.getPartnerSubRoles(callerKeycloakId, userId, RoleScope.HOTEL);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            result));
  }

  @DeleteMapping("/{userId}/sub-roles/{role}")
  @Operation(
      summary = "Révoquer un sous-rôle hôtel d'un membre",
      description = "🛡 **Rôle requis :** `PARTNER` du même hôtel.",
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
    userRoleService.revokePartnerSubRole(callerKeycloakId, userId, role, RoleScope.HOTEL);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }
}
