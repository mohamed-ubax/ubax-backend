package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Requête de réinitialisation de mot de passe par OTP SMS (étape 3). */
@Getter
@Setter
public class ResetPasswordByPhoneRequest {

  @NotBlank
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format de téléphone invalide")
  private String phone;

  @NotBlank
  @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit être à 6 chiffres")
  private String code;

  @NotBlank
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String newPassword;
}
