package com.africa.ubaxplatform.dashboard.dto;

import java.math.BigDecimal;

/**
 * Décomposition des dépenses par catégorie sur la période sélectionnée.
 *
 * @param category libellé de la catégorie (ex. {@code MAINTENANCE}, {@code SALARY})
 * @param amount montant total des dépenses en XOF
 */
public record ExpenseBreakdownItem(String category, BigDecimal amount) {}
