package com.africa.ubaxplatform.tenant.repository;

import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

  Optional<Tenant> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);

  List<Tenant> findByQualifiedTrue();

  // ── Admin : tous les dossiers non archivés ────────────────────────────────

  Page<Tenant> findByDeletedAtIsNull(Pageable pageable);

  Page<Tenant> findByStatusAndDeletedAtIsNull(TenantStatus status, Pageable pageable);

  // ── Partenaire agence : dossiers liés via le bien ciblé ─────────────────

  @Query("SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.property.agency.id = :agencyId")
  Page<Tenant> findByAgencyId(@Param("agencyId") UUID agencyId, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND t.property.agency.id = :agencyId")
  Page<Tenant> findByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") TenantStatus status, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL"
          + " AND t.property.id = :propertyId AND t.property.agency.id = :agencyId")
  Page<Tenant> findByAgencyIdAndPropertyId(
      @Param("agencyId") UUID agencyId, @Param("propertyId") UUID propertyId, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND t.property.id = :propertyId AND t.property.agency.id = :agencyId")
  Page<Tenant> findByAgencyIdAndPropertyIdAndStatus(
      @Param("agencyId") UUID agencyId,
      @Param("propertyId") UUID propertyId,
      @Param("status") TenantStatus status,
      Pageable pageable);

  // ── Admin : filtre par bien ───────────────────────────────────────────────

  @Query("SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.property.id = :propertyId")
  Page<Tenant> findByPropertyId(@Param("propertyId") UUID propertyId, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND t.property.id = :propertyId")
  Page<Tenant> findByPropertyIdAndStatus(
      @Param("propertyId") UUID propertyId,
      @Param("status") TenantStatus status,
      Pageable pageable);

  // ── Dossiers sans contrat actif – pas de filtre par bien (création de contrat) ───────────────

  /** Locataires qualifiés sans aucun contrat DRAFT/PENDING_SIGNATURE/ACTIVE (toutes agences). */
  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.TERMINATED"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.CANCELLED)")
  Page<Tenant> findWithoutActiveContract(Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.TERMINATED"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.CANCELLED)")
  Page<Tenant> findByStatusWithoutActiveContract(
      @Param("status") TenantStatus status, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.property.agency.id = :agencyId"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.TERMINATED"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.CANCELLED)")
  Page<Tenant> findByAgencyIdWithoutActiveContract(
      @Param("agencyId") UUID agencyId, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND t.property.agency.id = :agencyId"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.TERMINATED"
          + " AND c.status <> com.africa.ubaxplatform.contract.codeList.ContractStatus.CANCELLED)")
  Page<Tenant> findByAgencyIdAndStatusWithoutActiveContract(
      @Param("agencyId") UUID agencyId, @Param("status") TenantStatus status, Pageable pageable);

  // ── Dossiers sans contrat pour un bien (candidatures en attente) ──────────

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.property.id = :propertyId"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t AND c.property.id = :propertyId)")
  Page<Tenant> findByPropertyIdWithoutContract(
      @Param("propertyId") UUID propertyId, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND t.property.id = :propertyId"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t AND c.property.id = :propertyId)")
  Page<Tenant> findByPropertyIdAndStatusWithoutContract(
      @Param("propertyId") UUID propertyId,
      @Param("status") TenantStatus status,
      Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL"
          + " AND t.property.id = :propertyId AND t.property.agency.id = :agencyId"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t AND c.property.id = :propertyId)")
  Page<Tenant> findByAgencyIdAndPropertyIdWithoutContract(
      @Param("agencyId") UUID agencyId, @Param("propertyId") UUID propertyId, Pageable pageable);

  @Query(
      "SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status"
          + " AND t.property.id = :propertyId AND t.property.agency.id = :agencyId"
          + " AND NOT EXISTS (SELECT 1 FROM Contract c WHERE c.tenant = t AND c.property.id = :propertyId)")
  Page<Tenant> findByAgencyIdAndPropertyIdAndStatusWithoutContract(
      @Param("agencyId") UUID agencyId,
      @Param("propertyId") UUID propertyId,
      @Param("status") TenantStatus status,
      Pageable pageable);

  // ── Vérification appartenance agence (pour qualify/reject PARTNER) ────────

  @Query(
      "SELECT COUNT(t) > 0 FROM Tenant t WHERE t.id = :tenantId AND t.deletedAt IS NULL"
          + " AND t.property.agency.id = :agencyId")
  boolean isLinkedToAgency(@Param("tenantId") UUID tenantId, @Param("agencyId") UUID agencyId);
}
