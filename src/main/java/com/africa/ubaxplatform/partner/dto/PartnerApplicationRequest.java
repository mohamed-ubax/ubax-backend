package com.africa.ubaxplatform.partner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Corps de la requête publique de soumission d'une demande d'adhésion partenaire.
 *
 * <p>Accessible sans authentification : {@code POST /v1/partner/apply}.
 */
@Getter
@Setter
public class PartnerApplicationRequest {

  @NotBlank(message = "Le type de partenaire est obligatoire")
  private String partnerType;

  @Schema(
      description = "Raison sociale ou dénomination commerciale",
      example = "Agence Immobilière Teranga")
  @NotBlank(message = "La raison sociale est obligatoire")
  @Size(max = 200, message = "La raison sociale ne peut dépasser 200 caractères")
  private String companyName;

  @Schema(description = "Prénom du représentant légal", example = "Mohamed")
  @NotBlank(message = "Le prénom du représentant légal est obligatoire")
  @Size(max = 100, message = "Le prénom du représentant ne peut dépasser 100 caractères")
  private String legalRepFirstName;

  @Schema(description = "Nom de famille du représentant légal", example = "Ba")
  @NotBlank(message = "Le nom du représentant légal est obligatoire")
  @Size(max = 100, message = "Le nom du représentant ne peut dépasser 100 caractères")
  private String legalRepLastName;

  @NotBlank(message = "Le numéro de téléphone est obligatoire")
  @Pattern(
      regexp = "^\\+[1-9]\\d{7,14}$",
      message = "Format invalide. Utilisez le format international ex: +2250712345678")
  private String phone;

  @NotBlank(message = "L'adresse email est obligatoire")
  @Email(message = "Format d'email invalide")
  private String email;

  @NotBlank(message = "Le pays est obligatoire")
  @Size(min = 2, max = 5, message = "Code pays ISO invalide (ex: CI, SN)")
  private String country;

  @NotBlank(message = "La ville est obligatoire")
  @Size(max = 100, message = "La ville ne peut dépasser 100 caractères")
  private String city;

  @Size(max = 500, message = "L'adresse postale ne peut dépasser 500 caractères")
  private String postalAddress;

  @Size(max = 150, message = "La zone ne peut dépasser 150 caractères")
  private String zone;

  private String description;

  @DecimalMin(value = "-90.0", message = "Latitude invalide (min -90)")
  @DecimalMax(value = "90.0", message = "Latitude invalide (max 90)")
  private BigDecimal latitude;

  @DecimalMin(value = "-180.0", message = "Longitude invalide (min -180)")
  @DecimalMax(value = "180.0", message = "Longitude invalide (max 180)")
  private BigDecimal longitude;

  @Size(max = 100, message = "Le statut juridique ne peut dépasser 100 caractères")
  private String legalStatus;

  @Size(max = 100, message = "Le numéro d'immatriculation ne peut dépasser 100 caractères")
  private String registrationNumber;
}
