package com.africa.ubaxplatform.visitappointment.mapper;

import com.africa.ubaxplatform.visitappointment.dto.VisitRequestResponse;
import com.africa.ubaxplatform.visitappointment.entity.PropertyVisitRequest;

/** Mapper statique pour convertir les entités PropertyVisitRequest en DTO. */
public class VisitRequestMapper {

  private VisitRequestMapper() {}

  /**
   * Convertit une entité PropertyVisitRequest en DTO de réponse.
   *
   * @param request Entité PropertyVisitRequest
   * @return DTO VisitRequestResponse
   */
  public static VisitRequestResponse toResponse(PropertyVisitRequest request) {
    if (request == null) {
      return null;
    }

    return VisitRequestResponse.builder()
        .id(request.getId())
        .propertyId(request.getProperty().getId())
        .propertyTitle(request.getProperty().getTitle())
        .clientId(request.getClient().getId())
        .clientName(request.getClient().getFirstName() + " " + request.getClient().getLastName())
        .agentId(request.getAgent() != null ? request.getAgent().getId() : null)
        .agentName(
            request.getAgent() != null
                ? request.getAgent().getFirstName() + " " + request.getAgent().getLastName()
                : null)
        .requestedDate(request.getRequestedDate())
        .requestedTimeSlot(request.getRequestedTimeSlot())
        .status(request.getStatus())
        .confirmedDate(request.getConfirmedDate())
        .confirmedTimeSlot(request.getConfirmedTimeSlot())
        .rejectionReason(request.getRejectionReason())
        .clientNotes(request.getClientNotes())
        .createdAt(request.getCreatedAt())
        .updatedAt(request.getUpdatedAt())
        .build();
  }
}
