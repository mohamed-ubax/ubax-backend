package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.service.impl.KeycloakAdminServiceImpl;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.testHelper.RestClientStubHelper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeycloakAdminServiceImpl - tests unitaires")
class KeycloakAdminServiceImplTest {

  @Mock private KeycloakProperties props;

  private KeycloakAdminServiceImpl service;
  private RestClient mockRestClient;

  @BeforeEach
  void setUp() {
    service = new KeycloakAdminServiceImpl(props);
    mockRestClient = mock(RestClient.class);
    ReflectionTestUtils.setField(service, "restClient", mockRestClient);

    doReturn("http://keycloak:8080").when(props).getAuthServerUrl();
    doReturn("ubax").when(props).getRealm();
    doReturn("ubax-client").when(props).getClientId();
    doReturn("secret").when(props).getClientSecret();
  }

  @Nested
  @DisplayName("createUser()")
  class CreateUser {

    private RegisterCompleteRequest buildRequest() {
      RegisterCompleteRequest req = new RegisterCompleteRequest();
      req.setPhone("+2250755555555");
      req.setEmail("partner@example.com");
      req.setFirstName("Aya");
      req.setLastName("Kone");
      req.setPassword("Password123!");
      req.setTitle("Mme");
      return req;
    }

    @Test
    @DisplayName("Succès - retourne l'ID extrait du header Location")
    void createUser_success_returnsKeycloakId() throws CustomException {
      RestClient.ResponseSpec adminRetrieve =
          RestClientStubHelper.stubTokenThenAdminPost(mockRestClient);
      String newUserId = "kc-user-uuid-123";
      doReturn(
              ResponseEntity.created(
                      URI.create("http://keycloak:8080/admin/realms/ubax/users/" + newUserId))
                  .build())
          .when(adminRetrieve)
          .toBodilessEntity();

      assertThat(service.createUser(buildRequest())).isEqualTo(newUserId);
    }

    @Test
    @DisplayName("Échec - 409 Conflict → CustomException USER_CREATE_FAILURE_ALREADY_EXISTS")
    void createUser_conflict409_throwsCustomException() {
      RestClient.ResponseSpec adminRetrieve =
          RestClientStubHelper.stubTokenThenAdminPost(mockRestClient);
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.CONFLICT,
                  "Conflict",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(adminRetrieve)
          .toBodilessEntity();

      assertThatThrownBy(() -> service.createUser(buildRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
    }
  }

  @Nested
  @DisplayName("findUserIdByPhone()")
  class FindUserIdByPhone {

    @Test
    @DisplayName("Succès - retourne l'ID Keycloak")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findUserIdByPhone_found_returnsId() {
      String phone = "+2250766666666";
      String userId = "kc-id-abc";

      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(List.of(Map.of("id", userId, "username", phone)))
          .when(getRetrieve)
          .body(any(ParameterizedTypeReference.class));

      assertThat(service.findUserIdByPhone(phone)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Échec - aucun utilisateur trouvé → NotFoundException")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findUserIdByPhone_notFound_throwsNotFoundException() {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(List.of()).when(getRetrieve).body(any(ParameterizedTypeReference.class));

      assertThatThrownBy(() -> service.findUserIdByPhone("+2250777777777"))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findUsernameByPhone()")
  class FindUsernameByPhone {

    @Test
    @DisplayName("Succès - retourne le username Keycloak")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findUsernameByPhone_found_returnsUsername() {
      String phone = "+2250788888888";
      String username = "27088888888";

      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(List.of(Map.of("id", "some-id", "username", username)))
          .when(getRetrieve)
          .body(any(ParameterizedTypeReference.class));

      assertThat(service.findUsernameByPhone(phone)).isEqualTo(username);
    }

    @Test
    @DisplayName("Échec - aucun utilisateur trouvé → NotFoundException")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void findUsernameByPhone_notFound_throwsNotFoundException() {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(List.of()).when(getRetrieve).body(any(ParameterizedTypeReference.class));

      assertThatThrownBy(() -> service.findUsernameByPhone("+2250799999999"))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("resetPassword()")
  class ResetPassword {

    @Test
    @DisplayName("Succès - pas d'exception levée")
    void resetPassword_success_noException() throws CustomException {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec putRetrieve = RestClientStubHelper.stubPutChain(mockRestClient);
      doReturn(ResponseEntity.noContent().build()).when(putRetrieve).toBodilessEntity();

      service.resetPassword("kc-user-id", "NewPass123!", false);
    }

    @Test
    @DisplayName("Échec - 404 → CustomException USER_NOT_FOUND")
    void resetPassword_userNotFound_throwsCustomException() {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec putRetrieve = RestClientStubHelper.stubPutChain(mockRestClient);
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.NOT_FOUND,
                  "Not Found",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(putRetrieve)
          .toBodilessEntity();

      assertThatThrownBy(() -> service.resetPassword("unknown-kc-id", "NewPass123!", false))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("assignRole()")
  class AssignRole {

    @Test
    @DisplayName("Succès - rôle assigné sans exception")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assignRole_success_noException() throws CustomException {
      RestClient.ResponseSpec adminRetrieve =
          RestClientStubHelper.stubTokenThenAdminPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(Map.of("id", "role-uuid", "name", "UBAX_CLIENT"))
          .when(getRetrieve)
          .body(any(ParameterizedTypeReference.class));
      doReturn(ResponseEntity.noContent().build()).when(adminRetrieve).toBodilessEntity();

      service.assignRole("kc-user-id", UserRole.CLIENT);
    }

    @Test
    @DisplayName("Échec - 404 → CustomException USER_NOT_FOUND")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void assignRole_notFound_throwsCustomException() {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doThrow(
              HttpClientErrorException.create(
                  HttpStatus.NOT_FOUND,
                  "Not Found",
                  new org.springframework.http.HttpHeaders(),
                  null,
                  null))
          .when(getRetrieve)
          .body(any(ParameterizedTypeReference.class));

      assertThatThrownBy(() -> service.assignRole("kc-user-id", UserRole.CLIENT))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getRoles()")
  class GetRoles {

    @Test
    @DisplayName("Succès - retourne la liste des rôles")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getRoles_success_returnsList() {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(List.of(Map.of("name", "UBAX_CLIENT"), Map.of("name", "UBAX_ADMIN")))
          .when(getRetrieve)
          .body(any(ParameterizedTypeReference.class));

      List<Map<String, Object>> result = service.getRoles();

      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(m -> m.get("name"))
          .containsExactly("UBAX_CLIENT", "UBAX_ADMIN");
    }

    @Test
    @DisplayName("Succès - réponse null → liste vide")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getRoles_nullResponse_returnsEmptyList() {
      RestClientStubHelper.stubTokenPost(mockRestClient);
      RestClient.ResponseSpec getRetrieve = RestClientStubHelper.stubGetChain(mockRestClient);
      doReturn(null).when(getRetrieve).body(any(ParameterizedTypeReference.class));

      assertThat(service.getRoles()).isEmpty();
    }
  }
}
