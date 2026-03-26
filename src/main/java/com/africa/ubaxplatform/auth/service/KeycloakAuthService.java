package com.africa.ubaxplatform.auth.service;

import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

  public KeycloakAuthService(KeycloakProperties props, RestClient.Builder builder) {
    this.props = props;
    this.restClient = builder.build();
  }

  // ── Login ──────────────────────────────────────────────────────

  /**
   * Authentifie un utilisateur et retourne les tokens Keycloak.
   *
   * @param request email + mot de passe
   * @return access_token, refresh_token et métadonnées d'expiration
   */
  public LoginResponse login(LoginRequest request) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());
    form.add("username", request.getEmail());
    form.add("password", request.getPassword());
    form.add("scope", "openid profile email");

    return restClient
        .post()
        .uri(tokenEndpoint())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(LoginResponse.class);
  }

  // ── Logout ─────────────────────────────────────────────────────

  /**
   * Révoque le refresh token et invalide la session Keycloak.
   *
   * @param request refresh token à révoquer
   */
  public void logout(LogoutRequest request) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());
    form.add("refresh_token", request.getRefreshToken());

    restClient
        .post()
        .uri(logoutEndpoint())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .toBodilessEntity();
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
