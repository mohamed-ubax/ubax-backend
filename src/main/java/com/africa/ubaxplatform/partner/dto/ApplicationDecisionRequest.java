package com.africa.ubaxplatform.partner.dto;

import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
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
  private ApplicationStatus newStatus;

  /** Commentaire de l'administrateur (obligatoire pour REJECTED et INCOMPLETE). */
  private String comment;
}
