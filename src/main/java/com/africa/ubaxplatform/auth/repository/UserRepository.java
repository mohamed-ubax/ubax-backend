package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPhone(String phone);

  Optional<User> findByKeycloakId(String keycloakId);

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);
}
