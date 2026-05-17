package com.africa.ubaxplatform.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContractRequest {

  @NotNull
  @Schema(description = "Identifiant du bien immobilier")
  private UUID propertyId;

  @NotNull
  @Schema(description = "Identifiant du propriétaire (User)")
  private UUID ownerId;

  @Schema(description = "Identifiant du dossier locataire (requis pour LEASE)")
  private UUID tenantId;

  @NotBlank
  @Pattern(
      regexp = "LEASE|SALE|RESERVATION|MANDATE",
      message = "Valeurs : LEASE, SALE, RESERVATION, MANDATE")
  @Schema(
      description = "Type de contrat",
      allowableValues = {"LEASE", "SALE", "RESERVATION", "MANDATE"})
  private String contractType;

  @NotNull
  @Schema(description = "Date de début du contrat", example = "2026-06-01")
  private LocalDate startDate;

  @Schema(
      description = "Date de fin du contrat (null = durée indéterminée)",
      example = "2027-06-01")
  private LocalDate endDate;

  @Schema(description = "Loyer mensuel hors charges (XOF) — LEASE uniquement")
  private BigDecimal monthlyRent;

  @Schema(description = "Charges mensuelles (XOF) — LEASE uniquement")
  private BigDecimal monthlyCharges;

  @Schema(description = "Montant de la caution (XOF)")
  private BigDecimal depositAmount;

  @Schema(description = "Prix de vente (XOF) — SALE et RENT_TO_OWN : prix total du bien")
  private BigDecimal salePrice;

  @Schema(description = "Mensualité versée vers l'acquisition (XOF) — RENT_TO_OWN uniquement")
  private BigDecimal monthlyInstallment;

  @Schema(description = "Acompte de réservation (XOF) — RESERVATION uniquement")
  private BigDecimal reservationDeposit;

  @Schema(description = "Durée de réservation en jours — RESERVATION uniquement")
  private Integer reservationDurationDays;

  @Schema(description = "Taux de commission agence (%)")
  private BigDecimal agencyCommissionRate;

  @Min(1)
  @Max(28)
  @Schema(description = "Jour du mois pour l'échéance du loyer (1-28, défaut: 5)")
  private Integer paymentDay;

  @Schema(description = "Clauses particulières du contrat")
  private String specialClauses;

  @Schema(description = "Conditions de résiliation anticipée")
  private String terminationConditions;
}
