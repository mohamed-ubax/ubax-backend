package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.LoginRequest;
import com.africa.ubaxplatform.auth.dto.LoginResponse;
import com.africa.ubaxplatform.auth.dto.LogoutRequest;
import com.africa.ubaxplatform.auth.dto.PhoneLoginRequest;
import com.africa.ubaxplatform.auth.service.impl.KeycloakAuthServiceImpl;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.testHelper.RestClientStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Tests unitaires de KeycloakAuthServiceImpl.
 *
 * <p>Stratégie : RETURNS_SELF sur les specs builder — chaque méthode intermédiaire (contentType,
 * body, header...) retourne le mock lui-même. On surcharge uniquement retrieve() (changement de
 * type vers ResponseSpec). LENIENT évite UnnecessaryStubbingException sur les props non consommées
 * par certains tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakAuthServiceImpl – tests unitaires")
class KeycloakAuthServiceImplTest {

  @Mock private KeycloakProperties props;
  @Mock private KeycloakAdminService keycloakAdminService;

  private KeycloakAuthServiceImpl service;
  private RestClient mockRestClient;

  @BeforeEach
  void setUp() {
    service = new KeycloakAuthServiceImpl(props, keycloakAdminService);
    mockRestClient = mock(RestClient.class);
    ReflectionTestUtils.setField(service, "restClient", mockRestClient);

    doReturn("http://keycloak:8080").when(props).getAuthServerUrl();
    doReturn("ubax").when(props).getRealm();
    doReturn("ubax-client").when(props).getClientId();
    doReturn("secret").when(props).getClientSecret();
  }

  private RestClient.ResponseSpec stubPostChain() {
    return RestClientStubHelper.stubPostChain(mockRestClient);
  }

  @Nested
  @DisplayName("login()")
  class Login {

    @Test
    @DisplayName("Succès – retourne LoginResponse avec access_token")
    void login_success_returnsLoginResponse() throws CustomException {
      LoginResponse expected =
          LoginResponse.builder()
              .accessToken("access-token-abc")
              .refreshToken("refresh-token-xyz")
              .expiresIn(300)
              .tokenType("Bearer")
              .build();

      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doReturn(expected).when(retrieveSpec).body(LoginResponse.class);

      LoginRequest request = new LoginRequest();
      request.setEmail("user@example.com");
      request.setPassword("Password123!");

      LoginResponse result = service.login(request);

      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("access-token-abc");
      assertThat(result.getRefreshToken()).isEqualTo("refresh-token-xyz");
      assertThat(result.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Échec – 401 Unauthorized → CustomException USER_INVALID_CREDENTIALS")
    void login_unauthorized401_throwsCustomException() {
      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.UNAUTHORIZED,
                  "Unauthorized",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(retrieveSpec)
          .body(LoginResponse.class);

      LoginRequest request = new LoginRequest();
      request.setEmail("bad@example.com");
      request.setPassword("WrongPass");

      assertThatThrownBy(() -> service.login(request))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Échec – 400 Bad Request → CustomException USER_INVALID_CREDENTIALS")
    void login_badRequest400_throwsCustomException() {
      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.BAD_REQUEST,
                  "Bad Request",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(retrieveSpec)
          .body(LoginResponse.class);

      LoginRequest request = new LoginRequest();
      request.setEmail("bad@example.com");
      request.setPassword("WrongPass");

      assertThatThrownBy(() -> service.login(request))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_INVALID_CREDENTIALS);
    }
  }

  @Nested
  @DisplayName("loginByPhone()")
  class LoginByPhone {

    @Test
    @DisplayName("Succès – numéro trouvé dans Keycloak, token retourné")
    void loginByPhone_success_returnsLoginResponse() throws CustomException {
      String phone = "+2250722222222";
      String username = "27022222222";
      when(keycloakAdminService.findUsernameByPhone(phone)).thenReturn(username);

      LoginResponse expected =
          LoginResponse.builder().accessToken("phone-token-xyz").expiresIn(300).build();

      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doReturn(expected).when(retrieveSpec).body(LoginResponse.class);

      PhoneLoginRequest request = new PhoneLoginRequest();
      request.setPhone(phone);
      request.setPassword("Password123!");

      LoginResponse result = service.loginByPhone(request);

      assertThat(result).isNotNull();
      assertThat(result.getAccessToken()).isEqualTo("phone-token-xyz");
    }

    @Test
    @DisplayName("Échec – numéro introuvable → CustomException USER_INVALID_CREDENTIALS")
    void loginByPhone_phoneNotFound_throwsCustomException() {
      String phone = "+2250733333333";
      when(keycloakAdminService.findUsernameByPhone(phone))
          .thenThrow(new NotFoundException("Aucun utilisateur"));

      PhoneLoginRequest request = new PhoneLoginRequest();
      request.setPhone(phone);
      request.setPassword("Password123!");

      assertThatThrownBy(() -> service.loginByPhone(request))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_INVALID_CREDENTIALS);

      verify(mockRestClient, never()).post();
    }

    @Test
    @DisplayName("Échec – mot de passe incorrect → CustomException USER_INVALID_CREDENTIALS")
    void loginByPhone_wrongPassword_throwsCustomException() {
      String phone = "+2250744444444";
      when(keycloakAdminService.findUsernameByPhone(phone)).thenReturn("27044444444");

      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.UNAUTHORIZED,
                  "Unauthorized",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(retrieveSpec)
          .body(LoginResponse.class);

      PhoneLoginRequest request = new PhoneLoginRequest();
      request.setPhone(phone);
      request.setPassword("WrongPass");

      assertThatThrownBy(() -> service.loginByPhone(request))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_INVALID_CREDENTIALS);
    }
  }

  @Nested
  @DisplayName("logout()")
  class Logout {

    @Test
    @DisplayName("Succès – token révoqué sans exception")
    void logout_success_noException() throws CustomException {
      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doReturn(ResponseEntity.ok().build()).when(retrieveSpec).toBodilessEntity();

      LogoutRequest request = new LogoutRequest();
      request.setRefreshToken("valid-refresh-token");

      service.logout(request);
    }

    @Test
    @DisplayName("Échec – token invalide → CustomException")
    void logout_invalidToken_throwsCustomException() {
      RestClient.ResponseSpec retrieveSpec = stubPostChain();
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.BAD_REQUEST,
                  "Bad Request",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(retrieveSpec)
          .toBodilessEntity();

      LogoutRequest request = new LogoutRequest();
      request.setRefreshToken("expired-refresh-token");

      assertThatThrownBy(() -> service.logout(request)).isInstanceOf(CustomException.class);
    }
  }
}
