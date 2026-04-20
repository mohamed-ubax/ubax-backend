package com.africa.ubaxplatform.property.repository;

import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.entity.Property;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

  @Query(
      """
      SELECT p FROM Property p
      WHERE (:status   IS NULL OR p.status          = :status)
        AND (:city     IS NULL OR LOWER(p.city)     = LOWER(:city))
        AND (:propType IS NULL OR p.propertyType    = :propType)
        AND (:txType   IS NULL OR p.transactionType = :txType)
        AND (:minPrice IS NULL OR p.price           >= :minPrice)
        AND (:maxPrice IS NULL OR p.price           <= :maxPrice)
        AND (:agencyId IS NULL OR p.agency.id       = :agencyId)
        AND (:ownerId  IS NULL OR p.owner.id        = :ownerId)
      """)
  Page<Property> findWithFilters(
      @Param("status") PropertyStatus status,
      @Param("city") String city,
      @Param("propType") String propType,
      @Param("txType") String txType,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("agencyId") UUID agencyId,
      @Param("ownerId") UUID ownerId,
      Pageable pageable);

  long countByAgencyIdAndStatus(UUID agencyId, PropertyStatus status);

  long countByOwnerIdAndStatus(UUID ownerId, PropertyStatus status);

  long countByAgencyId(UUID agencyId);
}
