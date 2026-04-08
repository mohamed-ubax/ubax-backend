package com.africa.ubaxplatform.partner.repository;

import com.africa.ubaxplatform.partner.entity.ApplicationStatusLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusLogRepository extends JpaRepository<ApplicationStatusLog, UUID> {

  List<ApplicationStatusLog> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}
