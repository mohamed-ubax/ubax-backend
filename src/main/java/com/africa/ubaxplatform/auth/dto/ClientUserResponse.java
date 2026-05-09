package com.africa.ubaxplatform.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientUserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String city,
    String avatarUrl,
    boolean emailVerified,
    boolean phoneVerified,
    boolean identityVerified,
    boolean active,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt) {}
