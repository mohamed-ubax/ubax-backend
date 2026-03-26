package com.africa.ubaxplatform.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de connexion au serveur Keycloak.
 *
 * <p>Liées depuis le préfixe {@code keycloak} dans {@code application.yml}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

  /** URL de base du serveur Keycloak (ex : {@code http://localhost:8180}). */
  private String authServerUrl;

  /** Nom du realm Keycloak (ex : {@code ubax-plateform}). */
  private String realm;

  /** Client ID utilisé pour les appels token et admin. */
  private String clientId;

  /** Secret du client (confidentiel). */
  private String clientSecret;
}
