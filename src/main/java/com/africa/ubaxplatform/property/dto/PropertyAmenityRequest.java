package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Commodité d'un bien — standard (code) ou personnalisée (customValue)")
public record PropertyAmenityRequest(
    @Schema(
            description =
                "Code d'une commodité standard issue de la liste PROPERTY_AMENITY."
                    + " Null si commodité personnalisée.",
            example = "POOL",
            nullable = true)
        String code,
    @Schema(
            description =
                "Libellé d'une commodité non présente dans la liste standard."
                    + " Null si code renseigné.",
            example = "Hammam",
            nullable = true)
        String customValue,
    @Schema(
            description = "Description libre de la commodité personnalisée.",
            example = "Hammam traditionnel avec bain de vapeur",
            nullable = true)
        String customDescription,
    @Schema(
            description =
                "Description de la commodité telle qu'affichée dans la code list."
                    + " Pour une commodité standard : valeur issue de PROPERTY_AMENITY dans la_code_list."
                    + " Pour une commodité personnalisée : identique à customDescription.",
            example = "Piscine (privée ou commune)",
            nullable = true)
        String description) {}
