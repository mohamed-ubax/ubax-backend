package com.africa.ubaxplatform.partner.dto;

import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête d'un administrateur pour statuer sur une demande d'adhésion partenaire.
 *
 * <p>Statuts autorisés : {@code UNDER_REVIEW}, {@code APPROVED}, {@code REJECTED}, {@code
 * INCOMPLETE}. La transition {@code PENDING} est gérée automatiquement à la soumission.
 */
@Getter
@Setter
public class ApplicationDecisionRequest {

  @NotNull(message = "Le nouveau statut est obligatoire")
  @Schema(
      description = "Nouveau statut à appliquer. PENDING est interdit (réservé à la soumission).",
      type = "string",
      allowableValues = {"UNDER_REVIEW", "APPROVED", "REJECTED", "INCOMPLETE"},
      example = "APPROVED")
  private ApplicationStatus newStatus;

  /** Commentaire de l'administrateur (obligatoire pour REJECTED et INCOMPLETE). */
  @Schema(
      description = "Motif de la décision. Obligatoire si newStatus est REJECTED ou INCOMPLETE.",
      example = "Dossier RCCM manquant ou illisible")
  private String comment;
}
