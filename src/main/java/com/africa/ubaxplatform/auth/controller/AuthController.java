package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.dto.AssignRoleRequest;
import com.africa.ubaxplatform.auth.dto.ForgotPasswordRequest;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.auth.dto.ResetPasswordRequest;
import com.africa.ubaxplatform.auth.service.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.KeycloakAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class AuthController {

  private final KeycloakAuthService authService;
  private final KeycloakAdminService adminService;

  public AuthController(KeycloakAuthService authService, KeycloakAdminService adminService) {
    this.authService = authService;
    this.adminService = adminService;
  }

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

  // ── Reset Password (Admin) ─────────────────────────────────────

  @Operation(
      summary = "Réinitialiser le mot de passe d'un utilisateur (Admin)",
      description =
          "Permet à un administrateur de forcer un nouveau mot de passe sans passer par le flux"
              + " email.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
  })
  @PostMapping("/reset-password")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    adminService.resetPassword(
        request.getKeycloakId(), request.getNewPassword(), request.isTemporary());
    return ResponseEntity.noContent().build();
  }

  // ── Assign Role (Admin) ────────────────────────────────────────

  @Operation(
      summary = "Attribuer un rôle à un utilisateur (Admin)",
      description = "Assigne un rôle realm Keycloak à l'utilisateur identifié par son keycloakId.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Rôle attribué"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur ou rôle introuvable")
  })
  @PostMapping("/users/{keycloakId}/roles")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
  public ResponseEntity<Void> assignRole(
      @PathVariable String keycloakId, @Valid @RequestBody AssignRoleRequest request) {
    adminService.assignRole(keycloakId, request.getRole());
    return ResponseEntity.noContent().build();
  }
}
