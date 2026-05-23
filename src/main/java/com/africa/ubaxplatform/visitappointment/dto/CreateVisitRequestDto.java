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
 * DTO pour créer une demande de visite.
 *
 * <p>Client demande une visite pour une date et un créneau donnés.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    description = "Demande de visite d'un bien",
    example =
        """
        {
          "propertyId": "550e8400-e29b-41d4-a716-446655440000",
          "requestedDate": "2026-05-25",
          "requestedTimeSlot": "10:00-14:00",
          "clientNotes": "Très intéressé, visite rapide SVP"
        }
        """)
public class CreateVisitRequestDto {

  @NotNull(message = "propertyId est requis")
  @Schema(description = "ID du bien à visiter", example = "550e8400-e29b-41d4-a716-446655440000")
  private String propertyId;

  @NotNull(message = "requestedDate est requis")
  @Schema(description = "Date de visite souhaitée (YYYY-MM-DD)", example = "2026-05-25")
  private LocalDate requestedDate;

  @NotBlank(message = "requestedTimeSlot est requis")
  @Schema(description = "Créneau horaire demandé", example = "10:00-14:00")
  private String requestedTimeSlot;

  @Schema(description = "Notes du client (optionnel)", example = "Visite rapide, très intéressé")
  private String clientNotes;
}
