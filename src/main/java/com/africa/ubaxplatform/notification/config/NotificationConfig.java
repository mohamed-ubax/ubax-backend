package com.africa.ubaxplatform.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Configuration du module notification : SMS et Email. */
@Configuration
public class NotificationConfig {

  /** RestClient dédié aux appels LAfricaMobile (distinct du RestClient Keycloak). */
  @Bean(name = "smsRestClient")
  public RestClient smsRestClient() {
    return RestClient.create();
  }
}
