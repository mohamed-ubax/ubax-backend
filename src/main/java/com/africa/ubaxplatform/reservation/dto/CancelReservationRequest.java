package com.africa.ubaxplatform.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requête d'annulation d'une réservation")
public record CancelReservationRequest(
    @Schema(description = "Motif d'annulation", required = true) @NotBlank String reason) {}
