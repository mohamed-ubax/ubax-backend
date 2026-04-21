package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPhone(String phone);

  Optional<User> findByKeycloakId(String keycloakId);

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);

  @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r IN :roles")
  List<User> findAdminUsers(@Param("roles") Set<UserRole> roles);

  /**
   * Retourne tous les membres actifs (non supprimés) d'une agence.
   *
   * @param agencyId identifiant de l'agence
   * @return liste des utilisateurs rattachés à cette agence
   */
  @Query(
      "SELECT u FROM User u WHERE u.agency.id = :agencyId AND u.deletedAt IS NULL "
          + "ORDER BY u.firstName ASC")
  List<User> findActiveByAgencyId(@Param("agencyId") UUID agencyId);

  /**
   * Vérifie si un utilisateur (identifié par son UUID DB) est déjà membre d'une agence.
   *
   * @param userId identifiant de l'utilisateur
   * @return {@code true} si l'utilisateur est rattaché à une agence
   */
  @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :userId AND u.agency IS NOT NULL")
  boolean isMemberOfAnyAgency(@Param("userId") UUID userId);
}
