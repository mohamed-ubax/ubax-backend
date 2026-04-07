package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.auth.dto.PhoneLoginRequest;
import com.africa.ubaxplatform.common.exception.CustomException;

/**
 * Contrat du service d'authentification Keycloak.
 *
 * <p>Gère la connexion et la déconnexion des utilisateurs en déléguant à l'endpoint OpenID Connect
 * de Keycloak. Utilise le flux <em>Resource Owner Password Credentials</em> (ROPC), adapté aux
 * applications mobiles et SPA first-party.
 *
 * <p><b>Connexion email :</b> {@link #login(LoginRequest)}<br>
 * <b>Connexion téléphone (mobile) :</b> {@link #loginByPhone(PhoneLoginRequest)}<br>
 * <b>Déconnexion :</b> {@link #logout(LogoutRequest)}
 */
public interface KeycloakAuthService {

  /**
   * Authentifie un utilisateur par email et mot de passe.
   *
   * <p>Échange les identifiants contre un couple {@code access_token} / {@code refresh_token}
   * Keycloak via le flux ROPC.
   *
   * @param request objet contenant {@code email} et {@code password}
   * @return tokens Keycloak ({@code access_token}, {@code refresh_token}, expiration)
   * @throws CustomException si les identifiants sont invalides (401/400) ou si Keycloak est
   *     indisponible
   */
  LoginResponse login(LoginRequest request) throws CustomException;

  /**
   * Authentifie un utilisateur par numéro de téléphone et mot de passe (usage mobile).
   *
   * <p>Résout d'abord le username Keycloak associé au numéro via {@link
   * KeycloakAdminService#findUsernameByPhone(String)}, puis applique le flux ROPC. Le numéro de
   * téléphone doit être au format international (ex : {@code +221781234567}).
   *
   * @param request objet contenant {@code phone} et {@code password}
   * @return tokens Keycloak ({@code access_token}, {@code refresh_token}, expiration)
   * @throws CustomException si le numéro est inconnu ou si le mot de passe est incorrect
   */
  LoginResponse loginByPhone(PhoneLoginRequest request) throws CustomException;

  /**
   * Révoque le refresh token et invalide la session Keycloak de l'utilisateur.
   *
   * <p>Appelle l'endpoint {@code /protocol/openid-connect/logout}. Après déconnexion, le {@code
   * refresh_token} ne pourra plus être utilisé pour obtenir de nouveaux {@code access_token}.
   *
   * @param request objet contenant le {@code refreshToken} à révoquer
   * @throws CustomException si le token est invalide, expiré ou déjà révoqué
   */
  void logout(LogoutRequest request) throws CustomException;
}
