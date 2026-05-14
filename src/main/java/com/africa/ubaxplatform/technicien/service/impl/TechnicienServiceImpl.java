package com.africa.ubaxplatform.technicien.service.impl;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.technicien.dto.CreateTechnicienRequest;
import com.africa.ubaxplatform.technicien.dto.TechnicienResponse;
import com.africa.ubaxplatform.technicien.dto.UpdateTechnicienRequest;
import com.africa.ubaxplatform.technicien.entity.Technicien;
import com.africa.ubaxplatform.technicien.repository.TechnicienRepository;
import com.africa.ubaxplatform.technicien.service.interfaces.TechnicienService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicienServiceImpl implements TechnicienService {

  private final TechnicienRepository technicienRepo;
  private final UserRepository userRepo;

  @Override
  @Transactional
  public TechnicienResponse create(String callerKeycloakId, CreateTechnicienRequest request)
      throws CustomException {
    User caller = resolveCaller(callerKeycloakId);
    Agency agency = caller.getAgency();
    Hotel hotel = caller.getHotel();

    if (agency == null && hotel == null) {
      throw new CustomException(
          new BadRequestException("Vous devez appartenir à une agence ou un hôtel"));
    }

    Technicien technicien =
        Technicien.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .email(request.getEmail())
            .avatarUrl(request.getAvatarUrl())
            .profession(request.getProfession())
            .address(request.getAddress())
            .agency(agency)
            .hotel(hotel)
            .build();

    Technicien saved = technicienRepo.save(technicien);
    log.info("Technicien créé : id={}, par keycloakId={}", saved.getId(), callerKeycloakId);
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TechnicienResponse> list(
      String callerKeycloakId, Boolean available, Pageable pageable) throws CustomException {
    User caller = resolveCaller(callerKeycloakId);
    Agency agency = caller.getAgency();
    Hotel hotel = caller.getHotel();

    if (agency != null) {
      if (available != null) {
        return technicienRepo
            .findByAgencyIdAndAvailableAndDeletedAtIsNull(agency.getId(), available, pageable)
            .map(this::toResponse);
      }
      return technicienRepo
          .findByAgencyIdAndDeletedAtIsNull(agency.getId(), pageable)
          .map(this::toResponse);
    }

    if (hotel != null) {
      if (available != null) {
        return technicienRepo
            .findByHotelIdAndAvailableAndDeletedAtIsNull(hotel.getId(), available, pageable)
            .map(this::toResponse);
      }
      return technicienRepo
          .findByHotelIdAndDeletedAtIsNull(hotel.getId(), pageable)
          .map(this::toResponse);
    }

    throw new CustomException(
        new BadRequestException("Vous devez appartenir à une agence ou un hôtel"));
  }

  @Override
  @Transactional(readOnly = true)
  public TechnicienResponse getById(String callerKeycloakId, UUID id) throws CustomException {
    Technicien technicien = findAndCheckAccess(callerKeycloakId, id);
    return toResponse(technicien);
  }

  @Override
  @Transactional
  public TechnicienResponse update(
      String callerKeycloakId, UUID id, UpdateTechnicienRequest request) throws CustomException {
    Technicien technicien = findAndCheckAccess(callerKeycloakId, id);

    if (request.getFirstName() != null) technicien.setFirstName(request.getFirstName());
    if (request.getLastName() != null) technicien.setLastName(request.getLastName());
    if (request.getPhone() != null) technicien.setPhone(request.getPhone());
    if (request.getEmail() != null) technicien.setEmail(request.getEmail());
    if (request.getAvatarUrl() != null) technicien.setAvatarUrl(request.getAvatarUrl());
    if (request.getProfession() != null) technicien.setProfession(request.getProfession());
    if (request.getAddress() != null) technicien.setAddress(request.getAddress());

    return toResponse(technicienRepo.save(technicien));
  }

  @Override
  @Transactional
  public TechnicienResponse toggleAvailability(String callerKeycloakId, UUID id)
      throws CustomException {
    Technicien technicien = findAndCheckAccess(callerKeycloakId, id);
    technicien.setAvailable(!technicien.isAvailable());
    return toResponse(technicienRepo.save(technicien));
  }

  @Override
  @Transactional
  public void delete(String callerKeycloakId, UUID id) throws CustomException {
    Technicien technicien = findAndCheckAccess(callerKeycloakId, id);
    technicien.setDeletedAt(LocalDateTime.now());
    technicienRepo.save(technicien);
    log.info("Technicien supprimé logiquement : id={}", id);
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private Technicien findAndCheckAccess(String callerKeycloakId, UUID id) throws CustomException {
    User caller = resolveCaller(callerKeycloakId);
    Technicien technicien =
        technicienRepo
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.TECHNICIEN_NOT_FOUND)));

    boolean sameAgency =
        caller.getAgency() != null
            && technicien.getAgency() != null
            && caller.getAgency().getId().equals(technicien.getAgency().getId());
    boolean sameHotel =
        caller.getHotel() != null
            && technicien.getHotel() != null
            && caller.getHotel().getId().equals(technicien.getHotel().getId());

    if (!sameAgency && !sameHotel) {
      throw new CustomException(new UnAuthorizedException(ResponseMessageConstants.USER_FORBIDDEN));
    }
    return technicien;
  }

  private User resolveCaller(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new UnAuthorizedException(ResponseMessageConstants.USER_NOT_FOUND)));
  }

  private TechnicienResponse toResponse(Technicien t) {
    Agency agency = t.getAgency();
    Hotel hotel = t.getHotel();
    return new TechnicienResponse(
        t.getId(),
        t.getFirstName(),
        t.getLastName(),
        t.getFullName(),
        t.getPhone(),
        t.getEmail(),
        t.getAvatarUrl(),
        t.getProfession(),
        t.getAddress(),
        t.isAvailable(),
        agency != null ? agency.getId() : null,
        agency != null ? agency.getName() : null,
        hotel != null ? hotel.getId() : null,
        hotel != null ? hotel.getName() : null,
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
