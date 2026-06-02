package com.africa.ubaxplatform.payment.repository;

import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  @Query(
      """
      SELECT p FROM Payment p
      WHERE p.deletedAt IS NULL
        AND (:agencyId   IS NULL OR p.agency.id     = :agencyId)
        AND (:status     IS NULL OR p.status         = :status)
        AND (:type       IS NULL OR p.paymentType    = :type)
        AND (:propertyId IS NULL OR p.property.id   = :propertyId)
        AND (:contractId IS NULL OR p.contract.id   = :contractId)
        AND (:tenantId   IS NULL OR p.tenant.id     = :tenantId)
        AND (:from       IS NULL OR p.dueDate       >= :from)
        AND (:to         IS NULL OR p.dueDate       <= :to)
      """)
  Page<Payment> findWithFilters(
      @Param("agencyId") UUID agencyId,
      @Param("status") PaymentStatus status,
      @Param("type") PaymentType type,
      @Param("propertyId") UUID propertyId,
      @Param("contractId") UUID contractId,
      @Param("tenantId") UUID tenantId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  /** Paiements en retard : échéance dépassée et statut PENDING ou PARTIAL. */
  @Query(
      """
      SELECT p FROM Payment p
      WHERE p.deletedAt IS NULL
        AND p.agency.id = :agencyId
        AND p.status IN ('PENDING', 'PARTIAL')
        AND p.dueDate < :today
      ORDER BY p.dueDate ASC
      """)
  List<Payment> findLateByAgency(@Param("agencyId") UUID agencyId, @Param("today") LocalDate today);

  /** Somme des paiements PAID pour une agence sur une période. */
  @Query(
      """
      SELECT COALESCE(SUM(p.amountPaid), 0)
      FROM Payment p
      WHERE p.agency.id = :agencyId
        AND p.status = 'PAID'
        AND p.paidDate BETWEEN :from AND :to
      """)
  BigDecimal sumPaidByAgencyAndPeriod(
      @Param("agencyId") UUID agencyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  /** Somme des montants en retard (PENDING/PARTIAL) pour une agence. */
  @Query(
      """
      SELECT COALESCE(SUM(p.amount - COALESCE(p.amountPaid, 0)), 0)
      FROM Payment p
      WHERE p.agency.id = :agencyId
        AND p.status IN ('PENDING', 'PARTIAL')
        AND p.dueDate < :today
      """)
  BigDecimal sumOverdueByAgency(@Param("agencyId") UUID agencyId, @Param("today") LocalDate today);

  List<Payment> findByContractIdOrderByDueDateDesc(UUID contractId);

  /** Nombre de paiements d'une agence par statut (pour les KPIs du dashboard). */
  @Query("SELECT COUNT(p) FROM Payment p WHERE p.agency.id = :agencyId AND p.status = :status")
  long countByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") PaymentStatus status);

  /** Décomposition du revenu encaissé par type de paiement sur une période. */
  @Query(
      """
      SELECT p.paymentType, COALESCE(SUM(p.amountPaid), 0)
      FROM Payment p
      WHERE p.agency.id = :agencyId
        AND p.status = 'PAID'
        AND p.paidDate BETWEEN :from AND :to
      GROUP BY p.paymentType
      """)
  List<Object[]> sumPaidByTypeAndPeriod(
      @Param("agencyId") UUID agencyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  /**
   * Vérifie si un paiement existe déjà pour un contrat et une date d'échéance — anti-doublon
   * scheduler.
   */
  boolean existsByContractIdAndDueDateAndDeletedAtIsNull(UUID contractId, LocalDate dueDate);

  /** Paiements PENDING ou PARTIAL dont la date d'échéance est dépassée — passage en LATE. */
  @Query(
      """
      SELECT p FROM Payment p
      WHERE p.deletedAt IS NULL
        AND p.status IN :statuses
        AND p.dueDate < :today
      """)
  List<Payment> findOverdueByStatuses(
      @Param("statuses") Collection<PaymentStatus> statuses, @Param("today") LocalDate today);

  // ── Archivage (soft-deleted) ────────────────────────────────────

  @Query(
      """
      SELECT p FROM Payment p
      WHERE p.deletedAt IS NOT NULL
        AND (:agencyId IS NULL OR p.agency.id = :agencyId)
      """)
  Page<Payment> findArchived(@Param("agencyId") UUID agencyId, Pageable pageable);

  java.util.Optional<Payment> findByIdAndDeletedAtIsNotNull(UUID id);
}
