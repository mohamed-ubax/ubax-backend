package com.africa.ubaxplatform.visitappointment.repository;

import com.africa.ubaxplatform.visitappointment.entity.AgencyVisitAvailability;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgencyVisitAvailabilityRepository
    extends JpaRepository<AgencyVisitAvailability, UUID> {

  /**
   * Récupère la configuration de disponibilité d'un bien (1:1 relation).
   *
   * @param propertyId ID du bien
   * @return Optional de configuration
   */
  Optional<AgencyVisitAvailability> findByPropertyId(UUID propertyId);

  /**
   * Vérifie l'existence d'une configuration pour un bien.
   *
   * @param propertyId ID du bien
   * @return true si existe
   */
  boolean existsByPropertyId(UUID propertyId);
}
