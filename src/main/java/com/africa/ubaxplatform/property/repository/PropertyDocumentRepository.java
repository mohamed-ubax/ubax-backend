package com.africa.ubaxplatform.property.repository;

import com.africa.ubaxplatform.property.entity.PropertyDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDocumentRepository extends JpaRepository<PropertyDocument, UUID> {

  List<PropertyDocument> findByPropertyIdOrderByCreatedAtDesc(UUID propertyId);

  List<PropertyDocument> findByPropertyIdAndVisibleToPublicTrue(UUID propertyId);
}
