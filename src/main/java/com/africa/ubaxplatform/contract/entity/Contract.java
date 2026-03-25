package com.africa.ubaxplatform.contract.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.codeList.ContractType;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Contrat liant un propriétaire et un locataire/acheteur pour un bien donné.
 *
 * <p>Couvre 4 types de contrats selon le cycle immobilier UBAX :
 *
 * <ul>
 *   <li>{@code LEASE} — bail de location (mensuel, annuel).
 *   <li>{@code SALE} — acte de vente définitif.
 *   <li>{@code RESERVATION} — contrat de réservation avec acompte (option d'achat pendant une durée
 *       limitée).
 *   <li>{@code MANDATE} — mandat de gestion donné à une agence.
 * </ul>
 *
 * <p><b>Workflow :</b> {@code DRAFT → PENDING_SIGNATURE → ACTIVE → TERMINATED / EXPIRED}
 *
 * <ol>
 *   <li>Le gestionnaire génère le PDF depuis un template ({@code DocumentService}).
 *   <li>Le PDF est stocké dans MinIO et {@code fileUrl} est valorisé.
 *   <li>Les parties signent via DocuSeal ({@code SignatureRequest}).
 *   <li>Le PDF signé est récupéré et {@code signedFileUrl} est valorisé.
 *   <li>Le contrat passe en {@code ACTIVE}.
 * </ol>
 *
 * <p><b>Paiements récurrents :</b> pour un bail {@code LEASE}, le {@code PaymentSchedulerJob}
 * génère automatiquement une entrée {@code Payment} le 25 de chaque mois (configurable via {@code
 * paymentDay}) pour le mois suivant, tant que le contrat est {@code ACTIVE}.
 *
 * <p><b>Documents générés liés :</b> factures, reçus et états des lieux sont référencés dans {@code
 * Document} via {@code refId = contract.getId()} et {@code refType = CONTRACT}.
 */
