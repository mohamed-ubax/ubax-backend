package com.africa.ubaxplatform.testHelper.container;

import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public final class KeycloakTestHelper {

  private static final String KEYCLOAK_URL = "http://localhost:8181";
  private static final String REALM = "ubax-plateform";
  private static final String CLIENT_ID = "ubax-client";
  private static final String CLIENT_SECRET = "test-client-secret";
  private static final RestTemplate REST = new RestTemplate();

  private KeycloakTestHelper() {}

  public static String getAccessToken(String username, String password) {
    String tokenUrl = KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", CLIENT_ID);
    body.add("client_secret", CLIENT_SECRET);
    body.add("username", username);
    body.add("password", password);

    @SuppressWarnings("unchecked")
    Map<String, Object> response =
        REST.postForObject(tokenUrl, new HttpEntity<>(body, headers), Map.class);

    if (response == null || !response.containsKey("access_token")) {
      throw new IllegalStateException("Impossible d'obtenir un token Keycloak pour : " + username);
    }

    return (String) response.get("access_token");
  }

  public static String getAdminToken() {
    return getAccessToken("admin@ubax-test.io", "admin-test-password");
  }

  public static String getAgentToken() {
    return getAccessToken("agent@ubax-test.io", "agent-test-password");
  }

  public static String getPartnerToken() {
    return getAccessToken("partner@ubax-test.io", "partner-test-password");
  }

  public static String getTenantToken() {
    return getAccessToken("tenant@ubax-test.io", "tenant-test-password");
  }

  public static String getOwnerToken() {
    return getAccessToken("owner@ubax-test.io", "owner-test-password");
  }
}
