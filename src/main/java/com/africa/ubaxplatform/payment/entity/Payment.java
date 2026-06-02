package com.africa.ubaxplatform.payment.entity;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Paiement enregistré sur la plateforme UBAX.
 *
 * <p>Couvre les loyers mensuels, cautions, charges, commissions agence et prix de vente. Chaque
 * paiement est rattaché à un contrat et/ou à un bien, et peut être partiellement réglé ({@code
 * PARTIAL}).
 *
 * <p><b>Workflow :</b> {@code PENDING → PAID / PARTIAL / LATE / CANCELLED}
 *
 * <p>Le {@code PaymentSchedulerJob} (à implémenter) génère automatiquement les paiements {@code
 * PENDING} pour les baux actifs.
 */
@Entity
@Table(name = "payments", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id", foreignKey = @ForeignKey(name = "fk_payment_contract"))
  private Contract contract;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", foreignKey = @ForeignKey(name = "fk_payment_tenant"))
  private Tenant tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agency_id", foreignKey = @ForeignKey(name = "fk_payment_agency"))
  private Agency agency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "property_id", foreignKey = @ForeignKey(name = "fk_payment_property"))
  private Property property;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "recorded_by",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_payment_recorded_by"))
  private User recordedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type", nullable = false, length = 30)
  private PaymentType paymentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", length = 30)
  private PaymentMethod paymentMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private PaymentStatus status;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /** Montant effectivement encaissé (utile pour les paiements partiels). */
  @Column(name = "amount_paid", precision = 15, scale = 2)
  private BigDecimal amountPaid;

  /** Date d'échéance – quand le paiement est attendu. */
  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  /** Date de réception effective du paiement. Null si non encore encaissé. */
  @Column(name = "paid_date")
  private LocalDate paidDate;

  /** Libellé de la période couverte. Exemple : {@code "Octobre 2025"}. */
  @Column(name = "period_label", length = 50)
  private String periodLabel;

  /** Numéro de référence unique du paiement (ex : {@code PAY-2025-10-000042}). */
  @Column(name = "reference", unique = true, length = 100)
  private String reference;

  /** URL du reçu ou bordereau de paiement dans MinIO (bucket {@code partner-documents}). */
  @Column(name = "receipt_url", length = 500)
  private String receiptUrl;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  /** Horodatage de la suppression logique. {@code null} = paiement actif. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** {@code true} si la date d'échéance est dépassée et le paiement n'est pas {@code PAID}. */
  public boolean isOverdue() {
    return (status == PaymentStatus.PENDING || status == PaymentStatus.PARTIAL)
        && dueDate != null
        && dueDate.isBefore(LocalDate.now());
  }
}
