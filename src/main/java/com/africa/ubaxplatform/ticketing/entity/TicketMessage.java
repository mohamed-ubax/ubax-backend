package com.africa.ubaxplatform.ticketing.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Message échangé dans le fil de discussion d'un ticket SAV.
 *
 * <p>Permet un chat interne entre le locataire, l'agent SAV
 * et le prestataire technique tout au long du cycle de résolution.</p>
 *
 * <p><b>Pièces jointes :</b> chaque message peut contenir une pièce jointe
 * (photo du problème, facture du prestataire, bon d'intervention...)
 * stockée dans MinIO bucket {@code ticket-attachments}.</p>
 *
 * <p><b>Type de message :</b> distingue les messages visibles par toutes
 * les parties ({@code PUBLIC}) des notes internes réservées à l'équipe SAV
 * ({@code INTERNAL}), invisibles pour le locataire.</p>
 */
@Entity
@Table(
        name = "ticket_messages",
        schema = "administrative",
        indexes = {
                @Index(name = "idx_ticket_msg_ticket",  columnList = "ticket_id"),
                @Index(name = "idx_ticket_msg_sender",  columnList = "sender_id"),
                @Index(name = "idx_ticket_msg_type",    columnList = "message_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TicketMessage extends BaseEntity {

    // =========================================================================
    // RELATIONS
    // =========================================================================

    /**
     * Ticket auquel ce message est rattaché.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tmsg_ticket")
    )
    private Ticket ticket;

    /**
     * Auteur du message (locataire, agent SAV ou prestataire).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tmsg_sender")
    )
    private User sender;

    // =========================================================================
    // CONTENU
    // =========================================================================

    /**
     * Contenu textuel du message.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Type de message déterminant la visibilité.
     *
     * <ul>
     *   <li>{@code PUBLIC} — visible par toutes les parties (locataire, agent, propriétaire).</li>
     *   <li>{@code INTERNAL} — note interne visible uniquement par l'équipe SAV,
     *       invisible pour le locataire.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.PUBLIC;

    // =========================================================================
    // PIÈCE JOINTE — MinIO bucket ticket-attachments
    // =========================================================================

    /**
     * URL de la pièce jointe dans MinIO bucket {@code ticket-attachments}.
     * Peut être une photo du problème, une facture, un bon d'intervention...
     * {@code null} si le message ne contient pas de pièce jointe.
     * Format : {@code http://minio:9000/ticket-attachments/{ticketId}/{uuid}.jpg}
     */
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    /**
     * Nom original du fichier joint.
     * Affiché dans l'interface pour identifier la pièce jointe.
     */
    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    /**
     * Type MIME de la pièce jointe.
     * Exemples : {@code image/jpeg}, {@code application/pdf}
     */
    @Column(name = "attachment_mime_type", length = 100)
    private String attachmentMimeType;

    /**
     * {@code true} si le message a été lu par le destinataire.
     * Pour les messages PUBLIC : lu par le locataire.
     * Pour les messages INTERNAL : lu par l'agent assigné.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    // =========================================================================
    // MÉTHODES UTILITAIRES
    // =========================================================================

    /**
     * {@code true} si ce message contient une pièce jointe.
     */
    public boolean hasAttachment() {
        return attachmentUrl != null;
    }

    /**
     * {@code true} si ce message est une note interne (non visible par le locataire).
     */
    public boolean isInternal() {
        return messageType == MessageType.INTERNAL;
    }

    // =========================================================================
    // ENUM
    // =========================================================================

    public enum MessageType {
        /** Visible par toutes les parties du ticket. */
        PUBLIC,
        /** Note interne visible uniquement par l'équipe SAV. */
        INTERNAL
    }
}