package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Requête de déconnexion – révoque le refresh token auprès de Keycloak. */
@Getter
@Setter
public class LogoutRequest {

  @NotBlank(message = "Le refresh token est obligatoire")
  private String refreshToken;
}
