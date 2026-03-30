package com.africa.ubaxplatform.auth.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configure le client Keycloak admin utilisé pour les appels inter-services. */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
@RequiredArgsConstructor
public class KeycloakClientConfig {

  private final KeycloakProperties properties;

  /**
   * Fournit une instance {@link Keycloak} admin pour les appels machine-to-machine (client
   * credentials grant).
   */
  @Bean
  public Keycloak keycloak() {
    return KeycloakBuilder.builder()
        .serverUrl(properties.getAuthServerUrl())
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .realm(properties.getRealm())
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .build();
  }
}
