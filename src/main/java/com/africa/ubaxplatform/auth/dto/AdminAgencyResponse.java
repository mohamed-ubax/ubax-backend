package com.africa.ubaxplatform.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAgencyResponse(
    UUID id,
    String name,
    String city,
    String email,
    String phone,
    String logoUrl,
    String subscriptionPlan,
    LocalDateTime subscriptionExpiresAt,
    boolean subscriptionActive,
    boolean verified,
    LocalDateTime verifiedAt,
    boolean active,
    LocalDateTime createdAt) {}
