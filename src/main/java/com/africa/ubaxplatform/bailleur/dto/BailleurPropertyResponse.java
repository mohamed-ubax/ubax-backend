package com.africa.ubaxplatform.bailleur.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BailleurPropertyResponse {

  private UUID id;
  private String address;
  private String propertyType;
  private Integer rooms;
  private BigDecimal surface;
  private BigDecimal desiredRent;
  private String description;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private boolean geoVerified;
}
