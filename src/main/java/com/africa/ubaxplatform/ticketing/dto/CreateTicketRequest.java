package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de création d'un ticket de maintenance ou SAV.
 *
 * <p>Le ticket est déclaré par un membre de l'agence dans le cadre d'un contrat actif. Le reporter
 * est automatiquement déduit du JWT de l'appelant.
 */
@Getter
@Setter
public class CreateTicketRequest {

  @NotNull(message = "L'identifiant du contrat est obligatoire")
  @Schema(description = "Identifiant du contrat dans le cadre duquel l'incident est signalé")
  private UUID contractId;

  @NotBlank(message = "La catégorie est obligatoire")
  @Pattern(
      regexp = "LEAK|ELECTRICAL|LOCK|PLUMBING|APPLIANCE|STRUCTURE|PEST|COMMON_AREA|OTHER",
      message =
          "Catégorie invalide. Valeurs : LEAK, ELECTRICAL, LOCK, PLUMBING, APPLIANCE, STRUCTURE,"
              + " PEST, COMMON_AREA, OTHER")
  @Schema(
      description = "Catégorie de l'incident",
      allowableValues = {
        "LEAK",
        "ELECTRICAL",
        "LOCK",
        "PLUMBING",
        "APPLIANCE",
        "STRUCTURE",
        "PEST",
        "COMMON_AREA",
        "OTHER"
      },
      example = "LEAK")
  private String category;

  @NotBlank(message = "Le titre est obligatoire")
  @Size(max = 255, message = "Le titre ne peut dépasser 255 caractères")
  @Schema(description = "Titre court de l'incident", example = "Fuite sous l'évier de la cuisine")
  private String title;

  @NotBlank(message = "La description est obligatoire")
  @Schema(
      description = "Description détaillée de l'incident",
      example = "Il y a une fuite d'eau sous l'évier depuis ce matin. L'eau coule en continu.")
  private String description;

  @Pattern(
      regexp = "LOW|NORMAL|HIGH|URGENT",
      message = "Priorité invalide. Valeurs : LOW, NORMAL, HIGH, URGENT")
  @Schema(
      description = "Niveau d'urgence (défaut : NORMAL)",
      allowableValues = {"LOW", "NORMAL", "HIGH", "URGENT"},
      example = "HIGH")
  private String priority;
}
