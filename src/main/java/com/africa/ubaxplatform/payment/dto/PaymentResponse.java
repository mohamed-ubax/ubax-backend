package com.africa.ubaxplatform.payment.dto;

import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Réponse d'un paiement")
public record PaymentResponse(
    UUID id,
    UUID contractId,
    UUID tenantId,
    UUID agencyId,
    UUID propertyId,
    UUID recordedById,
    String recordedByName,
    PaymentType paymentType,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    BigDecimal amount,
    BigDecimal amountPaid,
    LocalDate dueDate,
    LocalDate paidDate,
    String periodLabel,
    String reference,
    String receiptUrl,
    String note,
    boolean overdue,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
