package com.africa.ubaxplatform.ticketing.dto;

import com.africa.ubaxplatform.ticketing.entity.TicketMessage.MessageType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant un message dans le fil de discussion d'un ticket SAV.
 *
 * @param id identifiant unique du message
 * @param ticketId identifiant du ticket auquel ce message est rattaché
 * @param senderId identifiant de l'auteur du message
 * @param senderName nom complet de l'auteur du message
 * @param message contenu textuel du message
 * @param messageType type de message (PUBLIC visible par tous, INTERNAL note interne équipe SAV)
 * @param attachmentUrl URL de la pièce jointe dans MinIO, {@code null} si aucune pièce jointe
 * @param attachmentName nom original du fichier joint
 * @param attachmentMimeType type MIME de la pièce jointe
 * @param read {@code true} si le message a été lu par le destinataire
 * @param createdAt date de création
 * @param updatedAt date de dernière modification
 */
public record TicketMessageResponse(
    UUID id,
    UUID ticketId,
    UUID senderId,
    String senderName,
    String message,
    MessageType messageType,
    String attachmentUrl,
    String attachmentName,
    String attachmentMimeType,
    boolean read,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
