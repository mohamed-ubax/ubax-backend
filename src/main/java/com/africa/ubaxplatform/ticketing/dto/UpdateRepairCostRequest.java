package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Requête de saisie du coût de réparation après résolution d'un ticket. */
@Getter
@Setter
public class UpdateRepairCostRequest {

  @NotNull(message = "Le coût de réparation est obligatoire")
  @DecimalMin(value = "0.0", inclusive = true, message = "Le coût ne peut pas être négatif")
  @Schema(description = "Coût total de la réparation en XOF", example = "75000.00")
  private BigDecimal repairCost;

  @NotBlank(message = "L'imputation du coût est obligatoire")
  @Pattern(regexp = "OWNER|TENANT|SHARED", message = "Valeurs acceptées : OWNER, TENANT, SHARED")
  @Schema(
      description = "Partie à qui imputer le coût",
      allowableValues = {"OWNER", "TENANT", "SHARED"},
      example = "OWNER")
  private String costImputedTo;

  @Schema(
      description = "Note de résolution décrivant les travaux effectués",
      example = "Joint remplacé, fuite colmatée. Pièces : 15 000 XOF. Main d'œuvre : 60 000 XOF.")
  private String resolutionNote;
}
