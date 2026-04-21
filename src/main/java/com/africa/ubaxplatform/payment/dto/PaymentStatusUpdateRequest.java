package com.africa.ubaxplatform.payment.dto;

import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Requête de mise à jour du statut d'un paiement")
public record PaymentStatusUpdateRequest(

    @NotNull
    @Schema(description = "Nouveau statut", example = "PAID")
    PaymentStatus status,

    @Schema(description = "Mode de paiement", example = "BANK_TRANSFER")
    PaymentMethod paymentMethod,

    @DecimalMin("0")
    @Schema(description = "Montant reçu (pour les paiements partiels)", example = "200000")
    BigDecimal amountPaid,

    @Schema(description = "Date de réception effective", example = "2025-10-03")
    LocalDate paidDate,

    @Schema(description = "URL du reçu (si disponible)")
    String receiptUrl,

    @Schema(description = "Note libre")
    String note) {}
