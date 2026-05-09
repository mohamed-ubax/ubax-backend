package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.AdminAgencyResponse;
import com.africa.ubaxplatform.auth.dto.AdminHotelResponse;
import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.dto.UpdateSubscriptionRequest;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminPartnerService {

  Page<AdminAgencyResponse> listAgencies(Pageable pageable) throws CustomException;

  Page<AdminHotelResponse> listHotels(Pageable pageable) throws CustomException;

  AdminAgencyResponse suspendAgency(UUID id) throws CustomException;

  AdminAgencyResponse activateAgency(UUID id) throws CustomException;

  AdminHotelResponse suspendHotel(UUID id) throws CustomException;

  AdminHotelResponse activateHotel(UUID id) throws CustomException;

  AdminAgencyResponse updateAgencySubscription(UUID id, UpdateSubscriptionRequest request)
      throws CustomException;

  AdminHotelResponse updateHotelSubscription(UUID id, UpdateSubscriptionRequest request)
      throws CustomException;

  Page<ClientUserResponse> listClients(Pageable pageable);
}
