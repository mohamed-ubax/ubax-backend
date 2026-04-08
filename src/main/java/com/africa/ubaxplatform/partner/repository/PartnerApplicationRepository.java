package com.africa.ubaxplatform.partner.repository;

import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerApplicationRepository extends JpaRepository<PartnerApplication, UUID> {

  Page<PartnerApplication> findByStatus(ApplicationStatus status, Pageable pageable);

  boolean existsByEmailAndStatusNot(String email, ApplicationStatus status);
}
