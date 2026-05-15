package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.dto.AssignSubRolesRequest;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
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
 * Gestion des sous-rôles internes UBAX (scope {@code UBAX_INTERNAL}).
 *
 * <p>Réservé au SUPER_ADMIN. Les scopes AGENCE et HOTEL sont délégués à {@code
 * AgencyTeamController} et {@code HotelTeamController}.
 */
@RestController
@RequestMapping("/v1/admin/users/{userId}/sub-roles")
@RequiredArgsConstructor
@Tag(name = "Administration UBAX")
public class UserSubRoleController {

  private final UserRoleService userRoleService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(
      summary = "Assigner des sous-rôles UBAX_INTERNAL",
      description =
          "👑 **Rôle requis :** `SUPER_ADMIN`.\n\n"
              + "Assigne un ou plusieurs sous-rôles de scope `UBAX_INTERNAL` à un administrateur. "
              + "Les sous-rôles existants sont préservés (assignation additive).\n\n"
              + "> ⚠️ Seul le scope `UBAX_INTERNAL` est accepté ici. "
              + "Pour les scopes `AGENCE` et `HOTEL`, utilisez `/v1/agency/team` ou `/v1/hotel/team`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Sous-rôles assignés"),
    @ApiResponse(responseCode = "400", description = "Scope non UBAX_INTERNAL ou rôles invalides"),
    @ApiResponse(responseCode = "403", description = "SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
  })
  public ResponseEntity<CustomResponse> assignSubRoles(
      @PathVariable UUID userId,
      @RequestBody @Valid AssignSubRolesRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
    if (request.getScope() != RoleScope.UBAX_INTERNAL) {
      throw new CustomException(
          new BadRequestException(
              "Cet endpoint est réservé au scope UBAX_INTERNAL. "
                  + "Utilisez /v1/agency/team ou /v1/hotel/team pour les scopes AGENCE et HOTEL."));
    }
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
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Retourne tous les sous-rôles d'un utilisateur. "
              + "Le filtre `scope` est optionnel — sans filtre, tous les scopes sont retournés.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée"),
    @ApiResponse(responseCode = "403", description = "ADMIN requis")
  })
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
      summary = "Révoquer un sous-rôle UBAX_INTERNAL",
      description =
          "👑 **Rôle requis :** `SUPER_ADMIN`.\n\n"
              + "Révoque un sous-rôle de scope `UBAX_INTERNAL` d'un administrateur.\n\n"
              + "> ⚠️ Seul le scope `UBAX_INTERNAL` est accepté ici. "
              + "Pour les scopes `AGENCE` et `HOTEL`, utilisez `/v1/agency/team` ou `/v1/hotel/team`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Sous-rôle révoqué"),
    @ApiResponse(responseCode = "400", description = "Scope non UBAX_INTERNAL"),
    @ApiResponse(responseCode = "403", description = "SUPER_ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Sous-rôle introuvable")
  })
  public ResponseEntity<CustomResponse> revokeSubRole(
      @PathVariable UUID userId,
      @PathVariable String role,
      @RequestParam RoleScope scope,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);
    if (scope != RoleScope.UBAX_INTERNAL) {
      throw new CustomException(
          new BadRequestException(
              "Cet endpoint est réservé au scope UBAX_INTERNAL. "
                  + "Utilisez /v1/agency/team ou /v1/hotel/team pour les scopes AGENCE et HOTEL."));
    }
    userRoleService.revokeSubRole(userId, role, scope);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }
}
