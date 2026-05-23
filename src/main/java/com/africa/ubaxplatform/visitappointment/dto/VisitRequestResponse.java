package com.africa.ubaxplatform.visitappointment.dto;

import com.africa.ubaxplatform.visitappointment.codeList.VisitRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour une demande de visite.
 *
 * <p>Retourne l'état complet d'une demande (demandée, confirmée, rejetée, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "État d'une demande de visite")
public class VisitRequestResponse {

  @Schema(description = "ID unique de la demande", example = "123e4567-e89b-12d3-a456-426614174000")
  private UUID id;

  @Schema(description = "ID du bien", example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID propertyId;

  @Schema(description = "Titre du bien", example = "Villa de luxe Cocody")
  private String propertyTitle;

  @Schema(description = "ID du client demandeur", example = "550e8400-e29b-41d4-a716-446655440001")
  private UUID clientId;

  @Schema(description = "Nom du client", example = "Jean Kouassi")
  private String clientName;

  @Schema(
      description = "ID de l'agent assigné (optionnel)",
      example = "550e8400-e29b-41d4-a716-446655440002")
  private UUID agentId;

  @Schema(description = "Nom de l'agent (optionnel)", example = "Marie Amani")
  private String agentName;

  @Schema(description = "Date demandée", example = "2026-05-25")
  private LocalDate requestedDate;

  @Schema(description = "Créneau horaire demandé", example = "10:00-14:00")
  private String requestedTimeSlot;

  @Schema(description = "Statut actuel", example = "PENDING")
  private VisitRequestStatus status;

  @Schema(description = "Date confirmée par l'agence (si confirmée)", example = "2026-05-25")
  private LocalDate confirmedDate;

  @Schema(description = "Créneau confirmé par l'agence (si confirmée)", example = "10:00-14:00")
  private String confirmedTimeSlot;

  @Schema(description = "Motif du rejet (si rejetée)", example = "Bien vendu")
  private String rejectionReason;

  @Schema(description = "Notes du client", example = "Visite rapide demandée")
  private String clientNotes;

  @Schema(description = "Date de création", example = "2026-05-23T14:30:00")
  private LocalDateTime createdAt;

  @Schema(description = "Date de dernière modification", example = "2026-05-23T15:00:00")
  private LocalDateTime updatedAt;
}
