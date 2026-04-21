package com.africa.ubaxplatform.payment.repository;

import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.entity.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

  @Query(
      """
      SELECT e FROM Expense e
      WHERE e.agency.id = :agencyId
        AND (:category   IS NULL OR e.category    = :category)
        AND (:propertyId IS NULL OR e.property.id = :propertyId)
        AND (:from       IS NULL OR e.expenseDate >= :from)
        AND (:to         IS NULL OR e.expenseDate <= :to)
      """)
  Page<Expense> findWithFilters(
      @Param("agencyId") UUID agencyId,
      @Param("category") ExpenseCategory category,
      @Param("propertyId") UUID propertyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  /** Somme totale des dépenses d'une agence sur une période. */
  @Query(
      """
      SELECT COALESCE(SUM(e.amount), 0)
      FROM Expense e
      WHERE e.agency.id = :agencyId
        AND e.expenseDate BETWEEN :from AND :to
      """)
  BigDecimal sumByAgencyAndPeriod(
      @Param("agencyId") UUID agencyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /** Somme des dépenses par catégorie pour un mois. */
  @Query(
      """
      SELECT e.category, COALESCE(SUM(e.amount), 0)
      FROM Expense e
      WHERE e.agency.id = :agencyId
        AND e.expenseDate BETWEEN :from AND :to
      GROUP BY e.category
      """)
  java.util.List<Object[]> sumByCategoryAndPeriod(
      @Param("agencyId") UUID agencyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}
