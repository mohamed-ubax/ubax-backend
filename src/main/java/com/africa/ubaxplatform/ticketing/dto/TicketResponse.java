package com.africa.ubaxplatform.ticketing.dto;

import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant un ticket de maintenance ou SAV.
 *
 * @param id identifiant unique du ticket
 * @param contractId identifiant du contrat dans le cadre duquel l'incident est signalé
 * @param reporterId identifiant du locataire ayant déclaré l'incident
 * @param reporterName nom complet du reporter
 * @param assignedToId identifiant de l'agent SAV assigné, {@code null} si non assigné
 * @param assignedToName nom complet de l'agent assigné, {@code null} si non assigné
 * @param category catégorie de l'incident (LEAK, ELECTRICAL, LOCK, PLUMBING, APPLIANCE, STRUCTURE,
 *     PEST, COMMON_AREA, OTHER)
 * @param title titre court de l'incident
 * @param description description détaillée de l'incident
 * @param priority niveau d'urgence (LOW, NORMAL, HIGH, URGENT)
 * @param status statut du ticket dans son cycle de résolution
 * @param technicianName nom du prestataire ou technicien mandaté
 * @param technicianPhone numéro de téléphone du prestataire
 * @param interventionScheduledAt date et heure planifiées pour l'intervention
 * @param resolvedAt date et heure de résolution effective
 * @param closedAt date et heure de clôture définitive
 * @param repairCost coût total de la réparation en XOF
 * @param costImputedTo partie imputable pour le coût (OWNER, TENANT, SHARED)
 * @param resolutionNote note de résolution rédigée par l'agent SAV
 * @param rating évaluation de la résolution par le locataire (1 à 5 étoiles)
 * @param ratingComment commentaire libre du locataire lors de la clôture
 * @param createdAt date de création
 * @param updatedAt date de dernière modification
 */
public record TicketResponse(
    UUID id,
    UUID contractId,
    UUID reporterId,
    String reporterName,
    UUID assignedToId,
    String assignedToName,
    String category,
    String title,
    String description,
    String priority,
    TicketStatus status,
    String technicianName,
    String technicianPhone,
    LocalDateTime interventionScheduledAt,
    LocalDateTime resolvedAt,
    LocalDateTime closedAt,
    BigDecimal repairCost,
    String costImputedTo,
    String resolutionNote,
    Integer rating,
    String ratingComment,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
