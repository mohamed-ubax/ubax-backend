package com.africa.ubaxplatform.bailleur.repository;

import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.africa.ubaxplatform.bailleur.entity.BailleurApplication;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BailleurApplicationRepository extends JpaRepository<BailleurApplication, UUID> {

  Page<BailleurApplication> findByAgencyId(UUID agencyId, Pageable pageable);

  boolean existsByPhoneAndStatus(String phone, BailleurApplicationStatus status);
}
