package com.africa.ubaxplatform.mandate.dto;

import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MandateResponse(
    UUID id,
    String referenceNumber,
    UUID agencyId,
    String agencyName,
    UUID ownerId,
    String ownerFullName,
    String ownerPhone,
    UUID createdById,
    String createdByFullName,
    MandateStatus status,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal commissionRate,
    String specialClauses,
    String terminationConditions,
    String fileUrl,
    LocalDateTime terminatedAt,
    String terminationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
