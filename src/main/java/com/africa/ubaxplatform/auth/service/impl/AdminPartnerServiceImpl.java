package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AdminAgencyResponse;
import com.africa.ubaxplatform.auth.dto.AdminHotelResponse;
import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.dto.UpdateSubscriptionRequest;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.AdminPartnerService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPartnerServiceImpl implements AdminPartnerService {

  private final AgencyRepository agencyRepo;
  private final HotelRepository hotelRepo;
  private final UserRepository userRepo;

  // ── Agences ───────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<AdminAgencyResponse> listAgencies(Pageable pageable) {
    return agencyRepo.findByDeletedAtIsNull(pageable).map(this::toAgencyResponse);
  }

  @Override
  @Transactional
  public AdminAgencyResponse suspendAgency(UUID id) throws CustomException {
    Agency agency = requireAgency(id);
    if (!agency.isActive()) {
      throw new CustomException(
          new ConflictException("Agence déjà suspendue"),
          ResponseMessageConstants.PARTNER_ALREADY_SUSPENDED);
    }
    agency.setActive(false);
    return toAgencyResponse(agencyRepo.save(agency));
  }

  @Override
  @Transactional
  public AdminAgencyResponse activateAgency(UUID id) throws CustomException {
    Agency agency = requireAgency(id);
    if (agency.isActive()) {
      throw new CustomException(
          new ConflictException("Agence déjà active"),
          ResponseMessageConstants.PARTNER_ALREADY_ACTIVE);
    }
    agency.setActive(true);
    return toAgencyResponse(agencyRepo.save(agency));
  }

  @Override
  @Transactional
  public AdminAgencyResponse updateAgencySubscription(UUID id, UpdateSubscriptionRequest request)
      throws CustomException {
    Agency agency = requireAgency(id);
    agency.setSubscriptionPlan(request.subscriptionPlan());
    agency.setSubscriptionExpiresAt(request.subscriptionExpiresAt());
    return toAgencyResponse(agencyRepo.save(agency));
  }

  // ── Hôtels ────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<AdminHotelResponse> listHotels(Pageable pageable) {
    return hotelRepo.findByDeletedAtIsNull(pageable).map(this::toHotelResponse);
  }

  @Override
  @Transactional
  public AdminHotelResponse suspendHotel(UUID id) throws CustomException {
    Hotel hotel = requireHotel(id);
    if (!hotel.isActive()) {
      throw new CustomException(
          new ConflictException("Hôtel déjà suspendu"),
          ResponseMessageConstants.PARTNER_ALREADY_SUSPENDED);
    }
    hotel.setActive(false);
    return toHotelResponse(hotelRepo.save(hotel));
  }

  @Override
  @Transactional
  public AdminHotelResponse activateHotel(UUID id) throws CustomException {
    Hotel hotel = requireHotel(id);
    if (hotel.isActive()) {
      throw new CustomException(
          new ConflictException("Hôtel déjà actif"),
          ResponseMessageConstants.PARTNER_ALREADY_ACTIVE);
    }
    hotel.setActive(true);
    return toHotelResponse(hotelRepo.save(hotel));
  }

  @Override
  @Transactional
  public AdminHotelResponse updateHotelSubscription(UUID id, UpdateSubscriptionRequest request)
      throws CustomException {
    Hotel hotel = requireHotel(id);
    hotel.setSubscriptionPlan(request.subscriptionPlan());
    hotel.setSubscriptionExpiresAt(request.subscriptionExpiresAt());
    return toHotelResponse(hotelRepo.save(hotel));
  }

  // ── Clients ───────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<ClientUserResponse> listClients(Pageable pageable) {
    return userRepo
        .findByRoleAndDeletedAtIsNull(UserRole.CLIENT, pageable)
        .map(
            u ->
                new ClientUserResponse(
                    u.getId(),
                    u.getFirstName(),
                    u.getLastName(),
                    u.getEmail(),
                    u.getPhone(),
                    u.getCity(),
                    u.getAvatarUrl(),
                    u.isEmailVerified(),
                    u.isPhoneVerified(),
                    u.isIdentityVerified(),
                    u.isActive(),
                    u.getLastLoginAt(),
                    u.getCreatedAt()));
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private Agency requireAgency(UUID id) throws CustomException {
    return agencyRepo
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Agence introuvable"),
                    ResponseMessageConstants.PARTNER_NOT_FOUND));
  }

  private Hotel requireHotel(UUID id) throws CustomException {
    return hotelRepo
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Hôtel introuvable"),
                    ResponseMessageConstants.PARTNER_NOT_FOUND));
  }

  private AdminAgencyResponse toAgencyResponse(Agency a) {
    return new AdminAgencyResponse(
        a.getId(),
        a.getName(),
        a.getCity(),
        a.getEmail(),
        a.getPhone(),
        a.getLogoUrl(),
        a.getSubscriptionPlan(),
        a.getSubscriptionExpiresAt(),
        a.isSubscriptionActive(),
        a.isVerified(),
        a.getVerifiedAt(),
        a.isActive(),
        a.getCreatedAt());
  }

  private AdminHotelResponse toHotelResponse(Hotel h) {
    return new AdminHotelResponse(
        h.getId(),
        h.getName(),
        h.getCity(),
        h.getEmail(),
        h.getPhone(),
        h.getLogoUrl(),
        h.getStars(),
        h.getTotalRooms(),
        h.getSubscriptionPlan(),
        h.getSubscriptionExpiresAt(),
        h.isSubscriptionActive(),
        h.isVerified(),
        h.getVerifiedAt(),
        h.isActive(),
        h.getCreatedAt());
  }
}
