package com.africa.ubaxplatform.payment.dto;

import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Requête d'enregistrement d'un paiement")
public record PaymentCreateRequest(
    @Schema(description = "UUID du contrat lié (optionnel si hors contrat)") UUID contractId,
    @Schema(description = "UUID du locataire (optionnel)") UUID tenantId,
    @Schema(description = "UUID du bien concerné (optionnel)") UUID propertyId,
    @NotNull @Schema(description = "Type de paiement", example = "RENT") PaymentType paymentType,
    @Schema(description = "Mode de paiement", example = "MOBILE_MONEY") PaymentMethod paymentMethod,
    @NotNull
        @DecimalMin("0")
        @Schema(description = "Montant total attendu (XOF)", example = "350000")
        BigDecimal amount,
    @Schema(
            description = "Montant effectivement reçu – renseigné pour les paiements partiels",
            example = "200000")
        @DecimalMin("0")
        BigDecimal amountPaid,
    @NotNull @Schema(description = "Date d'échéance (ISO-8601)", example = "2025-10-05")
        LocalDate dueDate,
    @Schema(
            description = "Date de réception effective (null si pas encore encaissé)",
            example = "2025-10-03")
        LocalDate paidDate,
    @Schema(description = "Période couverte (libellé libre)", example = "Octobre 2025")
        String periodLabel,
    @Schema(
            description = "Référence interne du paiement (générée si absente)",
            example = "PAY-2025-10-000042")
        String reference,
    @Schema(
            description = "URL du reçu déjà uploadé dans MinIO",
            example = "https://minio.ubax.africa/partner-documents/recu.pdf")
        String receiptUrl,
    @Schema(description = "Note libre") String note) {}
