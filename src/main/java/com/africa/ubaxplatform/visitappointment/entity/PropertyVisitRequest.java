package com.africa.ubaxplatform.visitappointment.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.visitappointment.codeList.VisitRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Demande de visite d'un bien immobilier.
 *
 * <p>Workflow : {@code PENDING → CONFIRMED / REJECTED | CANCELLED}
 *
 * <p>Un client demande une visite pour une date et un créneau spécifiques. L'agence propriétaire du
 * bien confirme, rejette ou propose une autre date.
 */
@Entity
@Table(name = "property_visit_requests", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PropertyVisitRequest extends BaseEntity {

  /** Bien immobilier visité */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "property_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_visit_request_property"))
  private Property property;

  /** Client demandeur de visite */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "client_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_visit_request_client"))
  private User client;

  /** Agent assigné de l'agence (optionnel) — qui effectuera la visite */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agent_id", foreignKey = @ForeignKey(name = "fk_visit_request_agent"))
  private User agent;

  /** Date demandée par le client (au format YYYY-MM-DD) */
  @Column(name = "requested_date", nullable = false)
  private LocalDate requestedDate;

  /** Créneau demandé (ex: "10:00-14:00") */
  @Column(name = "requested_time_slot", nullable = false)
  private String requestedTimeSlot;

  /** Statut actuel de la demande */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private VisitRequestStatus status;

  /** Date confirmée par l'agence (valorisée si status = CONFIRMED) */
  @Column(name = "confirmed_date")
  private LocalDate confirmedDate;

  /** Créneau confirmé par l'agence (ex: "14:00-18:00") */
  @Column(name = "confirmed_time_slot")
  private String confirmedTimeSlot;

  /** Motif du rejet (si status = REJECTED) */
  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  /** Notes du client (optionnel) */
  @Column(name = "client_notes", length = 1000)
  private String clientNotes;
}
