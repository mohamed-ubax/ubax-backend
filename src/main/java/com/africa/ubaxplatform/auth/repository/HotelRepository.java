package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.entity.Hotel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {

  Optional<Hotel> findByIdAndDeletedAtIsNull(UUID id);

  Page<Hotel> findByDeletedAtIsNull(Pageable pageable);

  Page<Hotel> findByActiveAndDeletedAtIsNull(boolean active, Pageable pageable);

  Page<Hotel> findByActiveAndDeletedAtIsNullAndCityIgnoreCase(
      boolean active, String city, Pageable pageable);

  long countByActiveAndDeletedAtIsNull(boolean active);
}
