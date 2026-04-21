package com.africa.ubaxplatform.payment.entity;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.payment.codeList.CostCenter;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.property.entity.Property;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Dépense comptable enregistrée par une agence UBAX.
 *
 * <p>Couvre toutes les sorties financières de l'agence : maintenance, marketing, salaires, charges
 * d'exploitation. Peut être rattachée à un bien spécifique ({@code PROPERTY_SPECIFIC}) ou à
 * l'agence en général ({@code AGENCY_GENERAL}).
 */
@Entity
@Table(name = "expenses", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Expense extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "agency_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_expense_agency"))
  private Agency agency;

  /** Bien immobilier concerné (null si la dépense est générale à l'agence). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "property_id", foreignKey = @ForeignKey(name = "fk_expense_property"))
  private Property property;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "created_by",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_expense_created_by"))
  private User createdBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 50)
  private ExpenseCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_center", nullable = false, length = 30)
  private CostCenter costCenter;

  /** Intitulé de la dépense (ex : «Réparation toiture villa Cocody»). */
  @Column(name = "label", nullable = false, length = 255)
  private String label;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", length = 30)
  private PaymentMethod paymentMethod;

  @Column(name = "expense_date", nullable = false)
  private LocalDate expenseDate;

  /** Nom du fournisseur ou prestataire (ex : «SODECI», «Agence pub ORANGE»). */
  @Column(name = "provider", length = 255)
  private String provider;

  /** Numéro de facture ou référence du bon de commande. */
  @Column(name = "invoice_reference", length = 100)
  private String invoiceReference;

  /** URL de la facture / justificatif dans MinIO (bucket {@code partner-documents}). */
  @Column(name = "justification_url", length = 500)
  private String justificationUrl;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;
}
