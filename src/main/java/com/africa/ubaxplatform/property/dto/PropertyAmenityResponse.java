package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Commodité associée à un bien immobilier")
public record PropertyAmenityResponse(
    @Schema(description = "Identifiant unique de la commodité") UUID id,
    @Schema(
            description = "Code commodité standard (null si personnalisée)",
            example = "POOL",
            nullable = true)
        String code,
    @Schema(
            description = "Libellé personnalisé (null si commodité standard)",
            example = "Hammam",
            nullable = true)
        String customValue,
    @Schema(
            description = "Description de la commodité personnalisée",
            example = "Hammam traditionnel avec bain de vapeur",
            nullable = true)
        String customDescription) {}
