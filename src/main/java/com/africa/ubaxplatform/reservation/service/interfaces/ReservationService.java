package com.africa.ubaxplatform.reservation.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import com.africa.ubaxplatform.reservation.dto.CancelReservationRequest;
import com.africa.ubaxplatform.reservation.dto.CreateReservationRequest;
import com.africa.ubaxplatform.reservation.dto.ReservationResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationService {

  // ── Client ────────────────────────────────────────────────────────────────

  ReservationResponse create(String keycloakId, CreateReservationRequest request)
      throws CustomException;

  Page<ReservationResponse> getMyReservations(String keycloakId, Pageable pageable)
      throws CustomException;

  ReservationResponse getById(String keycloakId, UUID id) throws CustomException;

  ReservationResponse cancelByClient(String keycloakId, UUID id, CancelReservationRequest request)
      throws CustomException;

  // ── Hôtel (PARTNER) ───────────────────────────────────────────────────────

  Page<ReservationResponse> getHotelReservations(
      String keycloakId, ReservationStatus status, Pageable pageable) throws CustomException;

  ReservationResponse confirm(String keycloakId, UUID id) throws CustomException;

  ReservationResponse cancelByHotel(String keycloakId, UUID id, CancelReservationRequest request)
      throws CustomException;

  ReservationResponse complete(String keycloakId, UUID id) throws CustomException;

  ReservationResponse noShow(String keycloakId, UUID id) throws CustomException;

  // ── Admin ─────────────────────────────────────────────────────────────────

  Page<ReservationResponse> listAll(ReservationStatus status, Pageable pageable);
}
