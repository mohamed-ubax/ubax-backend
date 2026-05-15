package com.africa.ubaxplatform.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Confirmation d'ajout ou de suppression d'un favori")
public record FavoriteResponse(
    @Schema(description = "Identifiant du favori") UUID favoriteId,
    @Schema(description = "Identifiant du bien mis en favori") UUID propertyId,
    @Schema(description = "Date d'ajout aux favoris") LocalDateTime addedAt) {}
