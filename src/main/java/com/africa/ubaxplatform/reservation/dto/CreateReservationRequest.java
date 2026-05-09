package com.africa.ubaxplatform.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Requête de création d'une réservation")
public record CreateReservationRequest(
    @Schema(description = "UUID du bien à réserver", required = true) @NotNull UUID propertyId,
    @Schema(description = "Date d'arrivée (incluse)", example = "2026-06-10", required = true)
        @NotNull
        @Future
        LocalDate checkInDate,
    @Schema(description = "Date de départ (exclue)", example = "2026-06-15", required = true)
        @NotNull
        @Future
        LocalDate checkOutDate,
    @Schema(description = "Nombre de voyageurs", example = "2", required = true) @NotNull @Min(1)
        Integer guestCount,
    @Schema(description = "Notes ou demandes spéciales du client", nullable = true) String notes) {}
