package com.africa.ubaxplatform.ticketing.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant une pièce jointe liée directement à un ticket SAV.
 *
 * @param id identifiant unique de la pièce jointe
 * @param ticketId identifiant du ticket auquel cette pièce jointe est rattachée
 * @param uploadedById identifiant de l'utilisateur ayant uploadé la pièce jointe
 * @param uploaderName nom complet de l'utilisateur ayant uploadé la pièce jointe
 * @param fileUrl URL du fichier dans MinIO (bucket ticket-attachments)
 * @param fileName nom original du fichier au moment de l'upload
 * @param fileSize taille du fichier en octets
 * @param mimeType type MIME du fichier (image/jpeg, image/png, video/mp4...)
 * @param attachmentType catégorie de la pièce jointe (INCIDENT_PHOTO, INCIDENT_VIDEO,
 *     INTERVENTION_REPORT, INVOICE, OTHER)
 * @param caption légende ou description courte de la pièce jointe
 * @param createdAt date de création
 * @param updatedAt date de dernière modification
 */
public record TicketAttachmentResponse(
    UUID id,
    UUID ticketId,
    UUID uploadedById,
    String uploaderName,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    String attachmentType,
    String caption,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
