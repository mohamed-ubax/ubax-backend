package com.africa.ubaxplatform.property.repository;

import com.africa.ubaxplatform.property.entity.PropertyDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDocumentRepository extends JpaRepository<PropertyDocument, UUID> {

  List<PropertyDocument> findByPropertyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID propertyId);

  List<PropertyDocument> findByPropertyIdAndVisibleToPublicTrueAndDeletedAtIsNull(UUID propertyId);

  // ── Archivage (soft-deleted) ────────────────────────────────────

  List<PropertyDocument> findByPropertyIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(UUID propertyId);

  java.util.Optional<PropertyDocument> findByIdAndDeletedAtIsNotNull(UUID id);
}
