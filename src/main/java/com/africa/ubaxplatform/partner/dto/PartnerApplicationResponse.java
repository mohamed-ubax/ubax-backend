package com.africa.ubaxplatform.partner.dto;

import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Représentation d'une demande d'adhésion partenaire retournée dans les réponses API. */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartnerApplicationResponse {

  private UUID id;
  private String partnerType;

  @Schema(description = "Raison sociale ou dénomination commerciale")
  private String companyName;

  @Schema(description = "Prénom du représentant légal")
  private String legalRepFirstName;

  @Schema(description = "Nom de famille du représentant légal")
  private String legalRepLastName;

  private String phone;
  private String email;
  private String country;
  private String city;
  private String postalAddress;
  private String zone;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String description;
  private String legalStatus;
  private String registrationNumber;
  private String rccmUrl;
  private String dfeUrl;
  private String bailUrl;
  private String logoUrl;
  private ApplicationStatus status;
  private LocalDateTime submittedAt;
  private String reviewedByName;
  private LocalDateTime reviewedAt;
  private String rejectionReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Historique des transitions de statut (inclus uniquement pour les admins). */
  private List<ApplicationStatusLogResponse> statusHistory;
}
