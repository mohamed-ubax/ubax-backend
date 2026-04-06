package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Connexion via numéro de téléphone et mot de passe (flow mobile). */
@Getter
@Setter
public class PhoneLoginRequest {

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;

  @NotBlank(message = "Le mot de passe est obligatoire")
  private String password;
}
