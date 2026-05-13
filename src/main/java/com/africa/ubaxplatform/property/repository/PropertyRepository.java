package com.africa.ubaxplatform.property.repository;

import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.entity.Property;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
        AND (:city     IS NULL OR LOWER(p.city)     = :city)
        AND (:propType IS NULL OR p.propertyType    = :propType)
        AND (:txType   IS NULL OR p.transactionType = :txType)
        AND (:minPrice IS NULL OR p.price           >= :minPrice)
        AND (:maxPrice IS NULL OR p.price           <= :maxPrice)
        AND (:agencyId IS NULL OR p.agency.id       = :agencyId)
        AND (:ownerId  IS NULL OR p.owner.id        = :ownerId)
        AND (:hotelId  IS NULL OR p.hotel.id        = :hotelId)
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
      @Param("hotelId") UUID hotelId,
      Pageable pageable);

  long countByAgencyIdAndStatus(UUID agencyId, PropertyStatus status);

  long countByOwnerIdAndStatus(UUID ownerId, PropertyStatus status);

  long countByAgencyId(UUID agencyId);

  long countByStatus(PropertyStatus status);

  /** Vérifie si un bien appartenant à ce bailleur est déjà géré par une autre agence. */
  boolean existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(UUID ownerId, UUID agencyId);

  /**
   * Détecte tout bien déjà rattaché à une agence différente dans un rayon donné (Haversine).
   * Utilisé pour signaler un conflit géographique sur les demandes de bailleur inconnu.
   */
  @Query(
      value =
          """
          SELECT COUNT(*) > 0
          FROM administrative.properties p
          WHERE p.agency_id IS NOT NULL
            AND p.agency_id != CAST(:agencyId AS uuid)
            AND p.latitude  IS NOT NULL
            AND p.longitude IS NOT NULL
            AND (6371000 * acos(
                  LEAST(1.0,
                    cos(radians(CAST(:lat AS double precision)))
                    * cos(radians(CAST(p.latitude  AS double precision)))
                    * cos(radians(CAST(p.longitude AS double precision)) - radians(CAST(:lng AS double precision)))
                    + sin(radians(CAST(:lat AS double precision)))
                    * sin(radians(CAST(p.latitude  AS double precision)))
                  )
                )) < CAST(:radiusMeters AS double precision)
          """,
      nativeQuery = true)
  boolean existsNearLocationForOtherAgency(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("agencyId") String agencyId,
      @Param("radiusMeters") double radiusMeters);

  /** Biens PUBLISHED dont la date d'expiration est dépassée → à archiver. */
  @Query(
      "SELECT p FROM Property p WHERE p.status = 'PUBLISHED' AND p.expiresAt IS NOT NULL AND p.expiresAt < :now")
  List<Property> findExpiredPublished(@Param("now") LocalDateTime now);

  /** Biens encore marqués boostés mais dont le boost a expiré → désactiver le flag. */
  @Query(
      "SELECT p FROM Property p WHERE p.boosted = true AND p.boostExpiresAt IS NOT NULL AND p.boostExpiresAt < :now")
  List<Property> findExpiredBoosts(@Param("now") LocalDateTime now);
}
