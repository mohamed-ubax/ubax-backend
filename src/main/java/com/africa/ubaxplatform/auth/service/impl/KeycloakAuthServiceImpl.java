package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.auth.dto.PhoneLoginRequest;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAuthService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Implémentation du service d'authentification Keycloak.
 *
 * <p>Utilise le flux « Resource Owner Password Credentials » pour la connexion et l'endpoint de
 * logout pour révoquer le refresh token.
 */
@Service
@Slf4j
public class KeycloakAuthServiceImpl implements KeycloakAuthService {

  private final KeycloakProperties props;
  private final KeycloakAdminService keycloakAdminService;
  private final RestClient restClient;

  public KeycloakAuthServiceImpl(
      KeycloakProperties props, KeycloakAdminService keycloakAdminService) {
    this.props = props;
    this.keycloakAdminService = keycloakAdminService;
    this.restClient = RestClient.create();
  }

  // ── Login ──────────────────────────────────────────────────────

  @Override
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

  // ── Login by Phone ─────────────────────────────────────────────

  @Override
  public LoginResponse loginByPhone(PhoneLoginRequest request) throws CustomException {
    String username;
    try {
      username = keycloakAdminService.findUsernameByPhone(request.getPhone());
    } catch (NotFoundException e) {
      throw new CustomException(
          new UnAuthorizedException("Numéro de téléphone ou mot de passe invalide"),
          ResponseMessageConstants.USER_INVALID_CREDENTIALS);
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());
    form.add("username", username);
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
            new UnAuthorizedException("Numéro de téléphone ou mot de passe invalide"),
            ResponseMessageConstants.USER_INVALID_CREDENTIALS);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()), "Erreur de connexion Keycloak");
    }
  }

  // ── Logout ─────────────────────────────────────────────────────

  @Override
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
