package com.africa.ubaxplatform.bailleur.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BailleurApplyRequest {

  @NotNull(message = "L'identifiant de l'agence est obligatoire")
  private UUID agencyId;

  @NotBlank(message = "Le prénom est obligatoire")
  @Size(max = 100)
  private String firstName;

  @NotBlank(message = "Le nom est obligatoire")
  @Size(max = 100)
  private String lastName;

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;

  @NotBlank(message = "L'email est obligatoire")
  @Email(message = "Format d'email invalide")
  private String email;

  @Schema(
      description =
          "Type de pièce d'identité — valeurs disponibles via GET /v1/code-list/type/ID_TYPE",
      example = "CNI",
      allowableValues = {"CNI", "PASSEPORT", "PERMIS_CONDUIRE", "TITRE_SEJOUR", "CARTE_CONSULAIRE"})
  @NotBlank(message = "Le type de pièce d'identité est obligatoire")
  private String idType;

  @Schema(
      description = "Numéro de la pièce d'identité correspondant au type sélectionné",
      example = "CI-1234567890")
  @NotBlank(message = "Le numéro de pièce d'identité est obligatoire")
  @Size(max = 100)
  private String idNumber;

  @NotEmpty(message = "Au moins un bien est requis")
  @Valid
  private List<BailleurPropertyRequest> properties;
}
