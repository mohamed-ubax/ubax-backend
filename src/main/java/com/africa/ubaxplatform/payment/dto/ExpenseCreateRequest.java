package com.africa.ubaxplatform.payment.dto;

import com.africa.ubaxplatform.payment.codeList.CostCenter;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Requête de création d'une dépense comptable")
public record ExpenseCreateRequest(
    @Schema(description = "UUID du bien concerné (requis si costCenter = PROPERTY_SPECIFIC)")
        UUID propertyId,
    @NotNull @Schema(description = "Catégorie de la dépense", example = "MAINTENANCE")
        ExpenseCategory category,
    @NotNull @Schema(description = "Centre de coût", example = "PROPERTY_SPECIFIC")
        CostCenter costCenter,
    @NotBlank
        @Schema(description = "Intitulé de la dépense", example = "Réparation toiture villa Cocody")
        String label,
    @NotNull @DecimalMin("0") @Schema(description = "Montant en XOF", example = "150000")
        BigDecimal amount,
    @Schema(description = "Mode de règlement", example = "BANK_TRANSFER")
        PaymentMethod paymentMethod,
    @NotNull @Schema(description = "Date de la dépense (ISO-8601)", example = "2025-10-12")
        LocalDate expenseDate,
    @Schema(description = "Nom du fournisseur ou prestataire", example = "SODECI") String provider,
    @Schema(description = "Référence facture ou bon de commande", example = "FAC-2025-1042")
        String invoiceReference,
    @Schema(description = "URL de la facture déjà uploadée dans MinIO") String justificationUrl,
    @Schema(description = "Note libre") String note) {}
