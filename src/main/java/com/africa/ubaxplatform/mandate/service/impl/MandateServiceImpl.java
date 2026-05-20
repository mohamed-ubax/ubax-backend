package com.africa.ubaxplatform.mandate.service.impl;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurAgencyLinkRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import com.africa.ubaxplatform.mandate.dto.CreateMandateRequest;
import com.africa.ubaxplatform.mandate.dto.MandateResponse;
import com.africa.ubaxplatform.mandate.dto.TerminateMandateRequest;
import com.africa.ubaxplatform.mandate.entity.ManagementContract;
import com.africa.ubaxplatform.mandate.mapper.MandateMapper;
import com.africa.ubaxplatform.mandate.repository.ManagementContractRepository;
import com.africa.ubaxplatform.mandate.service.interfaces.MandateService;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
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
public class MandateServiceImpl implements MandateService {

  private final ManagementContractRepository mandateRepo;
  private final UserRepository userRepo;
  private final BailleurAgencyLinkRepository bailleurAgencyLinkRepo;

  @Override
  @Transactional
  public MandateResponse create(String keycloakId, CreateMandateRequest req)
      throws CustomException {
    User caller = requireUser(keycloakId);
    Agency agency = requireAgency(caller);
    User owner = requireUserById(req.getOwnerId());

    // Le bailleur doit être approuvé par cette agence
    boolean linked =
        bailleurAgencyLinkRepo.existsByBailleurUserIdAndAgencyId(owner.getId(), agency.getId());
    if (!linked) {
      throw new CustomException(
          new BadRequestException(
              "Ce bailleur n'est pas approuvé par votre agence."
                  + " Approuvez d'abord sa demande d'adhésion avant de créer un mandat."),
          ResponseMessageConstants.MANDATE_ACCESS_DENIED);
    }

    // Un seul mandat ACTIVE ou PENDING_SIGNATURE par couple agence↔bailleur
    boolean exists =
        mandateRepo.existsByAgencyIdAndOwnerIdAndStatusIn(
            agency.getId(),
            owner.getId(),
            List.of(MandateStatus.ACTIVE, MandateStatus.PENDING_SIGNATURE));
    if (exists) {
      throw new CustomException(
          new ConflictException(
              "Un mandat actif ou en attente de signature existe déjà pour ce bailleur."),
          ResponseMessageConstants.MANDATE_CONFLICT);
    }

    ManagementContract mandate =
        ManagementContract.builder()
            .agency(agency)
            .owner(owner)
            .createdBy(caller)
            .status(MandateStatus.DRAFT)
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .commissionRate(req.getCommissionRate())
            .specialClauses(req.getSpecialClauses())
            .terminationConditions(req.getTerminationConditions())
            .build();

    ManagementContract saved = mandateRepo.save(mandate);
    saved.setReferenceNumber(generateReference(saved));
    saved = mandateRepo.save(saved);

    log.info(
        "Mandat créé : id={}, agence={}, bailleur={}",
        saved.getId(),
        agency.getId(),
        owner.getId());
    return MandateMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MandateResponse> list(String keycloakId, MandateStatus status, Pageable pageable)
      throws CustomException {
    Agency agency = requireAgency(requireUser(keycloakId));
    return mandateRepo
        .findByAgencyId(agency.getId(), status, pageable)
        .map(MandateMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public MandateResponse getById(String keycloakId, UUID id) throws CustomException {
    User caller = requireUser(keycloakId);
    ManagementContract mandate = requireMandate(id);
    checkAccess(caller, mandate);
    return MandateMapper.toResponse(mandate);
  }

  @Override
  @Transactional
  public MandateResponse submit(String keycloakId, UUID id) throws CustomException {
    User caller = requireUser(keycloakId);
    ManagementContract mandate = requireMandate(id);
    checkAccess(caller, mandate);
    requireTransition(mandate, MandateStatus.DRAFT, MandateStatus.PENDING_SIGNATURE);
    mandate.setStatus(MandateStatus.PENDING_SIGNATURE);
    log.info("Mandat {} → PENDING_SIGNATURE", id);
    return MandateMapper.toResponse(mandateRepo.save(mandate));
  }

  @Override
  @Transactional
  public MandateResponse activate(String keycloakId, UUID id) throws CustomException {
    ManagementContract mandate = requireMandate(id);
    requireTransition(mandate, MandateStatus.PENDING_SIGNATURE, MandateStatus.ACTIVE);
    mandate.setStatus(MandateStatus.ACTIVE);
    log.info("Mandat {} → ACTIVE", id);
    return MandateMapper.toResponse(mandateRepo.save(mandate));
  }

  @Override
  @Transactional
  public MandateResponse terminate(String keycloakId, UUID id, TerminateMandateRequest req)
      throws CustomException {
    User caller = requireUser(keycloakId);
    ManagementContract mandate = requireMandate(id);
    checkAccess(caller, mandate);
    requireTransition(mandate, MandateStatus.ACTIVE, MandateStatus.TERMINATED);
    mandate.setStatus(MandateStatus.TERMINATED);
    mandate.setTerminatedAt(LocalDateTime.now());
    mandate.setTerminationReason(req.getTerminationReason());
    mandate.setTerminatedBy(caller);
    log.info("Mandat {} → TERMINATED ({})", id, req.getTerminationReason());
    return MandateMapper.toResponse(mandateRepo.save(mandate));
  }

  @Override
  @Transactional
  public MandateResponse cancel(String keycloakId, UUID id) throws CustomException {
    User caller = requireUser(keycloakId);
    ManagementContract mandate = requireMandate(id);
    checkAccess(caller, mandate);
    if (mandate.getStatus() != MandateStatus.DRAFT
        && mandate.getStatus() != MandateStatus.PENDING_SIGNATURE) {
      throw new CustomException(
          new BadRequestException(
              "Seul un mandat DRAFT ou PENDING_SIGNATURE peut être annulé. Statut : "
                  + mandate.getStatus()),
          ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }
    mandate.setStatus(MandateStatus.CANCELLED);
    log.info("Mandat {} → CANCELLED", id);
    return MandateMapper.toResponse(mandateRepo.save(mandate));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private User requireUser(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private User requireUserById(UUID id) throws CustomException {
    return userRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Bailleur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private Agency requireAgency(User caller) throws CustomException {
    if (caller.getAgency() == null) {
      throw new CustomException(
          new UnAuthorizedException("Cet utilisateur n'est pas membre d'une agence"),
          ResponseMessageConstants.MANDATE_ACCESS_DENIED);
    }
    return caller.getAgency();
  }

  private ManagementContract requireMandate(UUID id) throws CustomException {
    return mandateRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Mandat introuvable"),
                    ResponseMessageConstants.MANDATE_NOT_FOUND));
  }

  private void checkAccess(User caller, ManagementContract mandate) throws CustomException {
    if (caller.getAgency() == null
        || !caller.getAgency().getId().equals(mandate.getAgency().getId())) {
      throw new CustomException(
          new UnAuthorizedException("Accès refusé"),
          ResponseMessageConstants.MANDATE_ACCESS_DENIED);
    }
  }

  private void requireTransition(ManagementContract m, MandateStatus from, MandateStatus to)
      throws CustomException {
    if (m.getStatus() != from) {
      throw new CustomException(
          new BadRequestException(
              "Transition invalide : statut actuel "
                  + m.getStatus()
                  + ", attendu "
                  + from
                  + " pour passer à "
                  + to),
          ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }
  }

  private String generateReference(ManagementContract m) {
    String year = String.valueOf(Year.now().getValue());
    String suffix = m.getId().toString().replace("-", "").substring(0, 6).toUpperCase();
    return "MAN-" + year + "-" + suffix;
  }
}
