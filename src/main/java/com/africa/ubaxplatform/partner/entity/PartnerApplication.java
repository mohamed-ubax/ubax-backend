package com.africa.ubaxplatform.partner.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.codeList.PartnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Demande d'adhésion partenaire soumise via le formulaire public de la plateforme UBAX.
 *
 * <p>Une demande traverse le cycle {@code PENDING → UNDER_REVIEW → APPROVED | REJECTED |
 * INCOMPLETE}. Chaque changement de statut est tracé dans {@link ApplicationStatusLog}.
 *
 * <p><b>Documents :</b> les pièces justificatives (RCCM, DFE, bail) sont stockées dans le bucket
 * MinIO {@code partner-documents}. Seules les URLs sont persistées ici.
 *
 * <p><b>Soft delete :</b> aucun – les demandes sont archivées, jamais supprimées physiquement.
 */
@Entity
@Table(name = "partner_applications", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PartnerApplication extends BaseEntity {

  // ── Identification du partenaire ───────────────────────────────

  /** Type d'activité du partenaire. */
  @Enumerated(EnumType.STRING)
  @Column(name = "partner_type", nullable = false, length = 40)
  private PartnerType partnerType;

  /** Raison sociale ou dénomination commerciale. */
  @Column(name = "company_name", nullable = false, length = 200)
  private String companyName;

  /** Nom complet du représentant légal. */
  @Column(name = "legal_representative", nullable = false, length = 200)
  private String legalRepresentative;

  /** Numéro de téléphone du représentant (format international). */
  @Column(name = "phone", nullable = false, length = 25)
  private String phone;

  /** Adresse email professionnelle du partenaire. */
  @Column(name = "email", nullable = false, length = 150)
  private String email;

  // ── Localisation ───────────────────────────────────────────────

  /** Pays (code ISO 3166-1 alpha-2, ex: CI, SN). */
  @Column(name = "country", nullable = false, length = 5)
  private String country;

  /** Ville d'implantation principale. */
  @Column(name = "city", nullable = false, length = 100)
  private String city;

  /** Adresse postale complète. */
  @Column(name = "postal_address", columnDefinition = "TEXT")
  private String postalAddress;

  /** Zone géographique / quartier de la ville. */
  @Column(name = "zone", length = 150)
  private String zone;

  // ── Informations complémentaires ───────────────────────────────

  /** Description de l'activité et des services offerts. */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Statut juridique (SARL, SAS, Entreprise individuelle…). */
  @Column(name = "legal_status", length = 100)
  private String legalStatus;

  /** Numéro d'immatriculation (RCCM ou équivalent). */
  @Column(name = "registration_number", length = 100)
  private String registrationNumber;

  // ── Documents (URLs MinIO bucket partner-documents) ────────────

  /** URL du document RCCM stocké dans le bucket MinIO {@code partner-documents}. */
  @Column(name = "rccm_url", length = 500)
  private String rccmUrl;

  /** URL du document DFE (Déclaration Fiscale d'Existence) stocké dans MinIO. */
  @Column(name = "dfe_url", length = 500)
  private String dfeUrl;

  /** URL du contrat de bail ou titre de propriété stocké dans MinIO. */
  @Column(name = "bail_url", length = 500)
  private String bailUrl;

  /** URL du logo de l'entreprise stocké dans MinIO. */
  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  // ── Statut et workflow ─────────────────────────────────────────

  /** Statut courant de la demande dans le cycle d'adhésion. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ApplicationStatus status;

  /** Horodatage de soumission initiale de la demande. */
  @Column(name = "submitted_at", nullable = false)
  private LocalDateTime submittedAt;

  /** Administrateur ayant traité la demande (null si non encore traitée). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_user_id")
  private User reviewedBy;

  /** Horodatage de la dernière décision administrative. */
  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  /** Motif de rejet ou d'incomplétude, renseigné par l'administrateur. */
  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;
}
