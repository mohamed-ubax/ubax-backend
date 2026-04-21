package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Requête d'activation ou de prolongation du boost d'une annonce immobilière.
 *
 * <p>Le boost propulse l'annonce en tête des résultats de recherche publics pendant la durée
 * spécifiée. À l'expiration, le scheduler {@code PropertySchedulerJob} remet automatiquement {@code
 * boosted = false}. Un admin peut booster à nouveau en appelant le même endpoint.
 */
@Schema(description = "Paramètres du boost d'une annonce")
public record PropertyBoostRequest(
    @Schema(
            description = "Durée du boost en jours (1 à 365).",
            example = "30",
            minimum = "1",
            maximum = "365")
        @NotNull(message = "boostDays est obligatoire")
        @Min(value = 1, message = "La durée minimale du boost est 1 jour")
        @Max(value = 365, message = "La durée maximale du boost est 365 jours")
        Integer boostDays) {}
