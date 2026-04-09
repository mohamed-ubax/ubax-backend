package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Étape 3 de l'inscription mobile : informations complètes de l'utilisateur.
 *
 * <p>Soumis après vérification OTP réussie. Le rôle est fixé à {@code CLIENT} côté serveur.
 */
@Getter
@Setter
public class RegisterCompleteRequest {

  /** Numéro de téléphone confirmé (doit correspondre au téléphone vérifié par OTP). */
  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;

  @NotBlank(message = "La civilité est obligatoire")
  private String title;

  @NotBlank(message = "Le prénom est obligatoire")
  @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
  private String firstName;

  @NotBlank(message = "Le nom est obligatoire")
  @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
  private String lastName;

  @Email(message = "Format d'email invalide")
  private String email;

  @NotBlank(message = "Le mot de passe est obligatoire")
  @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
  private String password;
}
