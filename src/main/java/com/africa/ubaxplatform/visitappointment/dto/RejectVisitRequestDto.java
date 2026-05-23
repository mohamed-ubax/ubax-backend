package com.africa.ubaxplatform.visitappointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour que l'agence rejette une demande de visite. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Rejet d'une demande de visite par l'agence")
public class RejectVisitRequestDto {

  @NotBlank(message = "reason est requis")
  @Schema(description = "Motif du rejet", example = "Bien vendu")
  private String reason;
}
