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
}
