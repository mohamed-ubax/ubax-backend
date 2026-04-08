package com.africa.ubaxplatform.partner.dto;

import com.africa.ubaxplatform.partner.codeList.PartnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

  @NotNull(message = "Le type de partenaire est obligatoire")
  private PartnerType partnerType;

  @NotBlank(message = "La raison sociale est obligatoire")
  @Size(max = 200, message = "La raison sociale ne peut dépasser 200 caractères")
  private String companyName;

  @NotBlank(message = "Le représentant légal est obligatoire")
  @Size(max = 200, message = "Le nom du représentant ne peut dépasser 200 caractères")
  private String legalRepresentative;

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

  @Size(max = 100, message = "Le statut juridique ne peut dépasser 100 caractères")
  private String legalStatus;

  @Size(max = 100, message = "Le numéro d'immatriculation ne peut dépasser 100 caractères")
  private String registrationNumber;

  /** URL du fichier RCCM après upload dans MinIO ({@code partner-documents}). */
  private String rccmUrl;

  /** URL du fichier DFE après upload dans MinIO ({@code partner-documents}). */
  private String dfeUrl;

  /** URL du contrat de bail après upload dans MinIO ({@code partner-documents}). */
  private String bailUrl;

  /** URL du logo après upload dans MinIO ({@code partner-documents}). */
  private String logoUrl;
}
