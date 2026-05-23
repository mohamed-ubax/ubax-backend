package com.africa.ubaxplatform.visitappointment.repository;

import com.africa.ubaxplatform.visitappointment.codeList.VisitRequestStatus;
import com.africa.ubaxplatform.visitappointment.entity.PropertyVisitRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyVisitRequestRepository extends JpaRepository<PropertyVisitRequest, UUID> {

  /**
   * Récupère les demandes de visite d'un bien.
   *
   * @param propertyId ID du bien
   * @param pageable Pagination
   * @return Page de demandes
   */
  Page<PropertyVisitRequest> findByPropertyIdOrderByCreatedAtDesc(
      UUID propertyId, Pageable pageable);

  /**
   * Récupère les demandes d'un client.
   *
   * @param clientId ID du client
   * @param pageable Pagination
   * @return Page de demandes
   */
  Page<PropertyVisitRequest> findByClientIdOrderByCreatedAtDesc(UUID clientId, Pageable pageable);

  /**
   * Récupère les demandes d'un client pour un bien.
   *
   * @param clientId ID du client
   * @param propertyId ID du bien
   * @return Optional de demande
   */
  Optional<PropertyVisitRequest> findByClientIdAndPropertyId(UUID clientId, UUID propertyId);

  /**
   * Compte les visites confirmées pour une date et un créneau donnés.
   *
   * @param propertyId ID du bien
   * @param date Date
   * @param timeSlot Créneau (ex: "10:00-14:00")
   * @return Nombre de visites confirmées
   */
  @Query(
      """
      SELECT COUNT(pvr) FROM PropertyVisitRequest pvr
      WHERE pvr.property.id = :propertyId
        AND pvr.confirmedDate = :date
        AND pvr.confirmedTimeSlot = :timeSlot
        AND pvr.status = 'CONFIRMED'
      """)
  long countConfirmedForDateAndSlot(
      @Param("propertyId") UUID propertyId,
      @Param("date") LocalDate date,
      @Param("timeSlot") String timeSlot);

  /**
   * Récupère les demandes d'une agence par propriété.
   *
   * @param agencyId ID de l'agence
   * @param status Statut optionnel
   * @param pageable Pagination
   * @return Page de demandes
   */
  @Query(
      """
      SELECT pvr FROM PropertyVisitRequest pvr
      WHERE pvr.property.agency.id = :agencyId
        AND (:status IS NULL OR pvr.status = :status)
      ORDER BY pvr.createdAt DESC
      """)
  Page<PropertyVisitRequest> findByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId,
      @Param("status") VisitRequestStatus status,
      Pageable pageable);

  /**
   * Récupère les demandes PENDING d'une agence (à traiter).
   *
   * @param agencyId ID de l'agence
   * @param pageable Pagination
   * @return Page de demandes
   */
  @Query(
      """
      SELECT pvr FROM PropertyVisitRequest pvr
      WHERE pvr.property.agency.id = :agencyId
        AND pvr.status = 'PENDING'
      ORDER BY pvr.createdAt ASC
      """)
  Page<PropertyVisitRequest> findPendingByAgencyId(
      @Param("agencyId") UUID agencyId, Pageable pageable);

  /**
   * Récupère les demandes CONFIRMED pour un bien.
   *
   * @param propertyId ID du bien
   * @return List de demandes
   */
  List<PropertyVisitRequest> findByPropertyIdAndStatus(UUID propertyId, VisitRequestStatus status);

  /**
   * Compte les demandes PENDING + CONFIRMED pour une agence.
   *
   * @param agencyId ID de l'agence
   * @return Nombre de demandes actives
   */
  @Query(
      """
      SELECT COUNT(pvr) FROM PropertyVisitRequest pvr
      WHERE pvr.property.agency.id = :agencyId
        AND pvr.status IN ('PENDING', 'CONFIRMED')
      """)
  long countActiveByAgencyId(@Param("agencyId") UUID agencyId);
}
