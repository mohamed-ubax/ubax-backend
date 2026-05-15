package com.africa.ubaxplatform.favorite.repository;

import com.africa.ubaxplatform.favorite.entity.PropertyFavorite;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyFavoriteRepository extends JpaRepository<PropertyFavorite, UUID> {

  Optional<PropertyFavorite> findByUserKeycloakIdAndPropertyId(String keycloakId, UUID propertyId);

  Page<PropertyFavorite> findByUserKeycloakId(String keycloakId, Pageable pageable);

  boolean existsByUserKeycloakIdAndPropertyId(String keycloakId, UUID propertyId);

  void deleteByUserKeycloakIdAndPropertyId(String keycloakId, UUID propertyId);
}
