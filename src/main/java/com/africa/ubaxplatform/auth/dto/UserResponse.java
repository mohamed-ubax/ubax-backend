package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/** Représentation publique d'un utilisateur retournée par l'API. */
public record UserResponse(
    UUID id,
    String keycloakId,

    // ── Identité ─────────────────────────────────────────────────
    String firstName,
    String lastName,
    String email,
    String phone,
    LocalDate dateOfBirth,

    // ── Localisation ─────────────────────────────────────────────
    String address,
    String city,
    String country,
    String language,

    // ── Avatar ───────────────────────────────────────────────────
    String avatarUrl,

    // ── Rôles & structure partenaire ─────────────────────────────
    Set<UserRole> roles,
    PartnerRole partnerRole,
    UUID agencyId,
    String agencyName,
    UUID hotelId,
    String hotelName,

    // ── Vérifications ────────────────────────────────────────────
    boolean emailVerified,
    boolean phoneVerified,
    boolean identityVerified,

    // ── État du compte ────────────────────────────────────────────
    boolean active,
    LocalDateTime lastLoginAt,

    // ── Audit ────────────────────────────────────────────────────
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
