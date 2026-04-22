package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de changement de statut d'un ticket.
 *
 * <p>Transitions valides :
 *
 * <pre>
 *  OPEN → IN_ANALYSIS | CANCELLED
 *  IN_ANALYSIS → TECHNICIAN_SENT | RESOLVED | CANCELLED
 *  TECHNICIAN_SENT → RESOLVED | CANCELLED
 *  RESOLVED → CLOSED
 * </pre>
 *
 * <p>La {@code resolutionNote} est obligatoire pour les transitions vers {@code RESOLVED} et {@code
 * CLOSED}.
 */
@Getter
@Setter
public class UpdateTicketStatusRequest {

  @NotBlank(message = "Le statut est obligatoire")
  @Pattern(
      regexp = "IN_ANALYSIS|TECHNICIAN_SENT|RESOLVED|CLOSED|CANCELLED",
      message = "Valeurs acceptées : IN_ANALYSIS, TECHNICIAN_SENT, RESOLVED, CLOSED, CANCELLED")
  @Schema(
      description = "Nouveau statut du ticket",
      allowableValues = {"IN_ANALYSIS", "TECHNICIAN_SENT", "RESOLVED", "CLOSED", "CANCELLED"},
      example = "IN_ANALYSIS")
  private String status;

  @Schema(
      description = "Note de résolution — obligatoire pour RESOLVED et CLOSED",
      example = "Fuite réparée, joint changé sous l'évier.")
  private String resolutionNote;
}
