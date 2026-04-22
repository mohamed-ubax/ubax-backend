package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.entity.UserSubRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubRoleRepository extends JpaRepository<UserSubRole, UUID> {

  List<UserSubRole> findByUserId(UUID userId);

  List<UserSubRole> findByUserIdAndScope(UUID userId, RoleScope scope);

  Optional<UserSubRole> findByUserIdAndRoleAndScope(UUID userId, String role, RoleScope scope);

  boolean existsByUserIdAndRoleAndScope(UUID userId, String role, RoleScope scope);

  @Modifying
  @Query(
      "DELETE FROM UserSubRole r WHERE r.user.id = :userId AND r.role = :role AND r.scope = :scope")
  void deleteByUserIdAndRoleAndScope(
      @Param("userId") UUID userId, @Param("role") String role, @Param("scope") RoleScope scope);

  @Modifying
  @Query("DELETE FROM UserSubRole r WHERE r.user.id = :userId AND r.scope = :scope")
  void deleteByUserIdAndScope(@Param("userId") UUID userId, @Param("scope") RoleScope scope);
}
