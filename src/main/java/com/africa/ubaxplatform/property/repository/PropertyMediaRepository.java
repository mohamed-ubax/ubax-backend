package com.africa.ubaxplatform.property.repository;

import com.africa.ubaxplatform.property.entity.PropertyMedia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, UUID> {

  List<PropertyMedia> findByPropertyIdOrderBySortOrderAsc(UUID propertyId);

  Optional<PropertyMedia> findByPropertyIdAndCoverTrue(UUID propertyId);

  boolean existsByPropertyId(UUID propertyId);

  @Modifying
  @Query("UPDATE PropertyMedia m SET m.cover = FALSE WHERE m.property.id = :propertyId")
  void clearCoversForProperty(@Param("propertyId") UUID propertyId);
}
