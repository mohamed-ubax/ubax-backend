package com.africa.ubaxplatform.tenant.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.tenant.codeList.EmploymentStatus;
import com.africa.ubaxplatform.tenant.codeList.IdDocumentType;
import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Dossier locataire sur la plateforme UBAX.
 *
 * <p>Un {@code Tenant} est la représentation métier d'un locataire, distincte de son compte {@link
 * User}. Cette séparation est intentionnelle :
 *
 * <ul>
 *   <li>{@link User} gère l'identité, l'authentification (Keycloak) et les préférences.
 *   <li>{@code Tenant} porte le dossier locatif : solvabilité, documents KYC, statut de
 *       qualification, historique de location.
 * </ul>
 *
 * Un utilisateur peut avoir le rôle {@code CLIENT} et devenir {@code Tenant} uniquement lorsqu'il
 * soumet un dossier de location.
 *
 * <p><b>Dossier locataire :</b> regroupe toutes les pièces justificatives nécessaires à la
 * constitution d'un dossier de location (pièce d'identité, justificatif de revenus, garant...)
 * stockées dans MinIO bucket {@code tenant-documents}.
 *
 * <p><b>KYC et solvabilité :</b> le gestionnaire ou l'agent peut qualifier le dossier ({@code
 * qualified}) après vérification des justificatifs. Un dossier qualifié débloque la signature du
 * bail.
 *
 * <p><b>Relation avec Contract :</b> un {@code Tenant} peut avoir plusieurs contrats successifs
 * (déménagements) mais un seul contrat actif à la fois. La relation est portée par {@code
 * Contract.tenant}.
 */
