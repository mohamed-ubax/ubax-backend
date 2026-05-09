package com.africa.ubaxplatform.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminHotelResponse(
    UUID id,
    String name,
    String city,
    String email,
    String phone,
    String logoUrl,
    Integer stars,
    Integer totalRooms,
    String subscriptionPlan,
    LocalDateTime subscriptionExpiresAt,
    boolean subscriptionActive,
    boolean verified,
    LocalDateTime verifiedAt,
    boolean active,
    LocalDateTime createdAt) {}
