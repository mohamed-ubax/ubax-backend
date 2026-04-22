package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** Requête d'ajout d'un message dans le fil de discussion d'un ticket. */
@Getter
@Setter
public class AddTicketMessageRequest {

  @NotBlank(message = "Le message ne peut pas être vide")
  @Schema(
      description = "Contenu du message",
      example = "Pouvez-vous préciser depuis combien de temps la fuite est présente ?")
  private String message;

  @Pattern(regexp = "PUBLIC|INTERNAL", message = "Valeurs acceptées : PUBLIC, INTERNAL")
  @Schema(
      description =
          "Visibilité du message : PUBLIC (visible par toutes les parties) ou INTERNAL (note"
              + " interne équipe SAV uniquement). Défaut : PUBLIC",
      allowableValues = {"PUBLIC", "INTERNAL"},
      example = "PUBLIC")
  private String messageType;
}
