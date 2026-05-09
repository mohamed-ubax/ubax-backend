package com.africa.ubaxplatform.document.repository;

import com.africa.ubaxplatform.document.codeList.RefType;
import com.africa.ubaxplatform.document.entity.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

  List<Document> findByRefIdAndRefType(UUID refId, RefType refType);

  Optional<Document> findTopByRefIdAndRefTypeOrderByCreatedAtDesc(UUID refId, RefType refType);
}
