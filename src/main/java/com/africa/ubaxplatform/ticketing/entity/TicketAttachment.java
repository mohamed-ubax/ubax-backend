package com.africa.ubaxplatform.ticketing.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.ticketing.codeList.AttachmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Pièce jointe uploadée lors de la déclaration d'un ticket ou en cours de traitement.
 *
 * <p>Entité dédiée pour gérer plusieurs fichiers par ticket, indépendamment des messages. Permet au
 * locataire d'uploader plusieurs photos/vidéos de l'incident dès la déclaration, avant même
 * d'écrire un message.
 *
 * <p><b>Stockage :</b> les fichiers sont dans MinIO bucket {@code ticket-attachments}. Dossier
 * organisé par ticket : {@code ticket-attachments/{ticketId}/{uuid}.jpg}
 *
 * <p><b>Distinction avec {@link TicketMessage#attachmentUrl} :</b>
 *
 * <ul>
 *   <li>{@code TicketAttachment} — pièces jointes liées directement au ticket (photos de l'incident
 *       au moment de la déclaration, PV d'intervention...).
 *   <li>{@code TicketMessage#attachmentUrl} — pièce jointe liée à un message spécifique dans le fil
 *       de discussion.
 * </ul>
 */
@Entity
@Table(
    name = "ticket_attachments",
    schema = "administrative",
    indexes = {
      @Index(name = "idx_ticket_attach_ticket", columnList = "ticket_id"),
      @Index(name = "idx_ticket_attach_uploader", columnList = "uploaded_by")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TicketAttachment extends BaseEntity {

  /** Ticket auquel cette pièce jointe est rattachée. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "ticket_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_tattach_ticket"))
  private Ticket ticket;

  /** Utilisateur ayant uploadé cette pièce jointe (locataire, agent SAV ou prestataire). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "uploaded_by",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_tattach_uploaded_by"))
  private User uploadedBy;

  /**
   * URL du fichier dans MinIO bucket {@code ticket-attachments}. Format : {@code
   * http://minio:9000/ticket-attachments/{ticketId}/{uuid}.jpg}
   */
  @Column(name = "file_url", nullable = false, length = 500)
  private String fileUrl;

  /** Nom original du fichier au moment de l'upload. */
  @Column(name = "file_name", length = 255)
  private String fileName;

  /** Taille du fichier en octets. */
  @Column(name = "file_size")
  private Long fileSize;

  /** Type MIME du fichier. Exemples : {@code image/jpeg}, {@code image/png}, {@code video/mp4} */
  @Column(name = "mime_type", length = 100)
  private String mimeType;

  /**
   * Catégorie de la pièce jointe pour faciliter le tri dans l'interface.
   *
   * <ul>
   *   <li>{@code INCIDENT_PHOTO} — photo de l'incident prise par le locataire.
   *   <li>{@code INCIDENT_VIDEO} — vidéo de l'incident.
   *   <li>{@code INTERVENTION_REPORT} — PV ou rapport d'intervention du technicien.
   *   <li>{@code INVOICE} — facture du prestataire après intervention.
   *   <li>{@code OTHER} — tout autre document.
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "attachment_type", nullable = false, length = 30)
  @Builder.Default
  private AttachmentType attachmentType = AttachmentType.INCIDENT_PHOTO;

  /**
   * Légende ou description courte de la pièce jointe. Saisie optionnellement par l'utilisateur lors
   * de l'upload. Exemple : {@code "Vue de la fuite sous le lavabo"}
   */
  @Column(name = "caption", length = 500)
  private String caption;

  /** {@code true} si la pièce jointe est une image affichable dans l'interface. */
  public boolean isImage() {
    return mimeType != null && mimeType.startsWith("image/");
  }

  /** {@code true} si la pièce jointe est une vidéo. */
  public boolean isVideo() {
    return mimeType != null && mimeType.startsWith("video/");
  }
}
