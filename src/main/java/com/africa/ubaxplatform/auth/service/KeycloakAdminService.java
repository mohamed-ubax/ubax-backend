package com.africa.ubaxplatform.auth.service;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
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

  public KeycloakAdminService(KeycloakProperties props, RestClient.Builder builder) {
    this.props = props;
    this.restClient = builder.build();
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
  public void sendForgotPasswordEmail(String email) {
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
  }

  // ── Reset Password ─────────────────────────────────────────────

  /**
   * Réinitialise directement le mot de passe d'un utilisateur (sans email).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param newPassword nouveau mot de passe en clair
   * @param temporary si {@code true}, l'utilisateur devra changer son MDP à la prochaine connexion
   */
  public void resetPassword(String keycloakId, String newPassword, boolean temporary) {
    String adminToken = getAdminToken();

    Map<String, Object> credential =
        Map.of(
            "type", "password",
            "value", newPassword,
            "temporary", temporary);

    restClient
        .put()
        .uri(adminBaseUrl() + "/users/" + keycloakId + "/reset-password")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(credential)
        .retrieve()
        .toBodilessEntity();
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
  public void assignRole(String keycloakId, UserRole role) {
    String adminToken = getAdminToken();
    String roleName = role.name().toLowerCase();

    // 1. Récupérer la représentation complète du rôle (id + name requis par Keycloak)
    Map<String, Object> roleRepresentation =
        restClient
            .get()
            .uri(adminBaseUrl() + "/roles/" + roleName)
            .header("Authorization", "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    // 2. Assign le rôle à l'utilisateur
    restClient
        .post()
        .uri(adminBaseUrl() + "/users/" + keycloakId + "/role-mappings/realm")
        .header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(List.of(roleRepresentation))
        .retrieve()
        .toBodilessEntity();
  }

  // ── Helpers ────────────────────────────────────────────────────

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
      throw new IllegalArgumentException("Aucun utilisateur trouvé avec l'email : " + email);
    }

    return (String) users.get(0).get("id");
  }

  /** Obtient un token admin via le flux client_credentials. */
  private String getAdminToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", props.getClientId());
    form.add("client_secret", props.getClientSecret());

    Map<String, Object> response =
        restClient
            .post()
            .uri(tokenEndpoint())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    if (response == null || !response.containsKey("access_token")) {
      throw new HttpClientErrorException(
          org.springframework.http.HttpStatus.UNAUTHORIZED,
          "Impossible d'obtenir le token admin Keycloak");
    }

    return (String) response.get("access_token");
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
