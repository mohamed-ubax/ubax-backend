package com.africa.ubaxplatform.visitappointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour mettre à jour les dates fermées (blackout) d'un bien. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Mise à jour des dates fermées (blackout)")
public class UpdateBlackoutDatesDto {

  @NotNull(message = "blackoutDates est requis")
  @Schema(
      description = "Liste des dates fermées au format YYYY-MM-DD",
      example = "[\"2026-05-11\", \"2026-05-12\", \"2026-05-13\"]")
  private java.util.List<LocalDate> blackoutDates;
}
