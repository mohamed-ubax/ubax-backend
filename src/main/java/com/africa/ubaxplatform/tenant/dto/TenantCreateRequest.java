package com.africa.ubaxplatform.tenant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Corps de la requête de création ou soumission d'un dossier locataire. */
public record TenantCreateRequest(

    // ── Situation professionnelle ─────────────────────────────────
    @Size(max = 50) String employmentStatus,
    @Size(max = 255) String employerName,
    @DecimalMin("0") BigDecimal monthlyIncome,

    // ── Garant ───────────────────────────────────────────────────
    Boolean hasGuarantor,
    @Size(max = 255) String guarantorName,
    @Size(max = 20) String guarantorPhone,
    @Size(max = 255) String guarantorEmail) {}
