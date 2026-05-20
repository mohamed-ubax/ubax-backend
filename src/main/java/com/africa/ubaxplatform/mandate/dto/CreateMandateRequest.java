package com.africa.ubaxplatform.mandate.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMandateRequest {

  @NotNull(message = "ownerId (bailleur) est obligatoire")
  private UUID ownerId;

  @NotNull(message = "startDate est obligatoire")
  private LocalDate startDate;

  private LocalDate endDate;
  private BigDecimal commissionRate;
  private String specialClauses;
  private String terminationConditions;
}
