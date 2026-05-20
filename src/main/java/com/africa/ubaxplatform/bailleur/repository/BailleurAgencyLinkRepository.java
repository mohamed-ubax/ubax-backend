package com.africa.ubaxplatform.bailleur.repository;

import com.africa.ubaxplatform.bailleur.entity.BailleurAgencyLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BailleurAgencyLinkRepository extends JpaRepository<BailleurAgencyLink, UUID> {

  boolean existsByBailleurUserIdAndAgencyId(UUID bailleurUserId, UUID agencyId);

  List<BailleurAgencyLink> findByBailleurUserId(UUID bailleurUserId);

  Page<BailleurAgencyLink> findByAgencyId(UUID agencyId, Pageable pageable);
}
