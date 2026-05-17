package com.africa.ubaxplatform.tenant.dto;

import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Représentation publique d'un dossier locataire retournée par l'API. */
public record TenantResponse(
    UUID id,

    // ── Bien ciblé ────────────────────────────────────────────────
    UUID propertyId,

    // ── Identité utilisateur ──────────────────────────────────────
    UUID userId,
    String fullName,
    String email,

    // ── Situation professionnelle ─────────────────────────────────
    String employmentStatus,
    String employerName,
    BigDecimal monthlyIncome,

    // ── Garant ───────────────────────────────────────────────────
    boolean hasGuarantor,
    String guarantorName,
    String guarantorPhone,
    String guarantorEmail,

    // ── Documents KYC ────────────────────────────────────────────
    String idDocumentUrl,
    String idDocumentType,
    String idDocumentNumber,
    LocalDate idDocumentExpiry,
    String incomeProofUrl,
    String addressProofUrl,

    // ── Statut & qualification ────────────────────────────────────
    TenantStatus status,
    boolean qualified,
    LocalDateTime qualifiedAt,

    // ── Refus ────────────────────────────────────────────────────
    String rejectionReason,

    // ── Audit ─────────────────────────────────────────────────────
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
