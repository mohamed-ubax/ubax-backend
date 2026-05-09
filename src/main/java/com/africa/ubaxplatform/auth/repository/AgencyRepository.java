package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.entity.Agency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

  boolean existsByEmail(String email);

  Optional<Agency> findByEmail(String email);

  Optional<Agency> findByIdAndDeletedAtIsNull(UUID id);

  Page<Agency> findByDeletedAtIsNull(Pageable pageable);

  Page<Agency> findByActiveAndDeletedAtIsNull(boolean active, Pageable pageable);

  long countByActiveAndDeletedAtIsNull(boolean active);
}
