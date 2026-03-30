package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AssignRoleRequest;
import com.africa.ubaxplatform.auth.dto.ForgotPasswordRequest;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.auth.dto.ResetPasswordRequest;
import com.africa.ubaxplatform.auth.service.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.KeycloakAuthService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur d'authentification – délègue toutes les opérations au serveur Keycloak via l'API
 * OpenID Connect et l'API Admin REST.
 *
 * <p>Endpoints publics : {@code /login}, {@code /logout}, {@code /forgot-password}.
 *
 * <p>Endpoints protégés : {@code /reset-password} (ADMIN), {@code /users/{id}/roles} (ADMIN).
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Connexion, déconnexion et gestion des mots de passe")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final KeycloakAuthService authService;
  private final KeycloakAdminService adminService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Login ──────────────────────────────────────────────────────

  @Operation(
      summary = "Connexion utilisateur",
      description =
          "Authentifie l'utilisateur via Keycloak (flux ROPC) et retourne l'access token et le"
              + " refresh token.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Connexion réussie"),
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
  })
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  // ── Logout ─────────────────────────────────────────────────────

  @Operation(
      summary = "Déconnexion utilisateur",
      description = "Révoque le refresh token et invalide la session Keycloak.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Déconnexion réussie"),
    @ApiResponse(responseCode = "400", description = "Refresh token invalide ou expiré")
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request);
    return ResponseEntity.noContent().build();
  }

  // ── Forgot Password ────────────────────────────────────────────

  @Operation(
      summary = "Demande de réinitialisation de mot de passe",
      description =
          "Envoie un email avec un lien de réinitialisation Keycloak à l'adresse fournie."
              + " Silencieux si l'email n'existe pas (sécurité anti-enumeration).")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Email envoyé (si l'adresse existe)"),
  })
  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    try {
      adminService.sendForgotPasswordEmail(request.getEmail());
    } catch (IllegalArgumentException ignored) {
      // Réponse volontairement silencieuse pour éviter l'énumération des comptes
    }
    return ResponseEntity.noContent().build();
  }

  // ── Reset Password (ADMIN) ─────────────────────────────────────

  @Operation(
      summary = "Réinitialiser le mot de passe d'un utilisateur (Admin)",
      description =
          "Permet à un administrateur de forcer un nouveau mot de passe sans passer par le flux"
              + " email.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé"),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
  })
  @PostMapping("/reset-password")
  public ResponseEntity<CustomResponse> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    log.info("Reset password request");
    RequestUser user = requestHeaderParser.parseUserFromRequest(httpRequest);
    if (user == null)
      throw new CustomException(new NotFoundException("Utilisateur inconnu"), "Utilisateur inconnu");
    if (!user.hasRole(UserRole.ADMIN) && !user.hasRole(UserRole.SUPER_ADMIN))
      throw new CustomException(
          new UnAuthorizedException("Accès refusé – rôle ADMIN requis"),
          ResponseMessageConstants.USER_FORBIDDEN);

    adminService.resetPassword(
        request.getKeycloakId(), request.getNewPassword(), request.isTemporary());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }

  // ── Assign Role (ADMIN) ────────────────────────────────────────

  @Operation(
      summary = "Attribuer un rôle à un utilisateur (Admin)",
      description = "Assigne un rôle realm Keycloak à l'utilisateur identifié par son keycloakId.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Rôle attribué"),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur ou rôle introuvable")
  })
  @PostMapping("/users/{keycloakId}/roles")
  public ResponseEntity<CustomResponse> assignRole(
      @PathVariable String keycloakId,
      @Valid @RequestBody AssignRoleRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    log.info("Assign role {} to user {}", request.getRole(), keycloakId);
    RequestUser user = requestHeaderParser.parseUserFromRequest(httpRequest);
    if (user == null)
      throw new CustomException(new NotFoundException("Utilisateur inconnu"), "Utilisateur inconnu");
    if (!user.hasRole(UserRole.ADMIN) && !user.hasRole(UserRole.SUPER_ADMIN))
      throw new CustomException(
          new UnAuthorizedException("Accès refusé – rôle ADMIN requis"),
          ResponseMessageConstants.USER_FORBIDDEN);

    adminService.assignRole(keycloakId, request.getRole());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }
}

