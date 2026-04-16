package com.africa.ubaxplatform.testHelper.container;

import com.africa.ubaxplatform.auth.config.KeycloakProperties;
import com.africa.ubaxplatform.auth.service.impl.KeycloakAdminServiceImpl;
import io.minio.MinioClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.MinioMockConfig.class)
public abstract class AbstractIntegrationTest {

  @TestConfiguration
  static class MinioMockConfig {
    @Bean
    public MinioClient minioClient() {
      return MinioClient.builder()
          .endpoint("http://localhost:9010")
          .credentials("minioadmin", "minioadmin123")
          .build();
    }

    @Bean
    @Primary
    public KeycloakAdminServiceImpl keycloakAdminService(KeycloakProperties props) {
      return new KeycloakAdminServiceImpl(props) {
        @Override
        protected String getAdminToken() {
          org.springframework.util.LinkedMultiValueMap<String, String> form =
              new org.springframework.util.LinkedMultiValueMap<>();
          form.add("grant_type", "password");
          form.add("client_id", "admin-cli");
          form.add("username", "admin");
          form.add("password", "admin");

          java.util.Map<String, Object> response =
              org.springframework.web.client.RestClient.create()
                  .post()
                  .uri(props.getAuthServerUrl() + "/realms/master/protocol/openid-connect/token")
                  .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                  .body(form)
                  .retrieve()
                  .body(new org.springframework.core.ParameterizedTypeReference<>() {});

          if (response == null || !response.containsKey("access_token")) {
            throw new com.africa.ubaxplatform.common.exception.TokenRetrievalException(
                "Impossible d'obtenir le token admin Keycloak");
          }
          return (String) response.get("access_token");
        }
      };
    }
  }
}
