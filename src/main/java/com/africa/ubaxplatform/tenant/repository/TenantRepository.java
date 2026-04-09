package com.africa.ubaxplatform.tenant.repository;

import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

  Optional<Tenant> findByUserId(UUID userId);

  boolean existsByUserId(UUID userId);

  Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);

  List<Tenant> findByQualifiedTrue();
}
