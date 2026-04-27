package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.TokenRetrievalException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Implémentation du service d'administration Keycloak.
 *
 * <p>Toutes les opérations nécessitent un token admin obtenu via le flux {@code
 * client_credentials}. Le client Keycloak doit avoir le service account activé avec les rôles
 * {@code realm-management → manage-users} et {@code realm-management → manage-roles}.
 */
@Service
@Slf4j
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

  private final KeycloakProperties props;
  private final RestClient restClient;
  private static final String UBAX_PREFIX = "UBAX_";

  public KeycloakAdminServiceImpl(KeycloakProperties props) {
    this.props = props;
    this.restClient = RestClient.create();
  }

  // ── Forgot Password ────────────────────────────────────────────

  @Override
  public void sendForgotPasswordEmail(String email) throws CustomException {
    try {
      String keycloakId = findUserIdByEmail(email);
      String adminToken = getAdminToken();

      restClient
          .put()
          .uri(adminBaseUrl() + "/users/" + keycloakId + "/execute-actions-email")
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(List.of("UPDATE_PASSWORD"))
          .retrieve()
          .toBodilessEntity();
    } catch (NotFoundException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur lors de l'envoi de l'email de réinitialisation");
    }
  }

  // ── Reset Password ─────────────────────────────────────────────

  @Override
  public void resetPassword(String keycloakId, String newPassword, boolean temporary)
      throws CustomException {
    String adminToken = getAdminToken();

    Map<String, Object> credential =
        Map.of(
            "type", "password",
            "value", newPassword,
            "temporary", temporary);

    try {
      restClient
          .put()
          .uri(adminBaseUrl() + "/users/" + keycloakId + "/reset-password")
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(credential)
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        throw new CustomException(
            new NotFoundException("Utilisateur introuvable dans Keycloak"),
            ResponseMessageConstants.USER_NOT_FOUND);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur lors de la réinitialisation du mot de passe");
    }
  }

  // ── Assign Role ────────────────────────────────────────────────

  @Override
  public void assignRole(String keycloakId, UserRole role) throws CustomException {
    String adminToken = getAdminToken();
    String roleName = UBAX_PREFIX + role.name();

    try {
      Map<String, Object> roleRepresentation =
          restClient
              .get()
              .uri(adminBaseUrl() + "/roles/" + roleName)
              .header("Authorization", "Bearer " + adminToken)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (roleRepresentation != null) {
        restClient
            .post()
            .uri(adminBaseUrl() + "/users/" + keycloakId + "/role-mappings/realm")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(List.of(roleRepresentation))
            .retrieve()
            .toBodilessEntity();
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        throw new CustomException(
            new NotFoundException("Rôle '" + roleName + "' ou utilisateur introuvable"),
            ResponseMessageConstants.USER_NOT_FOUND);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()), "Erreur lors de l'assignation du rôle");
    }
  }

  @Override
  public void removeRole(String keycloakId, UserRole role) throws CustomException {
    String adminToken = getAdminToken();
    String roleName = UBAX_PREFIX + role.name();

    try {
      Map<String, Object> roleRepresentation =
          restClient
              .get()
              .uri(adminBaseUrl() + "/roles/" + roleName)
              .header("Authorization", "Bearer " + adminToken)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (roleRepresentation != null) {
        restClient
            .method(org.springframework.http.HttpMethod.DELETE)
            .uri(adminBaseUrl() + "/users/" + keycloakId + "/role-mappings/realm")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(List.of(roleRepresentation))
            .retrieve()
            .toBodilessEntity();
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        throw new CustomException(
            new NotFoundException("Rôle '" + roleName + "' ou utilisateur introuvable"),
            ResponseMessageConstants.USER_NOT_FOUND);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()), "Erreur lors du retrait du rôle");
    }
  }

  // ── Create / Delete User ───────────────────────────────────────

  @Override
  public String createUser(RegisterCompleteRequest request) throws CustomException {
    String adminToken = getAdminToken();

    Map<String, Object> userRepresentation = new HashMap<>();
    userRepresentation.put("username", request.getPhone());
    userRepresentation.put("firstName", request.getFirstName());
    userRepresentation.put("lastName", request.getLastName());
    userRepresentation.put("enabled", true);
    userRepresentation.put("emailVerified", false);
    userRepresentation.put("attributes", Map.of("phone", List.of(request.getPhone())));
    userRepresentation.put(
        "credentials",
        List.of(Map.of("type", "password", "value", request.getPassword(), "temporary", false)));
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      userRepresentation.put("email", request.getEmail());
    }

    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(adminBaseUrl() + "/users")
              .header("Authorization", "Bearer " + adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(userRepresentation)
              .retrieve()
              .toBodilessEntity();

      if (response.getStatusCode() == HttpStatusCode.valueOf(201)
          && response.getHeaders().getLocation() != null) {
        String location = response.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
      }
      throw new CustomException(
          new IllegalStateException("Création Keycloak sans Location header"),
          "Erreur lors de la création du compte Keycloak");
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 409) {
        throw new CustomException(
            new IllegalArgumentException("Email ou téléphone déjà utilisé dans Keycloak"),
            ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur lors de la création du compte Keycloak");
    }
  }

  @Override
  public void deleteUser(String keycloakId) {
    try {
      String adminToken = getAdminToken();
      restClient
          .delete()
          .uri(adminBaseUrl() + "/users/" + keycloakId)
          .header("Authorization", "Bearer " + adminToken)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(KeycloakAdminServiceImpl.class)
          .error("Échec rollback Keycloak pour userId={}: {}", keycloakId, e.getMessage());
    }
  }

  @Override
  public void disableUser(String keycloakId) {
    try {
      String adminToken = getAdminToken();
      restClient
          .put()
          .uri(adminBaseUrl() + "/users/" + keycloakId)
          .header("Authorization", "Bearer " + adminToken)
          .header("Content-Type", "application/json")
          .body(Map.of("enabled", false))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(KeycloakAdminServiceImpl.class)
          .error("Échec désactivation Keycloak pour userId={}: {}", keycloakId, e.getMessage());
    }
  }

  // ── Get Roles ──────────────────────────────────────────────────

  @Override
  public List<Map<String, Object>> getRoles() {
    String adminToken = getAdminToken();
    List<Map<String, Object>> roles =
        restClient
            .get()
            .uri(adminBaseUrl() + "/roles")
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return roles != null ? roles : List.of();
  }

  // ── Find User ──────────────────────────────────────────────────

  @Override
  public String findUserIdByEmail(String email) {
    String adminToken = getAdminToken();

    List<Map<String, Object>> users =
        restClient
            .get()
            .uri(adminBaseUrl() + "/users?email={email}&exact=true", email)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    if (users == null || users.isEmpty()) {
      throw new NotFoundException("Aucun utilisateur trouvé avec l'email : " + email);
    }

    return (String) users.getFirst().get("id");
  }

  @Override
  public String findUserIdByPhone(String phone) {
    String adminToken = getAdminToken();

    List<Map<String, Object>> users =
        restClient
            .get()
            .uri(adminBaseUrl() + "/users?q=phone:{phone}&exact=true", phone)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    if (users == null || users.isEmpty()) {
      throw new NotFoundException("Aucun utilisateur trouvé avec le numéro : " + phone);
    }

    return (String) users.getFirst().get("id");
  }

  @Override
  public String findUsernameByPhone(String phone) {
    String adminToken = getAdminToken();

    List<Map<String, Object>> users =
        restClient
            .get()
            .uri(adminBaseUrl() + "/users?q=phone:{phone}&exact=true", phone)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    if (users == null || users.isEmpty()) {
      throw new NotFoundException("Aucun utilisateur trouvé avec le numéro : " + phone);
    }

    return (String) users.getFirst().get("username");
  }

  // ── Admin Account ──────────────────────────────────────────────

  @Override
  public String createAdminAccount(String email, String firstName, String lastName, String phone)
      throws CustomException {
    String adminToken = getAdminToken();

    Map<String, Object> userRepresentation = new HashMap<>();
    userRepresentation.put("username", email);
    userRepresentation.put("email", email);
    userRepresentation.put("firstName", firstName);
    userRepresentation.put("lastName", lastName);
    userRepresentation.put("enabled", true);
    userRepresentation.put("emailVerified", true);
    userRepresentation.put("requiredActions", List.of("UPDATE_PASSWORD"));
    if (phone != null && !phone.isBlank()) {
      userRepresentation.put("attributes", Map.of("phone", List.of(phone)));
    }

    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(adminBaseUrl() + "/users")
              .header("Authorization", "Bearer " + adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(userRepresentation)
              .retrieve()
              .toBodilessEntity();

      if (response.getStatusCode() == HttpStatusCode.valueOf(201)
          && response.getHeaders().getLocation() != null) {
        String location = response.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
      }
      throw new CustomException(
          new IllegalStateException("Keycloak : pas de Location header"),
          "Erreur lors de la création du compte administrateur");
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 409) {
        throw new CustomException(
            new IllegalArgumentException("Email déjà utilisé dans Keycloak : " + email),
            ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur Keycloak lors de la création du compte administrateur");
    }
  }

  // ── Partner Account ────────────────────────────────────────────

  @Override
  public String createPartnerAccount(
      String email, String companyName, String legalRepresentative, String phone)
      throws CustomException {
    String adminToken = getAdminToken();

    Map<String, Object> userRepresentation = new HashMap<>();
    userRepresentation.put("username", email);
    userRepresentation.put("email", email);
    userRepresentation.put("firstName", companyName);
    userRepresentation.put("lastName", legalRepresentative);
    userRepresentation.put("enabled", true);
    userRepresentation.put("emailVerified", true);
    userRepresentation.put("requiredActions", List.of("UPDATE_PASSWORD"));
    userRepresentation.put("attributes", Map.of("phone", List.of(phone)));

    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(adminBaseUrl() + "/users")
              .header("Authorization", "Bearer " + adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(userRepresentation)
              .retrieve()
              .toBodilessEntity();

      if (response.getStatusCode() == HttpStatusCode.valueOf(201)
          && response.getHeaders().getLocation() != null) {
        String location = response.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
      }
      throw new CustomException(
          new IllegalStateException("Keycloak : pas de Location header"),
          "Erreur lors de la création du compte partenaire");
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 409) {
        throw new CustomException(
            new IllegalArgumentException("Email déjà utilisé dans Keycloak : " + email),
            ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur Keycloak lors de la création du compte partenaire");
    }
  }

  // ── Bailleur Account ───────────────────────────────────────────

  @Override
  public String createBailleurAccount(
      String firstName, String lastName, String phone, String email, String tempPassword)
      throws CustomException {
    String adminToken = getAdminToken();

    Map<String, Object> userRepresentation = new HashMap<>();
    userRepresentation.put("username", phone);
    userRepresentation.put("firstName", firstName);
    userRepresentation.put("lastName", lastName);
    userRepresentation.put("enabled", true);
    userRepresentation.put("emailVerified", false);
    Map<String, Object> attributes = new java.util.HashMap<>();
    attributes.put("phone", List.of(phone));
    if (email != null && !email.isBlank()) {
      userRepresentation.put("email", email);
      attributes.put("email", List.of(email));
    }
    userRepresentation.put("attributes", attributes);
    userRepresentation.put(
        "credentials",
        List.of(Map.of("type", "password", "value", tempPassword, "temporary", false)));

    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(adminBaseUrl() + "/users")
              .header("Authorization", "Bearer " + adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(userRepresentation)
              .retrieve()
              .toBodilessEntity();

      if (response.getStatusCode() == HttpStatusCode.valueOf(201)
          && response.getHeaders().getLocation() != null) {
        String location = response.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
      }
      throw new CustomException(
          new IllegalStateException("Keycloak : pas de Location header"),
          "Erreur lors de la création du compte bailleur");
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 409) {
        throw new CustomException(
            new IllegalArgumentException("Téléphone déjà utilisé dans Keycloak : " + phone),
            ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
      }
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur Keycloak lors de la création du compte bailleur");
    }
  }

  @Override
  public void sendSetPasswordLink(String keycloakId) throws CustomException {
    String adminToken = getAdminToken();
    try {
      restClient
          .put()
          .uri(adminBaseUrl() + "/users/" + keycloakId + "/execute-actions-email")
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(List.of("UPDATE_PASSWORD"))
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException e) {
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur lors de l'envoi du lien de définition du mot de passe");
    }
  }

  // ── Helpers ────────────────────────────────────────────────────

  protected String getAdminToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());

    try {
      Map<String, Object> response =
          restClient
              .post()
              .uri(tokenEndpoint())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || !response.containsKey("access_token")) {
        throw new TokenRetrievalException("Impossible d'obtenir le token admin Keycloak");
      }
      return (String) response.get("access_token");
    } catch (TokenRetrievalException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      throw new TokenRetrievalException(
          "Authentification du service account échouée : " + e.getMessage(), e);
    }
  }

  private String tokenEndpoint() {
    return props.getAuthServerUrl()
        + "/realms/"
        + props.getRealm()
        + "/protocol/openid-connect/token";
  }

  private String adminBaseUrl() {
    return props.getAuthServerUrl() + "/admin/realms/" + props.getRealm();
  }
}
