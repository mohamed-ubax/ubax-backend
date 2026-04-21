package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.dto.AssignSubRolesRequest;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des sous-rôles applicatifs des utilisateurs (table {@code user_sub_roles}).
 *
 * <p>Ces sous-rôles ne sont pas portés par Keycloak. Ils affinent les accès à l'intérieur d'un rôle
 * Keycloak :
 *
 * <ul>
 *   <li>ADMIN → sous-rôles {@code UBAX_INTERNAL} (DIRECTEUR_GENERAL, FINANCE, OPERATIONS…)
 *   <li>PARTNER → sous-rôles {@code AGENCE} (DIRECTEUR_AGENCE, COMMERCIAL…) ou {@code HOTEL}
 *       (GERANT_HOTEL, RECEPTIONNISTE…)
 * </ul>
 */
@RestController
@RequestMapping("/v1/admin/users/{userId}/sub-roles")
@RequiredArgsConstructor
@Tag(
    name = "User Sub-Roles",
    description =
        "🔐 **SUPER_ADMIN / ADMIN** — Gestion des sous-rôles applicatifs.\n\n"
            + "**Scopes :** `UBAX_INTERNAL` (admins) · `AGENCE` (partenaires agence) · `HOTEL` (partenaires hôtel)\n\n"
            + "**UBAX_INTERNAL :** DIRECTEUR_GENERAL, SUPPORT_CLIENT, OPERATIONS, FINANCE, COMMERCIAL\n\n"
            + "**AGENCE :** DIRECTEUR_AGENCE, COMMERCIAL, COMPTABLE_AGENCE, AGENT_SAV\n\n"
            + "**HOTEL :** GERANT_HOTEL, RECEPTIONNISTE, COMPTABLE_HOTEL, RESPONSABLE_HEBERGEMENT")
public class UserSubRoleController {

  private final UserRoleService userRoleService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(
      summary = "Assigner des sous-rôles",
      description =
          "Assigne une liste de sous-rôles à un utilisateur pour un scope donné. "
              + "Réservé au SUPER_ADMIN.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Sous-rôles assignés"),
    @ApiResponse(responseCode = "400", description = "Scope/rôles incompatibles"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
  })
  public ResponseEntity<CustomResponse> assignSubRoles(
      @PathVariable UUID userId,
      @RequestBody @Valid AssignSubRolesRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
    var result = userRoleService.assignSubRoles(userId, request.getRoles(), request.getScope());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.USER_UPDATE_SUCCESS,
                result));
  }

  @GetMapping
  @Operation(
      summary = "Consulter les sous-rôles d'un utilisateur",
      description = "Retourne les sous-rôles d'un utilisateur, filtrables par scope.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste retournée")
  public ResponseEntity<CustomResponse> getSubRoles(
      @PathVariable UUID userId,
      @Parameter(description = "Filtre par scope (optionnel)") @RequestParam(required = false)
          RoleScope scope,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    var result = userRoleService.getSubRoles(userId, scope);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            result));
  }

  @DeleteMapping("/{role}")
  @Operation(
      summary = "Révoquer un sous-rôle",
      description = "Révoque un sous-rôle spécifique d'un utilisateur. Réservé au SUPER_ADMIN.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Sous-rôle révoqué"),
    @ApiResponse(responseCode = "404", description = "Sous-rôle introuvable")
  })
  public ResponseEntity<CustomResponse> revokeSubRole(
      @PathVariable UUID userId,
      @PathVariable String role,
      @RequestParam RoleScope scope,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
    userRoleService.revokeSubRole(userId, role, scope);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }
}
