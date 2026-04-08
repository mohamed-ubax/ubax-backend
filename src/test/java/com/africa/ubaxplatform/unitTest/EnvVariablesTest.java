package com.africa.ubaxplatform.unitTest;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que les variables du profil "local" (application-local.yml) sont correctement chargées.
 * Lancer avec le profil actif : -Dspring.profiles.active=local
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class EnvVariablesTest {

  @Value("${spring.mail.username}")
  private String mailUsername;

  @Value("${spring.mail.password}")
  private String mailPassword;

  @Value("${lam.sms.account-id}")
  private String lamAccountId;

  @Value("${lam.sms.password}")
  private String lamPassword;

  @Value("${keycloak.client-id}")
  private String keycloakClientId;

  @Value("${keycloak.client-secret}")
  private String keycloakClientSecret;

  @Test
  void mailPasswordShouldBeLoaded() {
    log.info("MAIL_USERNAME = {} ", mailUsername);
    log.info(
        "MAIL_PASSWORD = {} ",
        (mailPassword != null ? "[" + mailPassword.length() + " chars]" : "NULL"));
    assertThat(mailPassword)
        .as("MAIL_PASSWORD doit être chargé depuis le .env (non vide)")
        .isNotBlank();
  }

  @Test
  void lamCredentialsShouldBeLoaded() {
    log.info("LAM_ACCOUNT_ID = {} ", lamAccountId);
    log.info(
        "LAM_PASSWORD   = {} ",
        (lamPassword != null ? "[" + lamPassword.length() + " chars]" : "NULL"));
    assertThat(lamAccountId).as("LAM_ACCOUNT_ID doit être chargé depuis le .env").isNotBlank();
    assertThat(lamPassword).as("LAM_PASSWORD doit être chargé depuis le .env").isNotBlank();
  }

  @Test
  void keycloakCredentialsShouldBeLoaded() {
    log.info("KEYCLOAK_CLIENT_ID     {} ", keycloakClientId);
    log.info(
        "KEYCLOAK_CLIENT_SECRET = {} ",
        (keycloakClientSecret != null ? "[" + keycloakClientSecret.length() + " chars]" : "NULL"));
    assertThat(keycloakClientId)
        .as("keycloak.client-id doit être chargé depuis application-local.yml")
        .isNotBlank();
    assertThat(keycloakClientSecret)
        .as("keycloak.client-secret doit être chargé depuis application-local.yml")
        .isNotBlank();
  }
}
