package com.africa.ubaxplatform.property.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Réponse de disponibilité d'un bien hôtelier pour une plage de dates.
 *
 * <p>{@code availableUnits = unitCount - confirmedOverlaps} — si ≥ 1, le client peut réserver.
 */
public record PropertyAvailabilityResponse(
    UUID propertyId,
    int unitCount,
    long confirmedOverlaps,
    long availableUnits,
    LocalDate checkIn,
    LocalDate checkOut) {}
