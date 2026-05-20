package com.africa.ubaxplatform.bailleur.dto;

import com.africa.ubaxplatform.common.validation.MinWords;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BailleurApplyRequest {

  @NotNull(message = "L'identifiant de l'agence est obligatoire")
  private UUID agencyId;

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

  @Schema(
      description =
          "Email de contact. Requis uniquement si le compte CLIENT ne possède pas d'adresse email.",
      example = "bailleur@example.com")
  @Email(message = "L'adresse email n'est pas valide")
  @Size(max = 150)
  private String email;

  @Schema(
      description =
          "URL de la photo recto de la pièce d'identité — obtenue via POST /v1/storage/upload?bucket=bailleur-documents",
      example = "http://minio:9000/bailleur-documents/uuid-recto.jpg")
  private String idDocRectoUrl;

  @Schema(
      description =
          "URL de la photo verso de la pièce d'identité — obtenue via POST /v1/storage/upload?bucket=bailleur-documents",
      example = "http://minio:9000/bailleur-documents/uuid-verso.jpg")
  private String idDocVersoUrl;

  @Schema(
      description =
          "Description des biens et motivation de la demande d'adhésion à l'agence. "
              + "Obligatoire — minimum 10 mots pour permettre à l'agence d'évaluer la demande.",
      example =
          "Je suis propriétaire de trois appartements en centre-ville de Yaoundé que je souhaite confier à votre agence pour gestion locative.")
  @NotBlank(message = "La description est obligatoire")
  @MinWords(10)
  @Size(max = 1000)
  private String description;
}
