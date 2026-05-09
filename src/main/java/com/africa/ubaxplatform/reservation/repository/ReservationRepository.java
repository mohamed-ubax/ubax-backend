package com.africa.ubaxplatform.reservation.repository;

import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import com.africa.ubaxplatform.reservation.entity.Reservation;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

  // ── Client : mes réservations ─────────────────────────────────────────────

  Page<Reservation> findByClientIdAndDeletedAtIsNull(UUID clientId, Pageable pageable);

  // ── Hôtel : réservations de leurs biens ───────────────────────────────────

  @Query(
      "SELECT r FROM Reservation r WHERE r.property.owner.hotel.id = :hotelId"
          + " AND r.deletedAt IS NULL")
  Page<Reservation> findByHotelId(@Param("hotelId") UUID hotelId, Pageable pageable);

  @Query(
      "SELECT r FROM Reservation r WHERE r.property.owner.hotel.id = :hotelId"
          + " AND r.status = :status AND r.deletedAt IS NULL")
  Page<Reservation> findByHotelIdAndStatus(
      @Param("hotelId") UUID hotelId, @Param("status") ReservationStatus status, Pageable pageable);

  // ── Admin : toutes les réservations ──────────────────────────────────────

  Page<Reservation> findByDeletedAtIsNull(Pageable pageable);

  Page<Reservation> findByStatusAndDeletedAtIsNull(ReservationStatus status, Pageable pageable);

  // ── Vérification de disponibilité (détection de chevauchement) ───────────

  long countByStatusAndDeletedAtIsNull(ReservationStatus status);

  @Query(
      "SELECT COUNT(r) FROM Reservation r"
          + " WHERE r.property.id = :propertyId"
          + " AND r.status = 'CONFIRMED'"
          + " AND r.deletedAt IS NULL"
          + " AND r.checkInDate < :checkOutDate"
          + " AND r.checkOutDate > :checkInDate")
  long countOverlappingConfirmed(
      @Param("propertyId") UUID propertyId,
      @Param("checkInDate") LocalDate checkInDate,
      @Param("checkOutDate") LocalDate checkOutDate);
}
