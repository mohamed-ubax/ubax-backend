package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de planification d'une intervention technique sur un ticket.
 *
 * <p>Passe le ticket au statut {@code TECHNICIAN_SENT} et notifie le locataire de la date et du
 * prestataire mandaté.
 */
@Getter
@Setter
public class ScheduleInterventionRequest {

  @NotBlank(message = "Le nom du technicien est obligatoire")
  @Schema(description = "Nom du prestataire ou technicien mandaté", example = "Mamadou Diallo")
  private String technicianName;

  @NotBlank(message = "Le téléphone du technicien est obligatoire")
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format invalide. Ex : +2250712345678")
  @Schema(description = "Numéro international du technicien", example = "+2250712345678")
  private String technicianPhone;

  @NotNull(message = "La date d'intervention est obligatoire")
  @Future(message = "La date d'intervention doit être dans le futur")
  @Schema(
      description = "Date et heure planifiées pour l'intervention",
      example = "2026-04-25T10:00:00")
  private LocalDateTime interventionScheduledAt;
}
