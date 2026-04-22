package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Formulaire d'invitation d'un membre dans une équipe agence ou hôtel. */
@Getter
@Setter
public class AddTeamMemberRequest {

  @NotBlank(message = "Le prénom est obligatoire")
  @Size(min = 2, max = 100)
  private String firstName;

  @NotBlank(message = "Le nom est obligatoire")
  @Size(min = 2, max = 100)
  private String lastName;

  @NotBlank(message = "L'email est obligatoire")
  @Email(message = "Format d'email invalide")
  private String email;

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;

  /** Sous-rôles à assigner immédiatement (optionnel). */
  private List<String> subRoles;
}
