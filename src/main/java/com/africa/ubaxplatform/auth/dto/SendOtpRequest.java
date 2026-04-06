package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Étape 1 de l'inscription mobile : envoi du numéro de téléphone pour recevoir le code OTP. */
@Getter
@Setter
public class SendOtpRequest {

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;
}
