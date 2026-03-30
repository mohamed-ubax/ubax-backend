package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représentation de l'utilisateur extrait du JWT Keycloak.
 *
 * <p>Peuplé par {@link com.africa.ubaxplatform.common.util.RequestHeaderParser} à partir du
 * payload Base64 du Bearer token. Les champs correspondent aux claims standard OpenID Connect
 * enrichis des claims spécifiques au realm UBAX.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestUser {

  /** Identifiant unique Keycloak (claim {@code sub}). Correspond à {@code User.keycloakId}. */
  private String sub;

  /** Adresse email vérifiée (claim {@code email}). */
  private String email;

  /** Nom complet (claim {@code name}). */
  private String name;

  /** Prénom (claim {@code given_name}). */
  @JsonProperty("given_name")
  private String firstName;

  /** Nom de famille (claim {@code family_name}). */
  @JsonProperty("family_name")
  private String lastName;

  /** Nom d'utilisateur Keycloak (claim {@code preferred_username}). */
  @JsonProperty("preferred_username")
  private String username;

  /** Accès realm contenant la liste des rôles (claim {@code realm_access}). */
  @JsonProperty("realm_access")
  private RealmAccess realmAccess;

  /**
   * Rôle UBAX principal déduit des rôles Keycloak (préfixe {@code UBAX_}).
   *
   * <p>Calculé lors du parsing par {@code RequestHeaderParser.resolveRole()}.
   */
  private UserRole role;

  // ── Helpers ────────────────────────────────────────────────────

  /** @return {@code true} si l'utilisateur possède le rôle donné dans le realm. */
  public boolean hasRole(UserRole userRole) {
    if (realmAccess == null || realmAccess.getRoles() == null) return false;
    String expected = Constants.KEYCLOAK_ROLE_PREFIX + userRole.name();
    return realmAccess.getRoles().stream().anyMatch(r -> r.equalsIgnoreCase(expected));
  }

  /** @return la liste brute des rôles du realm (ex: {@code ["UBAX_ADMIN", "offline_access"]}). */
  public List<String> getRealmRoles() {
    if (realmAccess == null || realmAccess.getRoles() == null) return List.of();
    return realmAccess.getRoles();
  }

  // ── Inner classes ───────────────────────────────────────────────

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RealmAccess {
    private List<String> roles = new ArrayList<>();
  }
}
