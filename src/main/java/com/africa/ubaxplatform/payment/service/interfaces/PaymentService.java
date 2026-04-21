package com.africa.ubaxplatform.payment.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.dto.PaymentCreateRequest;
import com.africa.ubaxplatform.payment.dto.PaymentResponse;
import com.africa.ubaxplatform.payment.dto.PaymentStatusUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

  PaymentResponse create(String callerKeycloakId, PaymentCreateRequest request)
      throws CustomException;

  PaymentResponse getById(UUID id) throws CustomException;

  Page<PaymentResponse> list(
      String callerKeycloakId,
      PaymentStatus status,
      PaymentType type,
      UUID propertyId,
      UUID contractId,
      UUID tenantId,
      LocalDate from,
      LocalDate to,
      Pageable pageable)
      throws CustomException;

  /** Paiements en retard (échéance dépassée, statut PENDING ou PARTIAL). */
  List<PaymentResponse> listLate(String callerKeycloakId) throws CustomException;

  PaymentResponse updateStatus(UUID id, String callerKeycloakId, PaymentStatusUpdateRequest request)
      throws CustomException;

  void delete(UUID id, String callerKeycloakId) throws CustomException;
}
