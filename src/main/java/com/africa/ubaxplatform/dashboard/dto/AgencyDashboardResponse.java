package com.africa.ubaxplatform.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tableau de bord analytique de l'agence.
 *
 * <h2>KPIs financiers</h2>
 *
 * <ul>
 *   <li>{@code totalRevenue} – somme des paiements {@code PAID} sur la période
 *   <li>{@code totalExpenses} – somme des dépenses enregistrées sur la période
 *   <li>{@code netRevenue} – {@code totalRevenue - totalExpenses}
 *   <li>{@code overdueAmount} – montant restant dû sur les paiements {@code PENDING/PARTIAL} en
 *       retard
 *   <li>{@code recoveryRate} – taux de recouvrement en % : {@code totalRevenue / (totalRevenue +
 *       overdueAmount) × 100}
 * </ul>
 *
 * <h2>KPIs portfolio</h2>
 *
 * <ul>
 *   <li>{@code totalProperties} – nombre total de biens (tous statuts)
 *   <li>{@code publishedProperties} – biens actuellement publiés
 *   <li>{@code reservedProperties} – biens actuellement réservés
 *   <li>{@code activeContracts} – contrats en statut {@code ACTIVE}
 *   <li>{@code pendingPaymentsCount} – paiements en attente
 *   <li>{@code latePaymentsCount} – paiements en retard ({@code PENDING/PARTIAL} + échéance
 *       dépassée)
 *   <li>{@code paidPaymentsCount} – paiements acquittés
 * </ul>
 *
 * <h2>Décompositions</h2>
 *
 * <ul>
 *   <li>{@code revenueByType} – revenus encaissés ventilés par type de paiement
 *   <li>{@code expensesByCategory} – dépenses ventilées par catégorie
 * </ul>
 *
 * @param periodFrom début de la période analysée
 * @param periodTo fin de la période analysée
 * @param totalRevenue total encaissé en XOF
 * @param totalExpenses total des dépenses en XOF
 * @param netRevenue résultat net en XOF
 * @param overdueAmount montant en souffrance en XOF
 * @param recoveryRate taux de recouvrement (0–100 %)
 * @param totalProperties nombre total de biens
 * @param publishedProperties biens publiés
 * @param reservedProperties biens réservés
 * @param activeContracts contrats actifs
 * @param pendingPaymentsCount paiements en attente
 * @param latePaymentsCount paiements en retard
 * @param paidPaymentsCount paiements acquittés
 * @param revenueByType décomposition du revenu par type
 * @param expensesByCategory décomposition des dépenses par catégorie
 */
public record AgencyDashboardResponse(
    LocalDate periodFrom,
    LocalDate periodTo,
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal netRevenue,
    BigDecimal overdueAmount,
    BigDecimal recoveryRate,
    long totalProperties,
    long publishedProperties,
    long reservedProperties,
    long activeContracts,
    long pendingPaymentsCount,
    long latePaymentsCount,
    long paidPaymentsCount,
    List<RevenueBreakdownItem> revenueByType,
    List<ExpenseBreakdownItem> expensesByCategory) {}
