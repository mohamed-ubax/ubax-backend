package com.africa.ubaxplatform.visitappointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour configurer les créneaux disponibles d'un bien immobilier.
 *
 * <p>Utilisé par l'agence pour définir les jours et créneaux de visite disponibles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    description = "Configuration des créneaux de visite disponibles",
    example =
        """
        {
          "propertyId": "550e8400-e29b-41d4-a716-446655440000",
          "timeSlots": {
            "1": ["10:00-14:00", "14:00-18:00"],
            "2": ["10:00-14:00"],
            "3": ["14:00-22:00"],
            "5": ["10:00-18:00"],
            "6": ["10:00-18:00"]
          },
          "blackoutDates": ["2026-05-11", "2026-05-12"],
          "maxVisitsPerSlot": 3
        }
        """)
public class ConfigureVisitAvailabilityDto {

  @NotNull(message = "propertyId est requis")
  @Schema(description = "ID du bien à configurer", example = "550e8400-e29b-41d4-a716-446655440000")
  private String propertyId;

  @NotNull(message = "timeSlots est requis")
  @Schema(
      description =
          "Créneaux par jour de semaine (0=Dimanche, 1=Lundi, ..., 6=Samedi). "
              + "null ou absent = jour fermé",
      example =
          """
        {
          "1": ["10:00-14:00", "14:00-18:00"],
          "2": ["10:00-14:00"],
          "5": ["10:00-18:00"]
        }
        """)
  private Map<String, List<String>> timeSlots;

  @Schema(
      description = "Dates fermées (vacances, congés) au format YYYY-MM-DD",
      example = "[\"2026-05-11\", \"2026-05-12\"]")
  private List<LocalDate> blackoutDates;

  @Min(value = 1, message = "maxVisitsPerSlot doit être >= 1")
  @Schema(description = "Nombre maximum de visites par créneau", example = "3", defaultValue = "3")
  private Integer maxVisitsPerSlot = 3;
}
