package com.africa.ubaxplatform.partner.dto;

import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Entrée du journal d'audit des transitions de statut d'une demande partenaire. */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationStatusLogResponse {

  private UUID id;
  private ApplicationStatus previousStatus;
  private ApplicationStatus newStatus;
  private String changedByName;
  private String comment;
  private LocalDateTime changedAt;
}
