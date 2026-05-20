package com.africa.ubaxplatform.mandate.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import com.africa.ubaxplatform.mandate.dto.CreateMandateRequest;
import com.africa.ubaxplatform.mandate.dto.MandateResponse;
import com.africa.ubaxplatform.mandate.dto.TerminateMandateRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MandateService {

  MandateResponse create(String keycloakId, CreateMandateRequest request) throws CustomException;

  Page<MandateResponse> list(String keycloakId, MandateStatus status, Pageable pageable)
      throws CustomException;

  MandateResponse getById(String keycloakId, UUID id) throws CustomException;

  MandateResponse submit(String keycloakId, UUID id) throws CustomException;

  MandateResponse activate(String keycloakId, UUID id) throws CustomException;

  MandateResponse terminate(String keycloakId, UUID id, TerminateMandateRequest request)
      throws CustomException;

  MandateResponse cancel(String keycloakId, UUID id) throws CustomException;
}
