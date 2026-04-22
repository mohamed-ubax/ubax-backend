package com.africa.ubaxplatform.ticketing.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
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
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {

  TicketResponse create(String keycloakId, CreateTicketRequest request) throws CustomException;

  Page<TicketResponse> list(
      String keycloakId, TicketStatus status, UUID assignedToId, Pageable pageable)
      throws CustomException;

  TicketResponse getById(UUID ticketId) throws CustomException;

  TicketResponse assign(UUID ticketId, AssignTicketRequest request) throws CustomException;

  TicketResponse updateStatus(UUID ticketId, UpdateTicketStatusRequest request, String keycloakId)
      throws CustomException;

  TicketResponse scheduleIntervention(UUID ticketId, ScheduleInterventionRequest request)
      throws CustomException;

  TicketResponse updateRepairCost(UUID ticketId, UpdateRepairCostRequest request)
      throws CustomException;

  TicketMessageResponse addMessage(
      UUID ticketId, AddTicketMessageRequest request, String keycloakId) throws CustomException;

  List<TicketMessageResponse> listMessages(UUID ticketId) throws CustomException;

  List<TicketAttachmentResponse> listAttachments(UUID ticketId) throws CustomException;
}
