package com.africa.ubaxplatform.favorite.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.favorite.dto.FavoriteResponse;
import com.africa.ubaxplatform.property.dto.PropertyResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {

  FavoriteResponse add(String keycloakId, UUID propertyId) throws CustomException;

  void remove(String keycloakId, UUID propertyId) throws CustomException;

  Page<PropertyResponse> listMine(String keycloakId, Pageable pageable);
}
