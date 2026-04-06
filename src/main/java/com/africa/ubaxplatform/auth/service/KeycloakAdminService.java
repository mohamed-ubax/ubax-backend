package com.africa.ubaxplatform.auth.service;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.TokenRetrievalException;
import java.util.List;
import java.util.Map;
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
 * Service d'administration Keycloak.
 *
 * <p>Toutes les opérations nécessitent un token admin obtenu via le flux {@code
 * client_credentials}. Le client Keycloak doit avoir le service account activé avec les rôles
 * {@code realm-management → manage-users} et {@code realm-management → manage-roles}.
 */
@Service
public class KeycloakAdminService {

  private final KeycloakProperties props;
  private final RestClient restClient;

  public KeycloakAdminService(KeycloakProperties props) {
    this.props = props;
    this.restClient = RestClient.create();
  }

  // ── Forgot Password ────────────────────────────────────────────

  /**
   * Envoie un email de réinitialisation de mot de passe à l'utilisateur.
   *
   * <p>Keycloak envoie un lien sécurisé à durée limitée permettant à l'utilisateur de choisir un
   * nouveau mot de passe via l'interface Keycloak.
   *
   * @param email adresse email de l'utilisateur
   */
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
      throw e; // re-throw — le contrôleur gère silencieusement
    } catch (HttpClientErrorException e) {
      throw new CustomException(
          new IllegalArgumentException(e.getMessage()),
          "Erreur lors de l'envoi de l'email de réinitialisation");
    }
  }

  // ── Reset Password ─────────────────────────────────────────────

  /**
   * Réinitialise directement le mot de passe d'un utilisateur (sans email).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param newPassword nouveau mot de passe en clair
   * @param temporary si {@code true}, l'utilisateur devra changer son MDP à la prochaine connexion
   */
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

  /**
   * Attribue un rôle realm Keycloak à un utilisateur.
   *
   * <p>Le rôle doit exister dans le realm. Son nom correspond à la valeur en minuscules de {@link
   * UserRole} (ex : {@code client}, {@code admin}).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param role rôle à attribuer
   */
  public void assignRole(String keycloakId, UserRole role) throws CustomException {
    String adminToken = getAdminToken();
    String roleName = "UBAX_" + role.name(); // ex: UBAX_ADMIN, UBAX_CLIENT

    try {
      // 1. Récupérer la représentation complète du rôle (id + name requis par Keycloak)
      Map<String, Object> roleRepresentation =
          restClient
              .get()
              .uri(adminBaseUrl() + "/roles/" + roleName)
              .header("Authorization", "Bearer " + adminToken)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      // 2. Assign le rôle à l'utilisateur
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

  /**
   * Retire un rôle realm Keycloak d'un utilisateur.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param role rôle à retirer
   */
  public void removeRole(String keycloakId, UserRole role) throws CustomException {
    String adminToken = getAdminToken();
    String roleName = "UBAX_" + role.name();

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

  // ── Create User ────────────────────────────────────────────────

  /**
   * Crée un utilisateur dans Keycloak et retourne son identifiant (sub UUID).
   *
   * <p>Le numéro de téléphone est stocké dans l'attribut custom {@code phone}. L'email n'est pas
   * marqué comme vérifié : Keycloak enverra un email de vérification si configuré.
   *
   * @param request informations d'inscription
   * @return keycloakId (UUID) de l'utilisateur créé
   */
  public String createUser(RegisterCompleteRequest request) throws CustomException {
    String adminToken = getAdminToken();

    java.util.Map<String, Object> userRepresentation = new java.util.HashMap<>();
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

      // Keycloak retourne 201 avec Location: .../users/{id}
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

  /**
   * Supprime un utilisateur Keycloak par son identifiant (rollback en cas d'échec DB).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur à supprimer
   */
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
      // Log uniquement – ne pas propager pour ne pas masquer l'exception originale
      org.slf4j.LoggerFactory.getLogger(KeycloakAdminService.class)
          .error("Échec rollback Keycloak pour userId={}: {}", keycloakId, e.getMessage());
    }
  }

  // ── Get Roles ──────────────────────────────────────────────────

  /**
   * Récupère tous les rôles realm définis dans Keycloak.
   *
   * @return liste des rôles (champs : id, name, description, composite, clientRole)
   */
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

  /**
   * Recherche l'identifiant Keycloak d'un utilisateur par son email.
   *
   * @param email email de l'utilisateur
   * @return identifiant Keycloak (UUID)
   * @throws IllegalArgumentException si aucun utilisateur n'est trouvé
   */
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

  /**
   * Recherche l'identifiant Keycloak (UUID) d'un utilisateur par son numéro de téléphone.
   *
   * @param phone numéro de téléphone au format international
   * @return identifiant Keycloak (UUID) de l'utilisateur
   * @throws NotFoundException si aucun utilisateur ne correspond
   */
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

  /**
   * Recherche le username Keycloak d'un utilisateur par son numéro de téléphone (attribut custom).
   *
   * @param phone numéro de téléphone au format international
   * @return username Keycloak de l'utilisateur
   * @throws NotFoundException si aucun utilisateur ne correspond
   */
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

  /** Obtient un token admin via le flux client_credentials. */
  private String getAdminToken() {
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
