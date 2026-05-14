package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Requête de déclenchement d'une intervention technique sur un ticket.
 *
 * <p>Deux modes :
 *
 * <ul>
 *   <li><b>Technicien enregistré</b> : fournir {@code technicienId}. Les coordonnées sont
 *       récupérées automatiquement depuis le profil du technicien.
 *   <li><b>Technicien libre</b> : fournir {@code technicianName} + {@code technicianPhone} (utile
 *       pour un prestataire ponctuel non référencé).
 * </ul>
 *
 * <p>Au moins l'un des deux modes est obligatoire — validé dans le service.
 */
@Getter
@Setter
public class ScheduleInterventionRequest {

  @Schema(
      description = "ID d'un technicien enregistré dans le système — prioritaire sur nom/téléphone",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  private UUID technicienId;

  @Schema(
      description = "Nom du prestataire (requis si technicienId absent)",
      example = "Mamadou Diallo")
  private String technicianName;

  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format invalide. Ex : +2250712345678")
  @Schema(
      description = "Téléphone international du prestataire (requis si technicienId absent)",
      example = "+2250712345678")
  private String technicianPhone;

  @NotNull(message = "La date d'intervention est obligatoire")
  @Future(message = "La date d'intervention doit être dans le futur")
  @Schema(
      description = "Date et heure planifiées pour l'intervention",
      example = "2026-04-25T10:00:00")
  private LocalDateTime interventionScheduledAt;

  @DecimalMin(value = "0", message = "Le prix d'intervention ne peut pas être négatif")
  @Schema(
      description = "Prix d'intervention négocié en XOF (saisi par l'agent après négociation)",
      example = "75000")
  private BigDecimal interventionPrice;
}
