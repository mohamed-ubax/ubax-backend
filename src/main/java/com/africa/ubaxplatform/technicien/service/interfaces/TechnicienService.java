package com.africa.ubaxplatform.technicien.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.technicien.dto.CreateTechnicienRequest;
import com.africa.ubaxplatform.technicien.dto.TechnicienResponse;
import com.africa.ubaxplatform.technicien.dto.UpdateTechnicienRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TechnicienService {

  TechnicienResponse create(String callerKeycloakId, CreateTechnicienRequest request)
      throws CustomException;

  Page<TechnicienResponse> list(String callerKeycloakId, Boolean available, Pageable pageable)
      throws CustomException;

  TechnicienResponse getById(String callerKeycloakId, UUID id) throws CustomException;

  TechnicienResponse update(String callerKeycloakId, UUID id, UpdateTechnicienRequest request)
      throws CustomException;

  TechnicienResponse toggleAvailability(String callerKeycloakId, UUID id) throws CustomException;

  void delete(String callerKeycloakId, UUID id) throws CustomException;
}
