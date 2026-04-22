package com.africa.ubaxplatform.ticketing.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import com.africa.ubaxplatform.ticketing.dto.AddTicketMessageRequest;
import com.africa.ubaxplatform.ticketing.dto.AssignTicketRequest;
import com.africa.ubaxplatform.ticketing.dto.CreateTicketRequest;
import com.africa.ubaxplatform.ticketing.dto.ScheduleInterventionRequest;
import com.africa.ubaxplatform.ticketing.dto.TicketAttachmentResponse;
import com.africa.ubaxplatform.ticketing.dto.TicketMessageResponse;
import com.africa.ubaxplatform.ticketing.dto.TicketResponse;
import com.africa.ubaxplatform.ticketing.dto.UpdateRepairCostRequest;
import com.africa.ubaxplatform.ticketing.dto.UpdateTicketStatusRequest;
import com.africa.ubaxplatform.ticketing.entity.Ticket;
import com.africa.ubaxplatform.ticketing.entity.TicketMessage;
import com.africa.ubaxplatform.ticketing.entity.TicketMessage.MessageType;
import com.africa.ubaxplatform.ticketing.mapper.TicketMapper;
import com.africa.ubaxplatform.ticketing.repository.TicketAttachmentRepository;
import com.africa.ubaxplatform.ticketing.repository.TicketMessageRepository;
import com.africa.ubaxplatform.ticketing.repository.TicketRepository;
import com.africa.ubaxplatform.ticketing.service.interfaces.TicketService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

  private final TicketRepository ticketRepo;
  private final TicketMessageRepository messageRepo;
  private final TicketAttachmentRepository attachmentRepo;
  private final UserRepository userRepo;
  private final ContractRepository contractRepo;

  @Override
  @Transactional
  public TicketResponse create(String keycloakId, CreateTicketRequest request)
      throws CustomException {
    User reporter =
        userRepo
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));

    Contract contract =
        contractRepo
            .findById(request.getContractId())
            .orElseThrow(
                () ->
                    new NotFoundException(ResponseMessageConstants.CONTRACT_GET_FAILURE_NOT_FOUND));

    String priority =
        request.getPriority() != null
            ? request.getPriority()
            : Constants.CodeList.TicketPriority.NORMAL;

    Ticket ticket =
        Ticket.builder()
            .contract(contract)
            .reporter(reporter)
            .category(request.getCategory())
            .title(request.getTitle())
            .description(request.getDescription())
            .priority(priority)
            .build();

    Ticket saved = ticketRepo.save(ticket);
    log.info(
        "Ticket créé : id={}, contractId={}, reporter={}",
        saved.getId(),
        contract.getId(),
        keycloakId);
    return TicketMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TicketResponse> list(
      String keycloakId, TicketStatus status, UUID assignedToId, Pageable pageable)
      throws CustomException {
    User user =
        userRepo
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));

    if (user.getAgency() == null) {
      throw new BadRequestException("Aucune agence associée à ce compte");
    }

    UUID agencyId = user.getAgency().getId();

    if (status != null && assignedToId != null) {
      return ticketRepo
          .findByAgencyIdAndStatus(agencyId, status, pageable)
          .map(TicketMapper::toResponse);
    }
    if (status != null) {
      return ticketRepo
          .findByAgencyIdAndStatus(agencyId, status, pageable)
          .map(TicketMapper::toResponse);
    }
    if (assignedToId != null) {
      return ticketRepo
          .findByAgencyIdAndAssignedTo(agencyId, assignedToId, pageable)
          .map(TicketMapper::toResponse);
    }
    return ticketRepo.findByAgencyId(agencyId, pageable).map(TicketMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public TicketResponse getById(UUID ticketId) throws CustomException {
    Ticket ticket =
        ticketRepo
            .findById(ticketId)
            .orElseThrow(
                () -> new NotFoundException(ResponseMessageConstants.TICKET_GET_FAILURE_NOT_FOUND));
    return TicketMapper.toResponse(ticket);
  }

  @Override
  @Transactional
  public TicketResponse assign(UUID ticketId, AssignTicketRequest request) throws CustomException {
    Ticket ticket = findTicket(ticketId);
    User agent =
        userRepo
            .findById(request.getAssignedToId())
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));
    ticket.setAssignedTo(agent);
    log.info("Ticket {} assigné à userId={}", ticketId, agent.getId());
    return TicketMapper.toResponse(ticketRepo.save(ticket));
  }

  @Override
  @Transactional
  public TicketResponse updateStatus(
      UUID ticketId, UpdateTicketStatusRequest request, String keycloakId) throws CustomException {
    Ticket ticket = findTicket(ticketId);
    TicketStatus newStatus = TicketStatus.valueOf(request.getStatus());
    validateTransition(ticket.getStatus(), newStatus);

    if ((newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED)
        && (request.getResolutionNote() == null || request.getResolutionNote().isBlank())) {
      throw new BadRequestException(
          "La note de résolution est obligatoire pour RESOLVED et CLOSED");
    }

    ticket.setStatus(newStatus);
    if (request.getResolutionNote() != null) {
      ticket.setResolutionNote(request.getResolutionNote());
    }
    if (newStatus == TicketStatus.RESOLVED) {
      ticket.setResolvedAt(LocalDateTime.now());
    }
    if (newStatus == TicketStatus.CLOSED) {
      ticket.setClosedAt(LocalDateTime.now());
    }
    log.info("Ticket {} → statut {} (par {})", ticketId, newStatus, keycloakId);
    return TicketMapper.toResponse(ticketRepo.save(ticket));
  }

  @Override
  @Transactional
  public TicketResponse scheduleIntervention(UUID ticketId, ScheduleInterventionRequest request)
      throws CustomException {
    Ticket ticket = findTicket(ticketId);
    validateTransition(ticket.getStatus(), TicketStatus.TECHNICIAN_SENT);

    ticket.setStatus(TicketStatus.TECHNICIAN_SENT);
    ticket.setTechnicianName(request.getTechnicianName());
    ticket.setTechnicianPhone(request.getTechnicianPhone());
    ticket.setInterventionScheduledAt(request.getInterventionScheduledAt());
    log.info(
        "Intervention planifiée pour ticket {} : technicien={}",
        ticketId,
        request.getTechnicianName());
    return TicketMapper.toResponse(ticketRepo.save(ticket));
  }

  @Override
  @Transactional
  public TicketResponse updateRepairCost(UUID ticketId, UpdateRepairCostRequest request)
      throws CustomException {
    Ticket ticket = findTicket(ticketId);
    ticket.setRepairCost(request.getRepairCost());
    ticket.setCostImputedTo(request.getCostImputedTo());
    if (request.getResolutionNote() != null) {
      ticket.setResolutionNote(request.getResolutionNote());
    }
    log.info("Coût de réparation saisi pour ticket {} : {} XOF", ticketId, request.getRepairCost());
    return TicketMapper.toResponse(ticketRepo.save(ticket));
  }

  @Override
  @Transactional
  public TicketMessageResponse addMessage(
      UUID ticketId, AddTicketMessageRequest request, String keycloakId) throws CustomException {
    Ticket ticket = findTicket(ticketId);
    User sender =
        userRepo
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));

    MessageType type =
        request.getMessageType() != null
            ? MessageType.valueOf(request.getMessageType())
            : MessageType.PUBLIC;

    TicketMessage msg =
        TicketMessage.builder()
            .ticket(ticket)
            .sender(sender)
            .message(request.getMessage())
            .messageType(type)
            .build();

    return TicketMapper.toMessageResponse(messageRepo.save(msg));
  }

  @Override
  @Transactional(readOnly = true)
  public List<TicketMessageResponse> listMessages(UUID ticketId) throws CustomException {
    if (!ticketRepo.existsById(ticketId)) {
      throw new NotFoundException(ResponseMessageConstants.TICKET_GET_FAILURE_NOT_FOUND);
    }
    return messageRepo.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
        .map(TicketMapper::toMessageResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TicketAttachmentResponse> listAttachments(UUID ticketId) throws CustomException {
    if (!ticketRepo.existsById(ticketId)) {
      throw new NotFoundException(ResponseMessageConstants.TICKET_GET_FAILURE_NOT_FOUND);
    }
    return attachmentRepo.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
        .map(TicketMapper::toAttachmentResponse)
        .toList();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Ticket findTicket(UUID ticketId) {
    return ticketRepo
        .findById(ticketId)
        .orElseThrow(
            () -> new NotFoundException(ResponseMessageConstants.TICKET_GET_FAILURE_NOT_FOUND));
  }

  private void validateTransition(TicketStatus current, TicketStatus next) {
    boolean valid =
        switch (current) {
          case OPEN -> next == TicketStatus.IN_ANALYSIS || next == TicketStatus.CANCELLED;
          case IN_ANALYSIS ->
              next == TicketStatus.TECHNICIAN_SENT
                  || next == TicketStatus.RESOLVED
                  || next == TicketStatus.CANCELLED;
          case TECHNICIAN_SENT -> next == TicketStatus.RESOLVED || next == TicketStatus.CANCELLED;
          case RESOLVED -> next == TicketStatus.CLOSED;
          case CLOSED, CANCELLED -> false;
        };

    if (!valid) {
      throw new BadRequestException(
          "Transition invalide : "
              + current
              + " → "
              + next
              + ". Transitions autorisées depuis "
              + current
              + " : "
              + allowedFrom(current));
    }
  }

  private String allowedFrom(TicketStatus status) {
    return switch (status) {
      case OPEN -> "IN_ANALYSIS, CANCELLED";
      case IN_ANALYSIS -> "TECHNICIAN_SENT, RESOLVED, CANCELLED";
      case TECHNICIAN_SENT -> "RESOLVED, CANCELLED";
      case RESOLVED -> "CLOSED";
      case CLOSED, CANCELLED -> "aucune (état terminal)";
    };
  }
}
