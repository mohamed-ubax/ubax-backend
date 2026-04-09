package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.dto.AssignRoleRequest;
import com.africa.ubaxplatform.auth.dto.ForgotPasswordRequest;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.auth.dto.PhoneLoginRequest;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.auth.dto.ResetPasswordByPhoneRequest;
import com.africa.ubaxplatform.auth.dto.ResetPasswordRequest;
import com.africa.ubaxplatform.auth.dto.SendOtpRequest;
import com.africa.ubaxplatform.auth.dto.VerifyOtpRequest;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAuthService;
import com.africa.ubaxplatform.auth.service.interfaces.PasswordResetService;
import com.africa.ubaxplatform.auth.service.interfaces.RegistrationService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur d'authentification – délègue toutes les opérations au serveur Keycloak via l'API
 * OpenID Connect et l'API Admin REST.
 *
 * <p>Endpoints publics :
 *
 * <ul>
 *   <li>{@code POST /v1/auth/login} – connexion email/mot de passe
 *   <li>{@code POST /v1/auth/login/phone} – connexion téléphone/mot de passe (mobile)
 *   <li>{@code POST /v1/auth/logout}
 *   <li>{@code POST /v1/auth/forgot-password}
 *   <li>{@code POST /v1/auth/register/send-otp} – étape 1 inscription mobile
 *   <li>{@code POST /v1/auth/register/verify-otp} – étape 2 : vérification OTP
 *   <li>{@code POST /v1/auth/register/complete} – étape 3 : finalisation inscription
 * </ul>
 *
 * <p>Endpoints protégés : {@code /v1/auth/reset-password} (ADMIN), {@code
 * /v1/auth/users/{id}/roles} (ADMIN).
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final KeycloakAuthService authService;
  private final KeycloakAdminService adminService;
  private final RegistrationService registrationService;
  private final PasswordResetService passwordResetService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Login (email) ──────────────────────────────────────────────

  @Operation(
      tags = {"Authentication"},
      summary = "Connexion utilisateur",
      description =
          "Authentifie l'utilisateur via Keycloak (flux ROPC) et retourne l'access token et le"
              + " refresh token.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Connexion réussie"),
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
  })
  @PostMapping("/login")
  public ResponseEntity<CustomResponse> login(@Valid @RequestBody LoginRequest request)
      throws CustomException {
    LoginResponse loginResponse = authService.login(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_LOGIN_SUCCESS,
            loginResponse));
  }

  // ── Login (phone) ──────────────────────────────────────────────

  @Operation(
      tags = {"Mobile"},
      summary = "Connexion via numéro de téléphone (Mobile)",
      description =
          "Authentifie l'utilisateur via son numéro de téléphone et son mot de passe (flux ROPC).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Connexion réussie"),
    @ApiResponse(responseCode = "401", description = "Identifiants invalides")
  })
  @PostMapping("/login/phone")
  public ResponseEntity<CustomResponse> loginByPhone(@Valid @RequestBody PhoneLoginRequest request)
      throws CustomException {
    LoginResponse loginResponse = authService.loginByPhone(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_LOGIN_SUCCESS,
            loginResponse));
  }

  // ── Register – Étape 1 : Envoi OTP ────────────────────────────

  @Operation(
      tags = {"Mobile"},
      summary = "Inscription mobile – Étape 1 : Envoi OTP",
      description =
          "Génère et envoie un code OTP à 6 chiffres par SMS au numéro de téléphone fourni.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP envoyé"),
    @ApiResponse(responseCode = "409", description = "Numéro de téléphone déjà utilisé")
  })
  @PostMapping("/register/send-otp")
  public ResponseEntity<CustomResponse> sendOtp(@Valid @RequestBody SendOtpRequest request)
      throws CustomException {
    registrationService.sendOtp(request.getPhone());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.OTP_SENT_SUCCESS,
            null));
  }

  // ── Register – Étape 2 : Vérification OTP ─────────────────────

  @Operation(
      tags = {"Mobile"},
      summary = "Inscription mobile – Étape 2 : Vérification OTP",
      description = "Vérifie le code OTP reçu par SMS. Le code est valide une seule fois.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP vérifié avec succès"),
    @ApiResponse(responseCode = "400", description = "Code OTP invalide ou expiré")
  })
  @PostMapping("/register/verify-otp")
  public ResponseEntity<CustomResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request)
      throws CustomException {
    registrationService.verifyOtp(request.getPhone(), request.getCode());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.OTP_VERIFIED_SUCCESS,
            null));
  }

  // ── Register – Étape 3 : Complétion de l'inscription ──────────

  @Operation(
      tags = {"Mobile"},
      summary = "Inscription mobile – Étape 3 : Finalisation",
      description =
          "Finalise l'inscription. Crée le compte dans Keycloak puis en base PostgreSQL."
              + " Retourne les informations du compte créé.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
    @ApiResponse(responseCode = "409", description = "Email ou téléphone déjà utilisé")
  })
  @PostMapping("/register/complete")
  public ResponseEntity<CustomResponse> registerComplete(
      @Valid @RequestBody RegisterCompleteRequest request) throws CustomException {
    RegisterResponse response = registrationService.register(request);
    return ResponseEntity.status(201)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.USER_CREATE_SUCCESS,
                response));
  }

  // ── Logout ─────────────────────────────────────────────────────

  @Operation(
      tags = {"Authentication"},
      summary = "Déconnexion utilisateur",
      description = "Révoque le refresh token et invalide la session Keycloak.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Déconnexion réussie"),
    @ApiResponse(responseCode = "400", description = "Refresh token invalide ou expiré")
  })
  @PostMapping("/logout")
  public ResponseEntity<CustomResponse> logout(@Valid @RequestBody LogoutRequest request)
      throws CustomException {
    authService.logout(request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_LOGOUT_SUCCESS,
            null));
  }

  // ── Forgot Password (email) ────────────────────────────────────────

  @Operation(
      tags = {"Authentication"},
      summary = "Demande de réinitialisation de mot de passe",
      description =
          "Envoie un email avec un lien de réinitialisation Keycloak à l'adresse fournie."
              + " Silencieux si l'email n'existe pas (sécurité anti-enumeration).")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Email envoyé (si l'adresse existe)"),
  })
  @PostMapping("/forgot-password")
  public ResponseEntity<CustomResponse> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    try {
      adminService.sendForgotPasswordEmail(request.getEmail());
    } catch (NotFoundException | CustomException ignored) {
      // Réponse volontairement silencieuse pour éviter l'énumération des comptes
    }
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_FORGOT_PASSWORD_SUCCESS,
            null));
  }

  // ── Forgot Password via SMS OTP ─────────────────────────────────

  @Operation(
      tags = {"Mobile"},
      summary = "Récupération MDP – Étape 1 : Envoi OTP SMS",
      description =
          "Génère et envoie un OTP par SMS au numéro fourni. Le numéro doit être enregistré.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP envoyé"),
    @ApiResponse(responseCode = "404", description = "Numéro non enregistré")
  })
  @PostMapping("/forgot-password/send-otp")
  public ResponseEntity<CustomResponse> forgotPasswordSendOtp(
      @Valid @RequestBody SendOtpRequest request) throws CustomException {
    passwordResetService.sendResetOtp(request.getPhone());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.OTP_SENT_SUCCESS,
            null));
  }

  @Operation(
      tags = {"Mobile"},
      summary = "Récupération MDP – Étape 2 : Vérification OTP",
      description =
          "Vérifie le code OTP reçu par SMS. Le code n'est pas consommé (étape 3 requise).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OTP valide"),
    @ApiResponse(responseCode = "400", description = "OTP invalide ou expiré")
  })
  @PostMapping("/forgot-password/verify-otp")
  public ResponseEntity<CustomResponse> forgotPasswordVerifyOtp(
      @Valid @RequestBody VerifyOtpRequest request) throws CustomException {
    passwordResetService.verifyResetOtp(request.getPhone(), request.getCode());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.OTP_VERIFIED_SUCCESS,
            null));
  }

  @Operation(
      tags = {"Mobile"},
      summary = "Récupération MDP – Étape 3 : Nouveau mot de passe",
      description = "Vérifie l'OTP (le consomme) et réinitialise le mot de passe.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé"),
    @ApiResponse(responseCode = "400", description = "OTP invalide ou expiré")
  })
  @PostMapping("/forgot-password/reset")
  public ResponseEntity<CustomResponse> forgotPasswordReset(
      @Valid @RequestBody ResetPasswordByPhoneRequest request) throws CustomException {
    passwordResetService.resetPassword(
        request.getPhone(), request.getCode(), request.getNewPassword());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }

  // ── Reset Password (ADMIN) ─────────────────────────────────────

  @Operation(
      tags = {"Admin"},
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
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    adminService.resetPassword(
        request.getKeycloakId(), request.getNewPassword(), request.isTemporary());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_UPDATE_SUCCESS,
            null));
  }

  // ── Get Roles (ADMIN) ──────────────────────────────────────────

  @Operation(
      tags = {"Admin"},
      summary = "Lister tous les rôles Keycloak (Admin)",
      description = "Retourne l'ensemble des rôles realm définis dans le realm Keycloak.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste des rôles"),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis")
  })
  @GetMapping("/roles")
  public ResponseEntity<CustomResponse> getRoles(HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_LIST_SUCCESS,
            adminService.getRoles()));
  }

  // ── Assign Role (ADMIN) ────────────────────────────────────────

  @Operation(
      tags = {"Admin"},
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
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    adminService.assignRole(keycloakId, request.getRole());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_ROLE_ASSIGN_SUCCESS,
            null));
  }

  // ── Remove Role (ADMIN) ────────────────────────────────────────

  @Operation(
      tags = {"Admin"},
      summary = "Retirer un rôle à un utilisateur (Admin)",
      description =
          "Supprime un rôle realm Keycloak de l'utilisateur identifié par son keycloakId.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Rôle retiré"),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Utilisateur ou rôle introuvable")
  })
  @DeleteMapping("/users/{keycloakId}/roles")
  public ResponseEntity<CustomResponse> removeRole(
      @PathVariable String keycloakId,
      @Valid @RequestBody AssignRoleRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    log.info("Remove role {} from user {}", request.getRole(), keycloakId);
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    adminService.removeRole(keycloakId, request.getRole());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_ROLE_ASSIGN_SUCCESS,
            null));
  }
}
