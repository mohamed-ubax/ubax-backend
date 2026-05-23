package com.africa.ubaxplatform.visitappointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les créneaux disponibles d'un bien.
 *
 * <p>Retourné au client qui souhaite voir quand il peut demander une visite.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Créneaux disponibles pour les visites d'un bien")
public class VisitAvailabilityResponse {

  @Schema(description = "ID du bien", example = "550e8400-e29b-41d4-a716-446655440000")
  private String propertyId;

  @Schema(
      description = "Dates disponibles pour les 30 prochains jours",
      example = "[\"2026-05-25\", \"2026-05-26\", \"2026-05-27\"]")
  private List<LocalDate> availableDates;

  @Schema(
      description =
          "Créneaux disponibles par date. "
              + "Chaque créneau indique le nombre de places disponibles.",
      example =
          """
          {
            "2026-05-25": [
              {"slot": "10:00-14:00", "available": 2, "almostFull": false},
              {"slot": "14:00-18:00", "available": 1, "almostFull": true}
            ],
            "2026-05-26": [
              {"slot": "10:00-14:00", "available": 3, "almostFull": false}
            ]
          }
          """)
  private java.util.Map<LocalDate, List<AvailableSlot>> slotsByDate;

  /** Représentation d'un créneau disponible pour une date donnée. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "Créneau disponible avec nombre de places")
  public static class AvailableSlot {

    @Schema(description = "Créneau horaire", example = "10:00-14:00")
    private String slot;

    @Schema(description = "Nombre de places disponibles", example = "2")
    private Integer available;

    @Schema(
        description = "Vrai si 1 seule place reste (info pour UI: marquer comme \"peu de places\")",
        example = "false")
    private Boolean almostFull;
  }
}
