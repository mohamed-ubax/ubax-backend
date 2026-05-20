package com.africa.ubaxplatform.mandate.repository;

import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import com.africa.ubaxplatform.mandate.entity.ManagementContract;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagementContractRepository extends JpaRepository<ManagementContract, UUID> {

  @Query(
      "SELECT m FROM ManagementContract m"
          + " WHERE m.agency.id = :agencyId"
          + " AND (:status IS NULL OR m.status = :status)"
          + " AND m.deletedAt IS NULL")
  Page<ManagementContract> findByAgencyId(
      @Param("agencyId") UUID agencyId, @Param("status") MandateStatus status, Pageable pageable);

  boolean existsByAgencyIdAndOwnerIdAndStatusIn(
      UUID agencyId, UUID ownerId, java.util.List<MandateStatus> statuses);
}
