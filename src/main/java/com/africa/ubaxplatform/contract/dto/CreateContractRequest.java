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

  @Schema(description = "Identifiant du dossier locataire (requis pour LEASE et RENT_TO_OWN)")
  private UUID tenantId;

  @NotBlank
  @Pattern(
      regexp = "LEASE|SALE|RESERVATION|MANDATE|RENT_TO_OWN",
      message = "Valeurs : LEASE, SALE, RESERVATION, MANDATE, RENT_TO_OWN")
  @Schema(
      description =
          "Type de contrat :\n"
              + "- `LEASE` — Bail de location (monthlyRent requis)\n"
              + "- `SALE` — Acte de vente (salePrice requis)\n"
              + "- `RENT_TO_OWN` — Location-vente : le locataire verse une mensualité"
              + " (monthlyInstallment) qui s'impute sur le prix total du bien (salePrice)"
              + " jusqu'au terme fixé par endDate\n"
              + "- `RESERVATION` — Contrat de réservation (reservationDeposit requis)\n"
              + "- `MANDATE` — Mandat de gestion locative",
      allowableValues = {"LEASE", "SALE", "RENT_TO_OWN", "RESERVATION", "MANDATE"})
  private String contractType;

  @NotNull
  @Schema(description = "Date de début du contrat", example = "2026-06-01")
  private LocalDate startDate;

  @Schema(
      description =
          "Date de fin du contrat.\n"
              + "- `LEASE` : null = durée indéterminée\n"
              + "- `RENT_TO_OWN` : **obligatoire** — détermine la durée du programme"
              + " (ex. 2031-06-01 pour 5 ans)\n"
              + "- `SALE` / `MANDATE` : optionnel",
      example = "2031-06-01")
  private LocalDate endDate;

  @Schema(description = "Loyer mensuel hors charges (XOF) — LEASE uniquement")
  private BigDecimal monthlyRent;

  @Schema(description = "Charges mensuelles (XOF) — LEASE uniquement")
  private BigDecimal monthlyCharges;

  @Schema(description = "Montant de la caution (XOF) — LEASE et RENT_TO_OWN")
  private BigDecimal depositAmount;

  @Schema(
      description =
          "Prix total du bien (XOF).\n"
              + "- `SALE` : prix de cession\n"
              + "- `RENT_TO_OWN` : valeur totale à acquérir"
              + " (monthlyInstallment × durée = salePrice idéalement)")
  private BigDecimal salePrice;

  @Schema(
      description =
          "Mensualité versée vers l'acquisition du bien (XOF) — **RENT_TO_OWN uniquement**.\n"
              + "Exemple : bien à 12 000 000 XOF sur 60 mois → monthlyInstallment = 200 000 XOF")
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
