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

  // ── Partenaire agence : dossiers liés via contrats de l'agence ───────────

  @Query(
      "SELECT DISTINCT t FROM Tenant t JOIN Contract c ON c.tenant = t"
          + " WHERE c.property.agency.id = :agencyId AND t.deletedAt IS NULL")
  Page<Tenant> findByAgencyId(@Param("agencyId") UUID agencyId, Pageable pageable);

  @Query(
      "SELECT DISTINCT t FROM Tenant t JOIN Contract c ON c.tenant = t"
          + " WHERE c.property.agency.id = :agencyId AND t.status = :status AND t.deletedAt IS NULL")
  Page<Tenant> findByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") TenantStatus status, Pageable pageable);
}
