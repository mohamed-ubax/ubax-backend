package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.mapper.UserMapper;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.ClientListingService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.reservation.repository.ReservationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientListingServiceImpl implements ClientListingService {

  private final UserRepository userRepository;
  private final ReservationRepository reservationRepository;
  private final ContractRepository contractRepository;
  private final AgencyRepository agencyRepository;
  private final HotelRepository hotelRepository;

  @Override
  public Page<ClientUserResponse> listAllClients(Pageable pageable) {
    return userRepository
        .findByRoleAndDeletedAtIsNull(UserRole.CLIENT, pageable)
        .map(UserMapper::toClientResponse);
  }

  @Override
  public Page<ClientUserResponse> listClientsByHotel(UUID hotelId, Pageable pageable)
      throws CustomException {
    hotelRepository
        .findByIdAndDeletedAtIsNull(hotelId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException(ResponseMessageConstants.PARTNER_NOT_FOUND)));
    return reservationRepository
        .findDistinctClientsByHotelId(hotelId, pageable)
        .map(UserMapper::toClientResponse);
  }

  @Override
  public Page<ClientUserResponse> listClientsByAgency(UUID agencyId, Pageable pageable)
      throws CustomException {
    agencyRepository
        .findByIdAndDeletedAtIsNull(agencyId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException(ResponseMessageConstants.PARTNER_NOT_FOUND)));
    return contractRepository
        .findDistinctClientsByAgencyId(agencyId, pageable)
        .map(UserMapper::toClientResponse);
  }

  @Override
  public Page<ClientUserResponse> listClientsByCallerAgency(
      String callerKeycloakId, Pageable pageable) throws CustomException {
    User caller = resolveCaller(callerKeycloakId);
    if (caller.getAgency() == null) {
      throw new CustomException(
          new BadRequestException("Cet utilisateur n'appartient à aucune agence"));
    }
    return contractRepository
        .findDistinctClientsByAgencyId(caller.getAgency().getId(), pageable)
        .map(UserMapper::toClientResponse);
  }

  @Override
  public Page<ClientUserResponse> listClientsByCallerHotel(
      String callerKeycloakId, Pageable pageable) throws CustomException {
    User caller = resolveCaller(callerKeycloakId);
    if (caller.getHotel() == null) {
      throw new CustomException(
          new BadRequestException("Cet utilisateur n'appartient à aucun hôtel"));
    }
    return reservationRepository
        .findDistinctClientsByHotelId(caller.getHotel().getId(), pageable)
        .map(UserMapper::toClientResponse);
  }

  private User resolveCaller(String keycloakId) throws CustomException {
    return userRepository
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new UnAuthorizedException(ResponseMessageConstants.USER_NOT_FOUND)));
  }
}
