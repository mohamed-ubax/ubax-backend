package com.africa.ubaxplatform.bailleur.repository;

import com.africa.ubaxplatform.bailleur.entity.BailleurAgencyLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BailleurAgencyLinkRepository extends JpaRepository<BailleurAgencyLink, UUID> {

  boolean existsByBailleurUserIdAndAgencyId(UUID bailleurUserId, UUID agencyId);

  List<BailleurAgencyLink> findByBailleurUserId(UUID bailleurUserId);
}
