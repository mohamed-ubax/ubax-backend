package com.africa.ubaxplatform.contract.repository;

import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.entity.Contract;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Query("SELECT c FROM Contract c WHERE c.property.agency.id = :agencyId")
  Page<Contract> findByAgencyId(@Param("agencyId") UUID agencyId, Pageable pageable);

  @Query("SELECT c FROM Contract c WHERE c.property.agency.id = :agencyId AND c.status = :status")
  Page<Contract> findByAgencyIdAndStatus(
      @Param("agencyId") UUID agencyId, @Param("status") ContractStatus status, Pageable pageable);

  Page<Contract> findByOwnerId(UUID ownerId, Pageable pageable);

  Page<Contract> findByOwnerIdAndStatus(UUID ownerId, ContractStatus status, Pageable pageable);

  /** Tous les contrats actifs d'un type donné — utilisé par le PaymentSchedulerJob. */
  @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.contractType = :contractType")
  List<Contract> findAllByStatusAndContractType(
      @Param("status") ContractStatus status, @Param("contractType") String contractType);
}
