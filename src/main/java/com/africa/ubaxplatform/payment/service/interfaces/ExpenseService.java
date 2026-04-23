package com.africa.ubaxplatform.payment.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.dto.ExpenseCreateRequest;
import com.africa.ubaxplatform.payment.dto.ExpenseResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseService {

  ExpenseResponse create(String callerKeycloakId, ExpenseCreateRequest request)
      throws CustomException;

  ExpenseResponse getById(UUID id, String callerKeycloakId) throws CustomException;

  Page<ExpenseResponse> list(
      String callerKeycloakId,
      ExpenseCategory category,
      UUID propertyId,
      LocalDate from,
      LocalDate to,
      Pageable pageable)
      throws CustomException;

  void delete(UUID id, String callerKeycloakId) throws CustomException;
}
