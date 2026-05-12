package com.africa.ubaxplatform.bailleur.service.interfaces;

import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.africa.ubaxplatform.bailleur.dto.BailleurAgencyResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplicationResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplyRequest;
import com.africa.ubaxplatform.bailleur.dto.BailleurDecisionRequest;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BailleurService {

  BailleurApplicationResponse apply(BailleurApplyRequest request) throws CustomException;

  BailleurApplicationResponse processDecision(
      UUID applicationId, BailleurDecisionRequest request, String reviewerKeycloakId)
      throws CustomException;

  Page<BailleurApplicationResponse> listByAgency(UUID agencyId, Pageable pageable);

  BailleurApplicationResponse getById(UUID applicationId) throws CustomException;

  Page<BailleurApplicationResponse> listAll(Pageable pageable);

  Page<BailleurApplicationResponse> getMyApplications(
      String callerEmail, BailleurApplicationStatus status, Pageable pageable);

  List<BailleurAgencyResponse> getMyAgencies(String callerKeycloakId) throws CustomException;
}
