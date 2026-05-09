package com.africa.ubaxplatform.reservation.mapper;

import com.africa.ubaxplatform.reservation.dto.ReservationResponse;
import com.africa.ubaxplatform.reservation.entity.Reservation;

public class ReservationMapper {

  private ReservationMapper() {}

  public static ReservationResponse toResponse(Reservation r) {
    var client = r.getClient();
    var property = r.getProperty();
    return new ReservationResponse(
        r.getId(),
        property.getId(),
        property.getTitle(),
        property.getCity(),
        client.getId(),
        client.getFirstName() + " " + client.getLastName(),
        client.getEmail(),
        r.getCheckInDate(),
        r.getCheckOutDate(),
        r.getNumberOfNights(),
        r.getGuestCount(),
        r.getPricePerNight(),
        r.getTotalAmount(),
        r.getStatus(),
        r.getNotes(),
        r.getCancellationReason(),
        r.getCancelledBy() != null ? r.getCancelledBy().getId() : null,
        r.getConfirmedAt(),
        r.getCancelledAt(),
        r.getCompletedAt(),
        r.getCreatedAt(),
        r.getUpdatedAt());
  }
}
