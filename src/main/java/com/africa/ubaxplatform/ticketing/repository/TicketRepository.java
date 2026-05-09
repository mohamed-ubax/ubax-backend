package com.africa.ubaxplatform.ticketing.repository;

import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import com.africa.ubaxplatform.ticketing.entity.Ticket;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  /** Tous les tickets d'une agence (via contrat → bien → agence). */
  @Query(
      "SELECT t FROM Ticket t WHERE t.contract.property.agency.id = :agencyId"
          + " ORDER BY t.createdAt DESC")
  Page<Ticket> findByAgencyId(@Param("agencyId") UUID agencyId, Pageable pageable);

  /** Tickets d'une agence filtrés par statut. */
  @Query(
      "SELECT t FROM Ticket t WHERE t.contract.property.agency.id = :agencyId"
          + " AND t.status = :status ORDER BY t.createdAt DESC")
  Page<Ticket> findByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") TicketStatus status, Pageable pageable);

  /** Tickets d'une agence assignés à un agent spécifique. */
  @Query(
      "SELECT t FROM Ticket t WHERE t.contract.property.agency.id = :agencyId"
          + " AND t.assignedTo.id = :userId ORDER BY t.createdAt DESC")
  Page<Ticket> findByAgencyIdAndAssignedTo(
      @Param("agencyId") UUID agencyId, @Param("userId") UUID userId, Pageable pageable);

  /** Nombre de tickets d'une agence par statut (dashboard SAV). */
  @Query(
      "SELECT COUNT(t) FROM Ticket t WHERE t.contract.property.agency.id = :agencyId"
          + " AND t.status = :status")
  long countByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") TicketStatus status);

  @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status IN :statuses")
  long countByStatusIn(@Param("statuses") Collection<TicketStatus> statuses);

  Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

  Page<Ticket> findByReporterKeycloakId(String keycloakId, Pageable pageable);
}
