package com.africa.ubaxplatform.bailleur.repository;

import com.africa.ubaxplatform.bailleur.entity.BailleurApplicationProperty;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BailleurApplicationPropertyRepository
    extends JpaRepository<BailleurApplicationProperty, UUID> {

  List<BailleurApplicationProperty> findByApplicationId(UUID applicationId);
}
