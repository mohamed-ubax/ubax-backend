package com.africa.ubaxplatform.visitappointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour que l'agence confirme une demande de visite.
 *
 * <p>L'agence peut confirmer à la date/créneau demandé ou proposer une alternative.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Confirmation d'une demande de visite par l'agence")
public class ConfirmVisitRequestDto {

  @NotNull(message = "confirmedDate est requis")
  @Schema(description = "Date de confirmation", example = "2026-05-25")
  private LocalDate confirmedDate;

  @NotBlank(message = "confirmedTimeSlot est requis")
  @Schema(description = "Créneau confirmé", example = "10:00-14:00")
  private String confirmedTimeSlot;

  @Schema(
      description = "Notes de l'agence (optionnel)",
      example = "Veuillez arriver 5 min en avance")
  private String agencyNotes;
}
