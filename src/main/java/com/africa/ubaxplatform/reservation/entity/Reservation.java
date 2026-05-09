package com.africa.ubaxplatform.reservation.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reservations", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Reservation extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "property_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_reservation_property"))
  private Property property;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "client_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_reservation_client"))
  private User client;

  @Column(name = "check_in_date", nullable = false)
  private LocalDate checkInDate;

  @Column(name = "check_out_date", nullable = false)
  private LocalDate checkOutDate;

  @Column(name = "number_of_nights", nullable = false)
  private int numberOfNights;

  @Column(name = "guest_count", nullable = false)
  private int guestCount;

  /** Snapshot du prix par nuit au moment de la réservation. */
  @Column(name = "price_per_night", nullable = false, precision = 15, scale = 2)
  private BigDecimal pricePerNight;

  @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private ReservationStatus status;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "cancellation_reason", columnDefinition = "TEXT")
  private String cancellationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "cancelled_by_id",
      foreignKey = @ForeignKey(name = "fk_reservation_cancelled_by"))
  private User cancelledBy;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
