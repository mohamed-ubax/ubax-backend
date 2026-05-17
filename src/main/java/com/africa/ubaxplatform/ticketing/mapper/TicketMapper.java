package com.africa.ubaxplatform.ticketing.mapper;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.technicien.entity.Technicien;
import com.africa.ubaxplatform.ticketing.dto.TicketAttachmentResponse;
import com.africa.ubaxplatform.ticketing.dto.TicketMessageResponse;
import com.africa.ubaxplatform.ticketing.dto.TicketResponse;
import com.africa.ubaxplatform.ticketing.entity.Ticket;
import com.africa.ubaxplatform.ticketing.entity.TicketAttachment;
import com.africa.ubaxplatform.ticketing.entity.TicketMessage;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper statique entre les entités du module {@code ticketing} et leurs DTOs de sortie.
 *
 * <p>Centralise toutes les conversions pour éviter la duplication dans les services.
 *
 * <p>Usage :
 *
 * <pre>{@code
 * TicketResponse           response   = TicketMapper.toResponse(ticket);
 * TicketAttachmentResponse attachment = TicketMapper.toAttachmentResponse(ticketAttachment);
 * TicketMessageResponse    message    = TicketMapper.toMessageResponse(ticketMessage);
 * }</pre>
 */
@Component
public class TicketMapper {

  private TicketMapper() {}

  /**
   * Convertit un {@link Ticket} en {@link TicketResponse}.
   *
   * <p>Les informations du reporter et de l'agent assigné sont extraites des relations lazy.
   *
   * @param t entité à convertir (non null)
   * @return DTO de réponse complet
   */
  public static TicketResponse toResponse(Ticket t) {
    return toResponse(t, null);
  }

  public static TicketResponse toResponse(Ticket t, List<String> attachmentUrls) {
    Contract contract = t.getContract();
    User reporter = t.getReporter();
    User assignedTo = t.getAssignedTo();
    Technicien technicien = t.getTechnicien();
    String techName = technicien != null ? technicien.getFullName() : t.getTechnicianName();
    String techPhone = technicien != null ? technicien.getPhone() : t.getTechnicianPhone();
    return new TicketResponse(
        t.getId(),
        contract != null ? contract.getId() : null,
        reporter != null ? reporter.getId() : null,
        reporter != null ? reporter.getFullName() : null,
        assignedTo != null ? assignedTo.getId() : null,
        assignedTo != null ? assignedTo.getFullName() : null,
        t.getCategory(),
        t.getTitle(),
        t.getDescription(),
        t.getPriority(),
        t.getStatus(),
        technicien != null ? technicien.getId() : null,
        technicien != null ? technicien.getProfession() : null,
        techName,
        techPhone,
        t.getInterventionPrice(),
        t.getInterventionScheduledAt(),
        t.getResolvedAt(),
        t.getClosedAt(),
        t.getRepairCost(),
        t.getCostImputedTo(),
        t.getResolutionNote(),
        t.getRating(),
        t.getRatingComment(),
        t.getCreatedAt(),
        t.getUpdatedAt(),
        attachmentUrls);
  }

  /**
   * Convertit un {@link TicketAttachment} en {@link TicketAttachmentResponse}.
   *
   * @param a entité à convertir (non null)
   * @return DTO de réponse de la pièce jointe
   */
  public static TicketAttachmentResponse toAttachmentResponse(TicketAttachment a) {
    User uploadedBy = a.getUploadedBy();
    return new TicketAttachmentResponse(
        a.getId(),
        a.getTicket() != null ? a.getTicket().getId() : null,
        uploadedBy != null ? uploadedBy.getId() : null,
        uploadedBy != null ? uploadedBy.getFullName() : null,
        a.getFileUrl(),
        a.getFileName(),
        a.getFileSize(),
        a.getMimeType(),
        a.getAttachmentType(),
        a.getCaption(),
        a.getCreatedAt(),
        a.getUpdatedAt());
  }

  /**
   * Convertit un {@link TicketMessage} en {@link TicketMessageResponse}.
   *
   * @param m entité à convertir (non null)
   * @return DTO de réponse du message
   */
  public static TicketMessageResponse toMessageResponse(TicketMessage m) {
    User sender = m.getSender();
    return new TicketMessageResponse(
        m.getId(),
        m.getTicket() != null ? m.getTicket().getId() : null,
        sender != null ? sender.getId() : null,
        sender != null ? sender.getFullName() : null,
        m.getMessage(),
        m.getMessageType(),
        m.getAttachmentUrl(),
        m.getAttachmentName(),
        m.getAttachmentMimeType(),
        m.isRead(),
        m.getCreatedAt(),
        m.getUpdatedAt());
  }
}