@Entity
@Table(
    name = "contracts",
    schema = "administrative",
    indexes = {
      @Index(name = "idx_contract_property", columnList = "property_id"),
      @Index(name = "idx_contract_tenant", columnList = "tenant_id"),
      @Index(name = "idx_contract_owner", columnList = "owner_id"),
      @Index(name = "idx_contract_status", columnList = "status"),
      @Index(name = "idx_contract_type", columnList = "contract_type"),
      @Index(name = "idx_contract_start_date", columnList = "start_date"),
      @Index(name = "idx_contract_end_date", columnList = "end_date")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Contract extends BaseEntity {

  /**
   * Bien immobilier faisant l'objet du contrat. Permet de retrouver automatiquement la
   * localisation, le type de bien et les commodités pour les templates de documents générés.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "property_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_contract_property"))
  private Property property;

  /**
   * Dossier locataire qualifié lié à ce contrat. {@code null} pour les contrats de type {@code
   * SALE} ou {@code MANDATE}. La qualification du dossier ({@code Tenant.isReadyToSign()}) est
   * vérifiée avant la création du contrat.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", foreignKey = @ForeignKey(name = "fk_contract_tenant"))
  private Tenant tenant;

  /**
   * Propriétaire ou mandant signataire du contrat côté bailleur. Pour un mandat, c'est le
   * propriétaire qui délègue à l'agence.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "owner_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_contract_owner"))
  private User owner;

  /**
   * Agent ou gestionnaire ayant créé et suivi ce contrat. {@code null} si le contrat est géré
   * directement entre particuliers.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_contract_created_by"))
  private User createdBy;

  /**
   * Nature juridique du contrat.
   *
   * <ul>
   *   <li>{@code LEASE} — bail de location simple ou meublée.
   *   <li>{@code SALE} — acte de vente définitif du bien.
   *   <li>{@code RESERVATION} — contrat de réservation avec acompte (option d'achat pendant {@code
   *       reservationDurationDays} jours).
   *   <li>{@code MANDATE} — mandat de gestion locative donné à une agence.
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "contract_type", nullable = false, length = 30)
  private ContractType contractType;

  /**
   * Numéro de référence unique du contrat, lisible par les parties. Format : {@code
   * CTR-{YEAR}-{TYPE}-{SEQ}} Exemple : {@code CTR-2025-LEASE-000042} Imprimé sur le PDF du contrat
   * et dans les notifications.
   */
  @Column(name = "reference_number", unique = true, length = 100)
  private String referenceNumber;

  /**
   * Date de début de validité du contrat. Pour un bail, c'est la date d'entrée dans le logement.
   * Pour une vente, c'est la date de transfert de propriété.
   */
  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  /**
   * Date de fin du contrat. {@code null} pour les baux à durée indéterminée ou les mandats ouverts.
   * Pour les réservations, cette date marque l'expiration de l'option d'achat.
   */
  @Column(name = "end_date")
  private LocalDate endDate;

  /**
   * Durée de la réservation en jours (applicable au type {@code RESERVATION}). Exemple : {@code 7}
   * = l'acheteur a 7 jours pour confirmer son achat.
   */
  @Column(name = "reservation_duration_days")
  private Integer reservationDurationDays;

  /** Loyer mensuel hors charges en XOF (applicable aux contrats {@code LEASE}). */
  @Column(name = "monthly_rent", precision = 15, scale = 2)
  private BigDecimal monthlyRent;

  /**
   * Charges mensuelles en XOF (eau, électricité communes, gardiennage...). Ajoutées au loyer pour
   * calculer le montant total dû chaque mois.
   */
  @Column(name = "monthly_charges", precision = 15, scale = 2)
  private BigDecimal monthlyCharges;

  /**
   * Montant de la caution versée par le locataire à l'entrée (XOF). Généralement équivalent à 1 ou
   * 2 mois de loyer. Restituée en fin de contrat après déduction des éventuels dégâts.
   */
  @Column(name = "deposit_amount", precision = 15, scale = 2)
  private BigDecimal depositAmount;

  /** {@code true} si la caution a été restituée au locataire en fin de contrat. */
  @Column(name = "deposit_returned", nullable = false)
  @Builder.Default
  private boolean depositReturned = false;

  /** Prix de vente total en XOF (applicable aux contrats {@code SALE}). */
  @Column(name = "sale_price", precision = 15, scale = 2)
  private BigDecimal salePrice;

  /**
   * Montant de l'acompte versé lors d'une réservation (XOF). Applicable aux contrats {@code
   * RESERVATION}.
   */
  @Column(name = "reservation_deposit", precision = 15, scale = 2)
  private BigDecimal reservationDeposit;

  /**
   * Commission de l'agence en pourcentage (%) du loyer ou du prix de vente. Applicable uniquement
   * si un agent est associé au contrat.
   */
  @Column(name = "agency_commission_rate", precision = 5, scale = 2)
  private BigDecimal agencyCommissionRate;

  /**
   * Jour du mois auquel le loyer est exigible (1 à 28). Défaut : 5 (le 5 de chaque mois). Utilisé
   * par le {@code PaymentSchedulerJob} pour générer les échéances.
   */
  @Column(name = "payment_day", nullable = false)
  @Builder.Default
  private int paymentDay = 5;

  /**
   * Clauses particulières négociées entre les parties. Intégrées dans le PDF du contrat dans une
   * section dédiée. Exemple : {@code "Interdiction de sous-location. Animaux autorisés."}
   */
  @Column(name = "special_clauses", columnDefinition = "TEXT")
  private String specialClauses;

  /**
   * Conditions de résiliation anticipée du contrat. Exemple : {@code "Préavis de 3 mois. Pénalité
   * équivalente à 1 mois de loyer."}
   */
  @Column(name = "termination_conditions", columnDefinition = "TEXT")
  private String terminationConditions;

  /**
   * État du contrat dans son cycle de vie.
   *
   * <ul>
   *   <li>{@code DRAFT} — en cours de rédaction par le gestionnaire.
   *   <li>{@code PENDING_SIGNATURE} — PDF généré, en attente de signature des parties via DocuSeal.
   *   <li>{@code ACTIVE} — signé par toutes les parties, en cours d'exécution.
   *   <li>{@code TERMINATED} — résilié avant son terme (préavis respecté).
   *   <li>{@code EXPIRED} — arrivé à son terme sans renouvellement.
   *   <li>{@code CANCELLED} — annulé avant signature (désistement).
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private ContractStatus status = ContractStatus.DRAFT;

  /**
   * URL du PDF du contrat généré depuis le template, avant signature. Stocké dans MinIO bucket
   * {@code documents-generated}. Valorisé lors du passage au statut {@code PENDING_SIGNATURE}.
   * Format : {@code http://minio:9000/documents-generated/contracts/{id}.pdf}
   */
  @Column(name = "file_url", length = 500)
  private String fileUrl;

  /**
   * URL du PDF du contrat signé par toutes les parties. Récupéré depuis DocuSeal après signature
   * complète. Valorisé lors du passage au statut {@code ACTIVE}. Format : {@code
   * http://minio:9000/documents-generated/contracts/{id}-signed.pdf}
   */
  @Column(name = "signed_file_url", length = 500)
  private String signedFileUrl;

  /**
   * Date effective de résiliation ou d'expiration du contrat. Valorisée lors du passage aux statuts
   * {@code TERMINATED} ou {@code EXPIRED}.
   */
  @Column(name = "terminated_at")
  private LocalDateTime terminatedAt;

  /**
   * Motif de résiliation ou d'annulation du contrat. Renseigné par le gestionnaire lors de la
   * clôture.
   */
  @Column(name = "termination_reason", columnDefinition = "TEXT")
  private String terminationReason;

  /** Utilisateur ayant initié la résiliation (gestionnaire, propriétaire ou locataire). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "terminated_by", foreignKey = @ForeignKey(name = "fk_contract_terminated_by"))
  private User terminatedBy;

  /** {@code true} si le contrat est en cours d'exécution. */
  public boolean isActive() {
    return status == ContractStatus.ACTIVE;
  }

  /** {@code true} si le contrat est un bail de location. */
  public boolean isLease() {
    return contractType == ContractType.LEASE;
  }

  /** {@code true} si le contrat est un acte de vente. */
  public boolean isSale() {
    return contractType == ContractType.SALE;
  }

  /** {@code true} si le PDF signé est disponible. */
  public boolean isSigned() {
    return signedFileUrl != null;
  }

  /**
   * Calcule le montant total mensuel dû par le locataire (loyer + charges). Retourne {@code null}
   * si le contrat n'est pas un bail.
   */
  public BigDecimal getTotalMonthlyAmount() {
    if (monthlyRent == null) return null;
    BigDecimal charges = monthlyCharges != null ? monthlyCharges : BigDecimal.ZERO;
    return monthlyRent.add(charges);
  }

  /** {@code true} si le contrat est encore en attente de signature. */
  public boolean isPendingSignature() {
    return status == ContractStatus.PENDING_SIGNATURE;
  }
}
