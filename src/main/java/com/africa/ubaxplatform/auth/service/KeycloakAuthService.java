package com.africa.ubaxplatform.auth.service;

import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Service gérant la connexion et la déconnexion via l'endpoint OpenID Connect de Keycloak.
 *
 * <p>Utilise le flux « Resource Owner Password Credentials » pour la connexion (adapté aux
 * applications mobiles/SPA first-party) et l'endpoint de logout pour révoquer le refresh token.
 */
@Service
public class KeycloakAuthService {

  private final KeycloakProperties props;
  private final RestClient restClient;

  public KeycloakAuthService(KeycloakProperties props) {
    this.props = props;
    this.restClient = RestClient.create();
  }

  // ── Login ──────────────────────────────────────────────────────

  /**
   * Authentifie un utilisateur et retourne les tokens Keycloak.
   *
   * @param request email + mot de passe
   * @return access_token, refresh_token et métadonnées d'expiration
   */
  public LoginResponse login(LoginRequest request) throws CustomException {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());
    form.add("username", request.getEmail());
    form.add("password", request.getPassword());
    form.add("scope", "openid profile email");

    try {
      return restClient
          .post()
          .uri(tokenEndpoint())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(LoginResponse.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 400) {
        throw new CustomException(
            new UnAuthorizedException("Identifiants invalides ou compte inexistant"),
            ResponseMessageConstants.USER_INVALID_CREDENTIALS);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()), "Erreur de connexion Keycloak");
    }
  }

  // ── Logout ─────────────────────────────────────────────────────

  /**
   * Révoque le refresh token et invalide la session Keycloak.
   *
   * @param request refresh token à révoquer
   */
  public void logout(LogoutRequest request) throws CustomException {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());
    form.add("refresh_token", request.getRefreshToken());

    try {
      restClient
          .post()
          .uri(logoutEndpoint())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException e) {
      throw new CustomException(
          new IllegalArgumentException("Token de déconnexion invalide ou expiré"),
          "Token de déconnexion invalide");
    }
  }

  // ── Helpers ────────────────────────────────────────────────────

  private String tokenEndpoint() {
    return props.getAuthServerUrl()
        + "/realms/"
        + props.getRealm()
        + "/protocol/openid-connect/token";
  }

  private String logoutEndpoint() {
    return props.getAuthServerUrl()
        + "/realms/"
        + props.getRealm()
        + "/protocol/openid-connect/logout";
  }
}
