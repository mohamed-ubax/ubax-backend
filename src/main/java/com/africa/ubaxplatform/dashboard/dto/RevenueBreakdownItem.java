package com.africa.ubaxplatform.dashboard.dto;

import java.math.BigDecimal;

/**
 * Décomposition du revenu encaissé par type de paiement sur la période sélectionnée.
 *
 * @param type libellé du type de paiement (ex. {@code RENT}, {@code COMMISSION})
 * @param amount montant total encaissé en XOF
 */
public record RevenueBreakdownItem(String type, BigDecimal amount) {}
