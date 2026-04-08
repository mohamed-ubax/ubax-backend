package com.africa.ubaxplatform.partner.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Journal d'audit des changements de statut d'une {@link PartnerApplication}.
 *
 * <p>Une ligne est insérée à chaque transition de statut, capturant l'auteur du changement, le
 * statut précédent, le nouveau statut et un commentaire optionnel.
 */
@Entity
@Table(name = "application_status_logs", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ApplicationStatusLog extends BaseEntity {

  /** Demande d'adhésion concernée par cette transition. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id", nullable = false)
  private PartnerApplication application;

  /** Statut avant la transition (null pour la première entrée PENDING). */
  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 20)
  private ApplicationStatus previousStatus;

  /** Statut après la transition. */
  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 20)
  private ApplicationStatus newStatus;

  /** Utilisateur administrateur ayant effectué le changement (null = soumission initiale). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by_user_id")
  private User changedBy;

  /** Commentaire libre de l'administrateur (motif, retour partenaire…). */
  @Column(name = "comment", columnDefinition = "TEXT")
  private String comment;

  /** Horodatage de la transition. */
  @Column(name = "changed_at", nullable = false)
  private java.time.LocalDateTime changedAt;
}
