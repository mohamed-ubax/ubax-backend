package com.africa.ubaxplatform.bailleur.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Bailleur lié à une agence — retourné par GET /v1/bailleur/agency/bailleurs")
public class AgencyBailleurResponse {

  @Schema(
      description = "UUID du compte bailleur — à utiliser comme `ownerId` dans POST /v1/properties")
  private UUID id;

  private String firstName;
  private String lastName;
  private String phone;
  private String email;
  private String avatarUrl;

  @Schema(
      description =
          "Date à laquelle le lien agence↔bailleur a été créé (approbation de la demande)")
  private LocalDateTime joinedAt;
}
