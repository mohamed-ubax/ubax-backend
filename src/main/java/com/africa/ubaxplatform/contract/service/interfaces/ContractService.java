package com.africa.ubaxplatform.contract.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.dto.ContractResponse;
import com.africa.ubaxplatform.contract.dto.ContractStatsResponse;
import com.africa.ubaxplatform.contract.dto.CreateContractRequest;
import com.africa.ubaxplatform.contract.dto.TerminateContractRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContractService {

  ContractStatsResponse getStats(String keycloakId) throws CustomException;

  ContractResponse create(String keycloakId, CreateContractRequest request) throws CustomException;

  Page<ContractResponse> list(
      String keycloakId, ContractStatus status, String search, Pageable pageable)
      throws CustomException;

  Page<ContractResponse> listAll(ContractStatus status, String search, Pageable pageable);

  ContractResponse getById(String keycloakId, UUID id) throws CustomException;

  ContractResponse update(String keycloakId, UUID id, CreateContractRequest request)
      throws CustomException;

  ContractResponse submit(String keycloakId, UUID id) throws CustomException;

  ContractResponse activate(String keycloakId, UUID id) throws CustomException;

  ContractResponse terminate(String keycloakId, UUID id, TerminateContractRequest request)
      throws CustomException;

  ContractResponse cancel(String keycloakId, UUID id) throws CustomException;

  ContractResponse regeneratePdf(String keycloakId, UUID id) throws CustomException;
}
