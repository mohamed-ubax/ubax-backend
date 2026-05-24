package com.africa.ubaxplatform.bailleur.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BailleurAgencyResponse {

  private UUID agencyId;
  private String agencyName;
  private String agencyLogo;
  private String agencyPhone;
  private String agencyEmail;
  private String agencyDescription;
  private LocalDateTime linkedAt;
}
