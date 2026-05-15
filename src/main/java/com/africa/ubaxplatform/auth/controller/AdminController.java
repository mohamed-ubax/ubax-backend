package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.dto.AdminUserResponse;
import com.africa.ubaxplatform.auth.dto.AssignAdminRoleRequest;
import com.africa.ubaxplatform.auth.dto.CreateAdminRequest;
import com.africa.ubaxplatform.auth.service.interfaces.AdminManagementService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
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
 * Contrôleur de gestion des administrateurs internes UBAX.
 *
 * <p>Endpoints réservés aux Super Administrateurs pour la création de comptes admin, et aux
 * administrateurs pour la consultation.
 *
 * <ul>
 *   <li>{@code POST /v1/admin/users} – créer un administrateur (SUPER_ADMIN uniquement)
 *   <li>{@code GET /v1/admin/users} – lister les administrateurs (ADMIN ou SUPER_ADMIN)
 * </ul>
 */
@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administration UBAX")
public class AdminController {

  private final AdminManagementService adminManagementService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Créer un administrateur ────────────────────────────────────

  @Operation(
      summary = "Créer un administrateur UBAX",
      description =
          "👑 **Rôle requis :** `SUPER_ADMIN`\n\n"
              + "Crée un compte administrateur interne UBAX. "
              + "Déclenche la création Keycloak, l'attribution du rôle (`ADMIN` ou `SUPER_ADMIN`), "
              + "la synchronisation en base et l'envoi d'un email de définition du mot de passe.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Administrateur créé avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Données invalides ou rôle non autorisé",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Accès refusé – rôle SUPER_ADMIN requis",
        content = @Content),
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CreateAdminRequest.class)))
  @PostMapping
  public ResponseEntity<CustomResponse> createAdmin(
      @Valid @RequestBody CreateAdminRequest request, HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);

    AdminUserResponse response = adminManagementService.createAdmin(request);
    log.info("Administrateur créé : email={}, role={}", request.getEmail(), request.getRole());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.OK,
                ResponseMessageConstants.ADMIN_CREATE_SUCCESS,
                response));
  }

  // ── Lister les administrateurs ─────────────────────────────────

  @Operation(
      summary = "Lister les administrateurs UBAX",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne la liste de tous les administrateurs actifs de la plateforme.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des administrateurs",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Accès refusé – rôle ADMIN requis",
        content = @Content)
  })
  @GetMapping
  public ResponseEntity<CustomResponse> listAdmins(HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    List<AdminUserResponse> admins = adminManagementService.listAdmins();

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_LIST_SUCCESS,
            admins));
  }

  // ── Affecter un rôle à un administrateur ───────────────────────

  @Operation(
      summary = "Affecter un rôle à un administrateur UBAX",
      description =
          "👑 **Rôle requis :** `SUPER_ADMIN`\n\n"
              + "Modifie le rôle d'un administrateur existant (`ADMIN` ↔ `SUPER_ADMIN`). "
              + "L'ancien rôle est retiré dans Keycloak avant d'attribuer le nouveau.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Rôle affecté avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminUserResponse.class))),
    @ApiResponse(responseCode = "400", description = "Rôle invalide", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Accès refusé – rôle SUPER_ADMIN requis",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Administrateur introuvable",
        content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AssignAdminRoleRequest.class)))
  @PutMapping("/{userId}/role")
  public ResponseEntity<CustomResponse> assignAdminRole(
      @PathVariable UUID userId,
      @Valid @RequestBody AssignAdminRoleRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);

    AdminUserResponse response = adminManagementService.assignAdminRole(userId, request.getRole());
    log.info("Rôle affecté à l'administrateur : userId={}, role={}", userId, request.getRole());

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.ADMIN_ROLE_ASSIGN_SUCCESS,
            response));
  }

  // ── Supprimer logiquement un administrateur ────────────────────

  @Operation(
      summary = "Supprimer logiquement un administrateur UBAX",
      description =
          "👑 **Rôle requis :** `SUPER_ADMIN`\n\n"
              + "Suppression logique : désactive le compte Keycloak (`enabled=false`) "
              + "et positionne `deletedAt` en base. Le compte n'est pas physiquement supprimé.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Administrateur supprimé logiquement",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Accès refusé – rôle SUPER_ADMIN requis",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Administrateur introuvable",
        content = @Content),
    @ApiResponse(responseCode = "409", description = "Compte déjà supprimé", content = @Content)
  })
  @DeleteMapping("/{userId}")
  public ResponseEntity<CustomResponse> deleteAdmin(
      @PathVariable UUID userId, HttpServletRequest httpRequest) throws CustomException {

    RoleGuard.requireSuperAdmin(requestHeaderParser, httpRequest);

    adminManagementService.deleteAdmin(userId);
    log.info("Administrateur supprimé logiquement : userId={}", userId);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.ADMIN_DELETE_SUCCESS,
            null));
  }
}
