package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de demande de réinitialisation de mot de passe.
 *
 * <p>Keycloak enverra un email avec un lien de réinitialisation à l'adresse fournie.
 */
@Getter
@Setter
public class ForgotPasswordRequest {

  @NotBlank(message = "L'email est obligatoire")
  @Email(message = "Format d'email invalide")
  private String email;
}
