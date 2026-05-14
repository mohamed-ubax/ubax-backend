package com.africa.ubaxplatform.technicien.repository;

import com.africa.ubaxplatform.technicien.entity.Technicien;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicienRepository extends JpaRepository<Technicien, UUID> {

  Optional<Technicien> findByIdAndDeletedAtIsNull(UUID id);

  Page<Technicien> findByAgencyIdAndDeletedAtIsNull(UUID agencyId, Pageable pageable);

  Page<Technicien> findByAgencyIdAndAvailableAndDeletedAtIsNull(
      UUID agencyId, boolean available, Pageable pageable);

  Page<Technicien> findByHotelIdAndDeletedAtIsNull(UUID hotelId, Pageable pageable);

  Page<Technicien> findByHotelIdAndAvailableAndDeletedAtIsNull(
      UUID hotelId, boolean available, Pageable pageable);
}
