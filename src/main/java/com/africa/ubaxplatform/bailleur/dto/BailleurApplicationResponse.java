package com.africa.ubaxplatform.bailleur.dto;

import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BailleurApplicationResponse {

  private UUID id;
  private UUID agencyId;
  private String agencyName;
  private String firstName;
  private String lastName;
  private String phone;
  private String email;
  private String idType;
  private String idNumber;
  private BailleurApplicationStatus status;
  private boolean conflictDetected;
  private String conflictNote;
  private String rejectionReason;
  private String reviewedByName;
  private LocalDateTime reviewedAt;
  private List<BailleurPropertyResponse> properties;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
