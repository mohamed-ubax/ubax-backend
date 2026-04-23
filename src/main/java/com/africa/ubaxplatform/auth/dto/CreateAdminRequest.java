package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de création d'un administrateur interne UBAX.
 *
 * <p>Soumis via le formulaire du back-office par un Super Administrateur. Déclenche la création du
 * compte Keycloak, l'attribution du rôle, la synchronisation en base et l'envoi d'un email
 * permettant à l'agent de définir son mot de passe.
 *
 * <p>Le rôle doit obligatoirement être {@code ADMIN} ou {@code SUPER_ADMIN}. Toute autre valeur
 * sera rejetée avec une erreur métier.
 */
@Getter
@Setter
public class CreateAdminRequest {

  @NotBlank(message = "Le prénom est obligatoire")
  @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
  private String firstName;

  @NotBlank(message = "Le nom est obligatoire")
  @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
  private String lastName;

  /**
   * Adresse email professionnelle de l'administrateur. Sert de username Keycloak et de destination
   * pour l'email de définition du mot de passe.
   */
  @NotBlank(message = "L'email est obligatoire")
  @Email(message = "Format d'email invalide")
  private String email;

  /** Numéro de téléphone au format international (optionnel). Ex: {@code +2250712345678} */
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;

  /**
   * Rôle à attribuer. Valeurs autorisées : {@code ADMIN}, {@code SUPER_ADMIN}.
   *
   * <p>Toute autre valeur de {@link UserRole} sera rejetée par le service.
   */
  @NotNull(message = "Le rôle est obligatoire")
  private UserRole role;
}
