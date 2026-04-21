package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.entity.Hotel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {}
