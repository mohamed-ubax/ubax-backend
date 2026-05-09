package com.africa.ubaxplatform.reservation.dto;

import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Détail d'une réservation")
public record ReservationResponse(
    UUID id,
    UUID propertyId,
    String propertyTitle,
    String propertyCity,
    UUID clientId,
    String clientFullName,
    String clientEmail,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    int numberOfNights,
    int guestCount,
    BigDecimal pricePerNight,
    BigDecimal totalAmount,
    ReservationStatus status,
    String notes,
    String cancellationReason,
    UUID cancelledById,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
