package com.africa.ubaxplatform.tenant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Corps de la requête de création d'un dossier locataire. Tous les champs sont optionnels. */
public record TenantCreateRequest(

    // ── Situation professionnelle ─────────────────────────────────
    @Size(max = 50) String employmentStatus,
    @Size(max = 255) String employerName,
    @DecimalMin("0") BigDecimal monthlyIncome,

    // ── Garant ───────────────────────────────────────────────────
    Boolean hasGuarantor,
    @Size(max = 255) String guarantorName,
    @Size(max = 20) String guarantorPhone,
    @Size(max = 255) String guarantorEmail,

    // ── Documents KYC (URLs MinIO pré-uploadées) ──────────────────
    @Size(max = 500) String idDocumentUrl,
    @Size(max = 30) String idDocumentType,
    @Size(max = 100) String idDocumentNumber,
    String idDocumentExpiry, // ISO date string yyyy-MM-dd
    @Size(max = 500) String incomeProofUrl,
    @Size(max = 500) String addressProofUrl) {}
