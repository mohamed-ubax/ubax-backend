package com.africa.ubaxplatform.contract.repository;

import com.africa.ubaxplatform.auth.entity.User;
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

  /**
   * Recherche flexible pour une agence : filtre optionnel par statut et/ou mot-clé (titre du bien,
   * numéro de contrat, prénom/nom du locataire). Les paramètres null sont ignorés.
   */
  @Query(
      value =
          "SELECT c FROM Contract c LEFT JOIN c.tenant t LEFT JOIN t.user tu"
              + " WHERE c.property.agency.id = :agencyId"
              + " AND (:status IS NULL OR c.status = :status)"
              + " AND (:search IS NULL OR ("
              + "   LOWER(c.property.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(c.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ))",
      countQuery =
          "SELECT COUNT(c) FROM Contract c LEFT JOIN c.tenant t LEFT JOIN t.user tu"
              + " WHERE c.property.agency.id = :agencyId"
              + " AND (:status IS NULL OR c.status = :status)"
              + " AND (:search IS NULL OR ("
              + "   LOWER(c.property.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(c.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ))")
  Page<Contract> searchByAgency(
      @Param("agencyId") UUID agencyId,
      @Param("status") ContractStatus status,
      @Param("search") String search,
      Pageable pageable);

  /**
   * Recherche flexible pour un propriétaire : filtre optionnel par statut et/ou mot-clé (titre du
   * bien, numéro de contrat, prénom/nom du locataire). Les paramètres null sont ignorés.
   */
  @Query(
      value =
          "SELECT c FROM Contract c LEFT JOIN c.tenant t LEFT JOIN t.user tu"
              + " WHERE c.owner.id = :ownerId"
              + " AND (:status IS NULL OR c.status = :status)"
              + " AND (:search IS NULL OR ("
              + "   LOWER(c.property.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(c.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ))",
      countQuery =
          "SELECT COUNT(c) FROM Contract c LEFT JOIN c.tenant t LEFT JOIN t.user tu"
              + " WHERE c.owner.id = :ownerId"
              + " AND (:status IS NULL OR c.status = :status)"
              + " AND (:search IS NULL OR ("
              + "   LOWER(c.property.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(c.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ))")
  Page<Contract> searchByOwner(
      @Param("ownerId") UUID ownerId,
      @Param("status") ContractStatus status,
      @Param("search") String search,
      Pageable pageable);

  /** Recherche globale admin : filtre optionnel par statut et/ou mot-clé. */
  @Query(
      value =
          "SELECT c FROM Contract c LEFT JOIN c.tenant t LEFT JOIN t.user tu"
              + " WHERE (:status IS NULL OR c.status = :status)"
              + " AND (:search IS NULL OR ("
              + "   LOWER(c.property.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(c.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ))",
      countQuery =
          "SELECT COUNT(c) FROM Contract c LEFT JOIN c.tenant t LEFT JOIN t.user tu"
              + " WHERE (:status IS NULL OR c.status = :status)"
              + " AND (:search IS NULL OR ("
              + "   LOWER(c.property.title) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(c.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.firstName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + "   OR LOWER(tu.lastName) LIKE LOWER(CONCAT('%', :search, '%'))"
              + " ))")
  Page<Contract> searchAll(
      @Param("status") ContractStatus status, @Param("search") String search, Pageable pageable);

  /** Locataires distincts d'une agence — clients ayant au moins un contrat sur un bien agence. */
  @Query(
      value =
          "SELECT u FROM User u WHERE u.id IN ("
              + "SELECT DISTINCT t.user.id FROM Contract c JOIN c.tenant t"
              + " WHERE c.property.agency.id = :agencyId AND t.deletedAt IS NULL)",
      countQuery =
          "SELECT COUNT(u) FROM User u WHERE u.id IN ("
              + "SELECT DISTINCT t.user.id FROM Contract c JOIN c.tenant t"
              + " WHERE c.property.agency.id = :agencyId AND t.deletedAt IS NULL)")
  Page<User> findDistinctClientsByAgencyId(@Param("agencyId") UUID agencyId, Pageable pageable);

  /** Tous les contrats actifs d'un type donné — utilisé par le PaymentSchedulerJob. */
  @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.contractType = :contractType")
  List<Contract> findAllByStatusAndContractType(
      @Param("status") ContractStatus status, @Param("contractType") String contractType);

  /** Comptage par statut pour une agence — utilisé par l'endpoint stats. */
  @Query(
      "SELECT c.status, COUNT(c) FROM Contract c"
          + " WHERE c.property.agency.id = :agencyId GROUP BY c.status")
  List<Object[]> countByAgencyGroupByStatus(@Param("agencyId") UUID agencyId);

  /** Comptage par statut pour un propriétaire — utilisé par l'endpoint stats. */
  @Query("SELECT c.status, COUNT(c) FROM Contract c WHERE c.owner.id = :ownerId GROUP BY c.status")
  List<Object[]> countByOwnerGroupByStatus(@Param("ownerId") UUID ownerId);

  /** Comptage global par statut — réservé aux admins. */
  @Query("SELECT c.status, COUNT(c) FROM Contract c GROUP BY c.status")
  List<Object[]> countAllGroupByStatus();
}
