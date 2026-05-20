package com.africa.ubaxplatform.mandate.mapper;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.mandate.dto.MandateResponse;
import com.africa.ubaxplatform.mandate.entity.ManagementContract;

public class MandateMapper {

  private MandateMapper() {}

  public static MandateResponse toResponse(ManagementContract m) {
    Agency agency = m.getAgency();
    User owner = m.getOwner();
    User createdBy = m.getCreatedBy();

    return new MandateResponse(
        m.getId(),
        m.getReferenceNumber(),
        agency != null ? agency.getId() : null,
        agency != null ? agency.getName() : null,
        owner != null ? owner.getId() : null,
        owner != null ? owner.getFullName() : null,
        owner != null ? owner.getPhone() : null,
        createdBy != null ? createdBy.getId() : null,
        createdBy != null ? createdBy.getFullName() : null,
        m.getStatus(),
        m.getStartDate(),
        m.getEndDate(),
        m.getCommissionRate(),
        m.getSpecialClauses(),
        m.getTerminationConditions(),
        m.getFileUrl(),
        m.getTerminatedAt(),
        m.getTerminationReason(),
        m.getCreatedAt(),
        m.getUpdatedAt());
  }
}
