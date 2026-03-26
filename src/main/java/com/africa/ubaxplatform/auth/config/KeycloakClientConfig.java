package com.africa.ubaxplatform.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Active le binding des propriétés Keycloak. */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakClientConfig {}
