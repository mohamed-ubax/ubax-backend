package com.africa.ubaxplatform.contract.mapper;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.contract.dto.ContractResponse;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.tenant.entity.Tenant;

public class ContractMapper {

  private ContractMapper() {}

  public static ContractResponse toResponse(Contract c) {
    Property property = c.getProperty();
    User owner = c.getOwner();
    Tenant tenant = c.getTenant();
    User createdBy = c.getCreatedBy();

    return new ContractResponse(
        c.getId(),
        c.getReferenceNumber(),
        property != null ? property.getId() : null,
        property != null ? property.getTitle() : null,
        property != null ? property.getCity() : null,
        owner != null ? owner.getId() : null,
        owner != null ? owner.getFullName() : null,
        tenant != null ? tenant.getId() : null,
        tenant != null && tenant.getUser() != null ? tenant.getUser().getFullName() : null,
        createdBy != null ? createdBy.getId() : null,
        createdBy != null ? createdBy.getFullName() : null,
        c.getContractType(),
        c.getStatus(),
        c.getStartDate(),
        c.getEndDate(),
        c.getMonthlyRent(),
        c.getMonthlyCharges(),
        c.getTotalMonthlyAmount(),
        c.getDepositAmount(),
        c.isDepositReturned(),
        c.getSalePrice(),
        c.getReservationDeposit(),
        c.getReservationDurationDays(),
        c.getAgencyCommissionRate(),
        c.getPaymentDay(),
        c.getSpecialClauses(),
        c.getTerminationConditions(),
        c.getFileUrl(),
        c.getSignedFileUrl(),
        c.getTerminatedAt(),
        c.getTerminationReason(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
