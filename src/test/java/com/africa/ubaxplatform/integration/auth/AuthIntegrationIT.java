package com.africa.ubaxplatform.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.testHelper.container.AbstractIntegrationTest;
import com.africa.ubaxplatform.testHelper.container.KeycloakTestHelper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AuthIntegrationIT extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Nested
  @DisplayName("POST /v1/auth/login")
  class LoginTests {

    @Test
    @DisplayName("Admin peut se connecter et reçoit un access token")
    void adminShouldLoginSuccessfully() {
      LoginRequest request = new LoginRequest();
      request.setEmail("admin@ubax-test.io");
      request.setPassword("admin-test-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();

      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
      assertThat(data).containsKey("access_token");
      assertThat(data).containsKey("refresh_token");
      assertThat(data.get("access_token")).isNotNull();
      assertThat(data.get("refresh_token")).isNotNull();
    }

    @Test
    @DisplayName("Agent peut se connecter")
    void agentShouldLoginSuccessfully() {
      LoginRequest request = new LoginRequest();
      request.setEmail("agent@ubax-test.io");
      request.setPassword("agent-test-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
      assertThat(data).containsKey("access_token");
    }

    @Test
    @DisplayName("Partner peut se connecter")
    void partnerShouldLoginSuccessfully() {
      LoginRequest request = new LoginRequest();
      request.setEmail("partner@ubax-test.io");
      request.setPassword("partner-test-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
      assertThat(data).containsKey("access_token");
    }

    @Test
    @DisplayName("Tenant peut se connecter")
    void tenantShouldLoginSuccessfully() {
      LoginRequest request = new LoginRequest();
      request.setEmail("tenant@ubax-test.io");
      request.setPassword("tenant-test-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
      assertThat(data).containsKey("access_token");
    }

    @Test
    @DisplayName("Owner peut se connecter")
    void ownerShouldLoginSuccessfully() {
      LoginRequest request = new LoginRequest();
      request.setEmail("owner@ubax-test.io");
      request.setPassword("owner-test-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
      assertThat(data).containsKey("access_token");
    }

    @Test
    @DisplayName("Doit retourner 401 avec un mot de passe invalide")
    void shouldFailWithInvalidPassword() {
      LoginRequest request = new LoginRequest();
      request.setEmail("admin@ubax-test.io");
      request.setPassword("wrong-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Doit retourner 401 avec un email inexistant")
    void shouldFailWithNonExistentEmail() {
      LoginRequest request = new LoginRequest();
      request.setEmail("nonexistent@ubax-test.io");
      request.setPassword("some-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Doit retourner 400 avec un email invalide")
    void shouldFailWithInvalidEmailFormat() {
      LoginRequest request = new LoginRequest();
      request.setEmail("not-an-email");
      request.setPassword("some-password");

      ResponseEntity<CustomResponse> response =
          restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  // ── Logout ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("POST /v1/auth/logout")
  class LogoutTests {

    @Test
    @DisplayName("Doit déconnecter avec un refresh token valide")
    void shouldLogoutSuccessfully() {
      LoginRequest loginRequest = new LoginRequest();
      loginRequest.setEmail("agent@ubax-test.io");
      loginRequest.setPassword("agent-test-password");

      ResponseEntity<CustomResponse> loginResponse =
          restTemplate.postForEntity("/v1/auth/login", loginRequest, CustomResponse.class);

      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(loginResponse.getBody()).isNotNull();

      @SuppressWarnings("unchecked")
      Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().getData();
      String refreshToken = (String) loginData.get("refresh_token");

      LogoutRequest logoutRequest = new LogoutRequest();
      logoutRequest.setRefreshToken(refreshToken);

      ResponseEntity<CustomResponse> logoutResponse =
          restTemplate.postForEntity("/v1/auth/logout", logoutRequest, CustomResponse.class);

      assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Le logout est idempotent — retourne 200 même avec un token déjà révoqué")
    void logoutIsIdempotent() {
      LoginRequest loginRequest = new LoginRequest();
      loginRequest.setEmail("owner@ubax-test.io");
      loginRequest.setPassword("owner-test-password");

      ResponseEntity<CustomResponse> loginResponse =
          restTemplate.postForEntity("/v1/auth/login", loginRequest, CustomResponse.class);

      @SuppressWarnings("unchecked")
      Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().getData();
      String refreshToken = (String) loginData.get("refresh_token");

      LogoutRequest logoutRequest = new LogoutRequest();
      logoutRequest.setRefreshToken(refreshToken);

      // Premier logout
      ResponseEntity<CustomResponse> firstLogout =
          restTemplate.postForEntity("/v1/auth/logout", logoutRequest, CustomResponse.class);
      assertThat(firstLogout.getStatusCode()).isEqualTo(HttpStatus.OK);

      // Deuxième logout — Keycloak 26 est idempotent
      ResponseEntity<CustomResponse> secondLogout =
          restTemplate.postForEntity("/v1/auth/logout", logoutRequest, CustomResponse.class);
      assertThat(secondLogout.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
  }

  // ── Roles ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("GET /v1/auth/roles")
  class RolesTests {

    @Test
    @DisplayName("Admin peut lister les rôles")
    void adminShouldListRoles() {
      String token = KeycloakTestHelper.getAdminToken();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);
      headers.setContentType(MediaType.APPLICATION_JSON);

      ResponseEntity<CustomResponse> response =
          restTemplate.exchange(
              "/v1/auth/roles", HttpMethod.GET, new HttpEntity<>(headers), CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> roles = (List<Map<String, Object>>) response.getBody().getData();
      assertThat(roles).isNotEmpty();
      assertThat(roles).anyMatch(r -> "UBAX_ADMIN".equals(r.get("name")));
      assertThat(roles).anyMatch(r -> "UBAX_PARTNER".equals(r.get("name")));
    }

    @Test
    @DisplayName("Requête sans token doit retourner 401")
    void shouldReturn401WithoutToken() {
      ResponseEntity<CustomResponse> response =
          restTemplate.getForEntity("/v1/auth/roles", CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Agent ne peut pas lister les rôles")
    void agentShouldNotListRoles() {
      String token = KeycloakTestHelper.getAgentToken();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);

      ResponseEntity<CustomResponse> response =
          restTemplate.exchange(
              "/v1/auth/roles", HttpMethod.GET, new HttpEntity<>(headers), CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Partner ne peut pas lister les rôles")
    void partnerShouldNotListRoles() {
      String token = KeycloakTestHelper.getPartnerToken();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);

      ResponseEntity<CustomResponse> response =
          restTemplate.exchange(
              "/v1/auth/roles", HttpMethod.GET, new HttpEntity<>(headers), CustomResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  // ── Flow complet ───────────────────────────────────────────────

  @Nested
  @DisplayName("Flow : Login → Roles → Logout")
  class FullFlowTests {

    @Test
    @DisplayName("Flow complet : login → accès rôles → logout")
    void shouldCompleteFullLoginFlow() {
      // 1. Login
      LoginRequest loginRequest = new LoginRequest();
      loginRequest.setEmail("admin@ubax-test.io");
      loginRequest.setPassword("admin-test-password");

      ResponseEntity<CustomResponse> loginResponse =
          restTemplate.postForEntity("/v1/auth/login", loginRequest, CustomResponse.class);

      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(loginResponse.getBody()).isNotNull();

      @SuppressWarnings("unchecked")
      Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().getData();
      String accessToken = (String) loginData.get("access_token");
      String refreshToken = (String) loginData.get("refresh_token");
      assertThat(accessToken).isNotBlank();
      assertThat(refreshToken).isNotBlank();

      // 2. Accès endpoint protégé
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);

      ResponseEntity<CustomResponse> rolesResponse =
          restTemplate.exchange(
              "/v1/auth/roles", HttpMethod.GET, new HttpEntity<>(headers), CustomResponse.class);

      assertThat(rolesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 3. Logout
      LogoutRequest logoutRequest = new LogoutRequest();
      logoutRequest.setRefreshToken(refreshToken);

      ResponseEntity<CustomResponse> logoutResponse =
          restTemplate.postForEntity("/v1/auth/logout", logoutRequest, CustomResponse.class);

      assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 4. Endpoint protégé sans token → 401
      ResponseEntity<CustomResponse> unauthorizedResponse =
          restTemplate.getForEntity("/v1/auth/roles", CustomResponse.class);

      assertThat(unauthorizedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Tous les rôles peuvent se connecter")
    void allRolesShouldBeAbleToLogin() {
      String[][] credentials = {
        {"admin@ubax-test.io", "admin-test-password"},
        {"agent@ubax-test.io", "agent-test-password"},
        {"partner@ubax-test.io", "partner-test-password"},
        {"tenant@ubax-test.io", "tenant-test-password"},
        {"owner@ubax-test.io", "owner-test-password"}
      };

      for (String[] cred : credentials) {
        LoginRequest request = new LoginRequest();
        request.setEmail(cred[0]);
        request.setPassword(cred[1]);

        ResponseEntity<CustomResponse> response =
            restTemplate.postForEntity("/v1/auth/login", request, CustomResponse.class);

        assertThat(response.getStatusCode())
            .as("Login failed for %s", cred[0])
            .isEqualTo(HttpStatus.OK);
      }
    }
  }
}