@Entity
@Table(
    name = "tenants",
    schema = "administrative",
    uniqueConstraints = {@UniqueConstraint(name = "uq_tenant_user_id", columnNames = "user_id")},
    indexes = {
      @Index(name = "idx_tenant_user", columnList = "user_id"),
      @Index(name = "idx_tenant_status", columnList = "status"),
      @Index(name = "idx_tenant_qualified", columnList = "is_qualified"),
      @Index(name = "idx_tenant_deleted_at", columnList = "deleted_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tenant extends BaseEntity {

  /**
   * Compte utilisateur UBAX associé à ce dossier locataire. Relation 1-1 : un utilisateur ne peut
   * avoir qu'un seul dossier locataire. C'est via ce lien que l'on récupère les informations
   * d'identité (nom, email, téléphone) et l'avatar MinIO.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tenant_user"))
  private User user;

  /**
   * Situation professionnelle du locataire. Valeurs : {@code EMPLOYEE | SELF_EMPLOYED | STUDENT |
   * RETIRED | UNEMPLOYED | OTHER} Utilisée pour évaluer la solvabilité du dossier.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "employment_status", length = 30)
  private EmploymentStatus employmentStatus;

  /**
   * Nom de l'employeur ou de l'entreprise du locataire. Renseigné pour les statuts {@code EMPLOYEE}
   * et {@code SELF_EMPLOYED}.
   */
  @Column(name = "employer_name", length = 200)
  private String employerName;

  /**
   * Revenus mensuels nets déclarés par le locataire (XOF). Utilisés pour vérifier la règle du tiers
   * (loyer ≤ 1/3 des revenus) lors de la qualification du dossier.
   */
  @Column(name = "monthly_income", precision = 15, scale = 2)
  private java.math.BigDecimal monthlyIncome;

  /**
   * {@code true} si le locataire a un garant pour son dossier. La présence d'un garant peut être
   * obligatoire selon les critères du propriétaire (revenus insuffisants, étudiant, etc.).
   */
  @Column(name = "has_guarantor", nullable = false)
  @Builder.Default
  private boolean hasGuarantor = false;

  /** Nom complet du garant. */
  @Column(name = "guarantor_name", length = 200)
  private String guarantorName;

  /** Numéro de téléphone du garant au format international. */
  @Column(name = "guarantor_phone", length = 20)
  private String guarantorPhone;

  /** Adresse email du garant. Utilisée pour l'envoi du contrat de cautionnement à signer. */
  @Column(name = "guarantor_email", length = 150)
  private String guarantorEmail;

  /**
   * URL de la pièce d'identité (CNI, passeport) dans MinIO. Bucket : {@code tenant-documents}
   * Format : {@code http://minio:9000/tenant-documents/{tenantId}/id-card.pdf}
   */
  @Column(name = "id_document_url", length = 500)
  private String idDocumentUrl;

  /**
   * Type de pièce d'identité fournie. Valeurs : {@code CNI | PASSPORT | RESIDENCE_PERMIT |
   * DRIVER_LICENSE}
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "id_document_type", length = 30)
  private IdDocumentType idDocumentType;

  /** Numéro de la pièce d'identité. Conservé pour les contrats et les vérifications AML. */
  @Column(name = "id_document_number", length = 100)
  private String idDocumentNumber;

  /**
   * Date d'expiration de la pièce d'identité. Une pièce expirée bloque la validation du dossier.
   */
  @Column(name = "id_document_expiry")
  private LocalDate idDocumentExpiry;

  /**
   * URL du justificatif de revenus dans MinIO (bulletin de salaire, relevé bancaire...). Bucket :
   * {@code tenant-documents}
   */
  @Column(name = "income_proof_url", length = 500)
  private String incomeProofUrl;

  /**
   * URL du justificatif de domicile actuel dans MinIO (facture, attestation...). Bucket : {@code
   * tenant-documents}
   */
  @Column(name = "address_proof_url", length = 500)
  private String addressProofUrl;

  /**
   * Statut global du dossier locataire dans son cycle de vie.
   *
   * <ul>
   *   <li>{@code INCOMPLETE} — dossier en cours de constitution, documents manquants.
   *   <li>{@code PENDING_REVIEW} — dossier complet, en attente de vérification par le gestionnaire.
   *   <li>{@code QUALIFIED} — dossier validé, le locataire peut signer un bail.
   *   <li>{@code REJECTED} — dossier refusé (voir {@code rejectionReason}).
   *   <li>{@code BLACKLISTED} — locataire banni suite à des impayés ou incidents graves.
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private TenantStatus status = TenantStatus.INCOMPLETE;

  /**
   * {@code true} si le dossier a été validé par le gestionnaire ou l'agent. Condition nécessaire
   * pour déclencher la génération et la signature du bail.
   */
  @Column(name = "is_qualified", nullable = false)
  @Builder.Default
  private boolean qualified = false;

  /** Date et heure de qualification du dossier par le gestionnaire. */
  @Column(name = "qualified_at")
  private LocalDateTime qualifiedAt;

  /**
   * Utilisateur (agent ou gestionnaire) ayant qualifié le dossier. Traçabilité de la validation
   * pour l'audit.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "qualified_by", foreignKey = @ForeignKey(name = "fk_tenant_qualified_by"))
  private User qualifiedBy;

  /**
   * Motif de refus du dossier, communiqué au locataire. Exemple : {@code "Revenus insuffisants par
   * rapport au loyer demandé."}
   */
  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  /**
   * Note interne de l'agent sur le locataire (non visible par le locataire). Exemple : {@code
   * "Locataire sérieux, références vérifiées."}
   */
  @Column(name = "internal_note", columnDefinition = "TEXT")
  private String internalNote;

  /**
   * Date et heure de suppression logique du dossier (soft delete). {@code null} = dossier actif.
   * Non null = dossier archivé. Un dossier archivé conserve l'historique des contrats associés.
   */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** {@code true} si le dossier est complet et validé pour signer un bail. */
  public boolean isReadyToSign() {
    return qualified && status == TenantStatus.QUALIFIED;
  }

  /** {@code true} si la pièce d'identité est encore valide. */
  public boolean isIdDocumentValid() {
    return idDocumentExpiry != null && idDocumentExpiry.isAfter(LocalDate.now());
  }

  /** {@code true} si le dossier a été supprimé logiquement. */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /** {@code true} si le locataire est blacklisté (impayés, incidents graves). */
  public boolean isBlacklisted() {
    return status == TenantStatus.BLACKLISTED;
  }

  /**
   * Retourne le nom complet du locataire depuis son profil utilisateur. Raccourci pratique pour les
   * templates de documents générés.
   */
  public String getFullName() {
    return user != null ? user.getFullName() : "";
  }
}
