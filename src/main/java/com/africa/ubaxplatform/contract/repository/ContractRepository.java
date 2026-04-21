package com.africa.ubaxplatform.contract.repository;

import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.entity.Contract;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

  /** Nombre de contrats d'une agence par statut (via le bien associé). */
  @Query(
      "SELECT COUNT(c) FROM Contract c WHERE c.property.agency.id = :agencyId AND c.status = :status")
  long countByAgencyAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") ContractStatus status);
}
