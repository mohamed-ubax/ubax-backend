package com.africa.ubaxplatform.payment.dto;

import com.africa.ubaxplatform.payment.codeList.CostCenter;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Réponse d'une dépense comptable")
public record ExpenseResponse(
    UUID id,
    UUID agencyId,
    UUID propertyId,
    UUID createdById,
    String createdByName,
    ExpenseCategory category,
    CostCenter costCenter,
    String label,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    LocalDate expenseDate,
    String provider,
    String invoiceReference,
    String justificationUrl,
    String note,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
