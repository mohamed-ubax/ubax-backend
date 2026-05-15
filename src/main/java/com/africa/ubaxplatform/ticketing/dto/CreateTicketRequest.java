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
      regexp =
          "PLOMBIER|ELECTRICIEN|SERRURIER|MENUISIER|MACON|PEINTRE|CLIMATISATION|VITRERIE|JARDINAGE|NETTOYAGE|DESINSECTISATION|AUTRE",
      message =
          "Catégorie invalide. Valeurs : PLOMBIER, ELECTRICIEN, SERRURIER, MENUISIER, MACON,"
              + " PEINTRE, CLIMATISATION, VITRERIE, JARDINAGE, NETTOYAGE, DESINSECTISATION, AUTRE")
  @Schema(
      description = "Catégorie métier de l'incident (alignée avec TECHNICIEN_PROFESSION)",
      allowableValues = {
        "PLOMBIER",
        "ELECTRICIEN",
        "SERRURIER",
        "MENUISIER",
        "MACON",
        "PEINTRE",
        "CLIMATISATION",
        "VITRERIE",
        "JARDINAGE",
        "NETTOYAGE",
        "DESINSECTISATION",
        "AUTRE"
      },
      example = "PLOMBIER")
  private String category;

  @NotBlank(message = "Le titre est obligatoire")
  @Size(max = 255, message = "Le titre ne peut dépasser 255 caractères")
  @Schema(description = "Titre court de l'incident", example = "Fuite sous l'évier de la cuisine")
  private String title;

  @NotBlank(message = "La description est obligatoire")
  @Size(min = 20, message = "La description doit contenir au moins 20 caractères")
  @Schema(
      description = "Description détaillée de l'incident (20 caractères minimum)",
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
