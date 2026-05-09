package com.africa.ubaxplatform.contract.dto;

import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractResponse(
    UUID id,
    String referenceNumber,
    UUID propertyId,
    String propertyTitle,
    String propertyCity,
    UUID ownerId,
    String ownerFullName,
    UUID tenantId,
    String tenantFullName,
    UUID createdById,
    String createdByFullName,
    String contractType,
    ContractStatus status,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal monthlyRent,
    BigDecimal monthlyCharges,
    BigDecimal totalMonthlyAmount,
    BigDecimal depositAmount,
    boolean depositReturned,
    BigDecimal salePrice,
    BigDecimal reservationDeposit,
    Integer reservationDurationDays,
    BigDecimal agencyCommissionRate,
    int paymentDay,
    String specialClauses,
    String terminationConditions,
    String fileUrl,
    String signedFileUrl,
    LocalDateTime terminatedAt,
    String terminationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
