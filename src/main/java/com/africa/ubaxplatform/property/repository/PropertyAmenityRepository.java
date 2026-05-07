package com.africa.ubaxplatform.property.repository;

import com.africa.ubaxplatform.property.entity.PropertyAmenity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, UUID> {

  List<PropertyAmenity> findAllByPropertyId(UUID propertyId);

  void deleteAllByPropertyId(UUID propertyId);
}
