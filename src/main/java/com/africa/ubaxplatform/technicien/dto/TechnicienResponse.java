package com.africa.ubaxplatform.technicien.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TechnicienResponse(
    UUID id,
    String firstName,
    String lastName,
    String fullName,
    String phone,
    String email,
    String avatarUrl,
    String profession,
    String address,
    boolean available,
    UUID agencyId,
    String agencyName,
    UUID hotelId,
    String hotelName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
