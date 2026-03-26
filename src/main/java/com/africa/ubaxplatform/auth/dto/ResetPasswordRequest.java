package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de réinitialisation directe du mot de passe (admin uniquement).
 *
 * <p>Permet à un administrateur de forcer un nouveau mot de passe pour un utilisateur via l'API
 * Admin Keycloak, sans passer par le flux email.
 */
@Getter
@Setter
public class ResetPasswordRequest {

  @NotBlank(message = "L'identifiant Keycloak est obligatoire")
  private String keycloakId;

  @NotBlank(message = "Le nouveau mot de passe est obligatoire")
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String newPassword;

  /** Si {@code true}, l'utilisateur devra changer son mot de passe à la prochaine connexion. */
  private boolean temporary = false;
}
