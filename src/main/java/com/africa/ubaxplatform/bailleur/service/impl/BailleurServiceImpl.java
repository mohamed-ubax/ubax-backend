package com.africa.ubaxplatform.bailleur.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.africa.ubaxplatform.bailleur.dto.AgencyBailleurResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurAgencyResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplicationResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplyRequest;
import com.africa.ubaxplatform.bailleur.dto.BailleurDecisionRequest;
import com.africa.ubaxplatform.bailleur.entity.BailleurAgencyLink;
import com.africa.ubaxplatform.bailleur.entity.BailleurApplication;
import com.africa.ubaxplatform.bailleur.repository.BailleurAgencyLinkRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurApplicationRepository;
import com.africa.ubaxplatform.bailleur.service.interfaces.BailleurService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import java.time.LocalDateTime;
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
public class BailleurServiceImpl implements BailleurService {

  private final BailleurApplicationRepository applicationRepo;
  private final BailleurAgencyLinkRepository linkRepo;
  private final UserRepository userRepo;
  private final AgencyRepository agencyRepo;
  private final KeycloakAdminService keycloakAdminService;

  // ── Soumission (CLIENT authentifié) ───────────────────────────

  @Override
  @Transactional
  public BailleurApplicationResponse apply(String callerKeycloakId, BailleurApplyRequest request)
      throws CustomException {

    User caller =
        userRepo
            .findByKeycloakId(callerKeycloakId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND),
                        ResponseMessageConstants.USER_NOT_FOUND));

    Agency agency =
        agencyRepo
            .findById(request.getAgencyId())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND),
                        ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND));

    // Email : compte en priorité, body en fallback, erreur si aucun
    String email = caller.getEmail();
    if (email == null || email.isBlank()) {
      if (request.getEmail() == null || request.getEmail().isBlank()) {
        throw new BadRequestException(
            "Votre compte ne possède pas d'adresse email. Veuillez en fournir une dans le champ 'email'.");
      }
      email = request.getEmail();
    }

    BailleurApplication application =
        BailleurApplication.builder()
            .userId(caller.getId())
            .agencyId(request.getAgencyId())
            .firstName(caller.getFirstName())
            .lastName(caller.getLastName())
            .phone(caller.getPhone())
            .email(email)
            .idType(request.getIdType())
            .idNumber(request.getIdNumber())
            .idDocRectoUrl(request.getIdDocRectoUrl())
            .idDocVersoUrl(request.getIdDocVersoUrl())
            .description(request.getDescription())
            .status(BailleurApplicationStatus.PENDING)
            .build();

    application = applicationRepo.save(application);

    log.info(
        "Demande bailleur soumise : applicationId={}, agencyId={}, userId={}",
        application.getId(),
        request.getAgencyId(),
        caller.getId());

    return toResponse(application, agency.getName());
  }

  // ── Décision (approuver / rejeter) ────────────────────────────

  @Override
  @Transactional
  public BailleurApplicationResponse processDecision(
      UUID applicationId, BailleurDecisionRequest request, String reviewerKeycloakId)
      throws CustomException {

    BailleurApplication application =
        applicationRepo
            .findById(applicationId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(
                            ResponseMessageConstants.BAILLEUR_APPLICATION_NOT_FOUND),
                        ResponseMessageConstants.BAILLEUR_APPLICATION_NOT_FOUND));

    if (application.getStatus() != BailleurApplicationStatus.PENDING) {
      throw new CustomException(
          new IllegalArgumentException(
              "Cette demande a déjà été traitée (statut : " + application.getStatus() + ")"),
          ResponseMessageConstants.BAILLEUR_APPLICATION_INVALID_STATUS);
    }

    Agency agency =
        agencyRepo
            .findById(application.getAgencyId())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND),
                        ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND));

    User reviewer =
        userRepo
            .findByKeycloakId(reviewerKeycloakId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND),
                        ResponseMessageConstants.USER_NOT_FOUND));

    if (request.getDecision() == BailleurDecisionRequest.Decision.REJECT) {
      application.setStatus(BailleurApplicationStatus.REJECTED);
      application.setRejectionReason(request.getComment());
      application.setReviewedBy(reviewer);
      application.setReviewedAt(LocalDateTime.now());
      applicationRepo.save(application);
      log.info("Demande bailleur rejetée : applicationId={}", applicationId);
    } else {
      approveApplication(application, agency, reviewer);
    }

    return toResponse(application, agency.getName());
  }

  // ── Lectures ──────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<BailleurApplicationResponse> listByAgency(UUID agencyId, Pageable pageable) {
    return applicationRepo
        .findByAgencyId(agencyId, pageable)
        .map(
            app -> {
              String agencyName = agencyRepo.findById(agencyId).map(Agency::getName).orElse(null);
              return toResponse(app, agencyName);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public BailleurApplicationResponse getById(UUID applicationId) throws CustomException {
    BailleurApplication application =
        applicationRepo
            .findById(applicationId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(
                            ResponseMessageConstants.BAILLEUR_APPLICATION_NOT_FOUND),
                        ResponseMessageConstants.BAILLEUR_APPLICATION_NOT_FOUND));

    String agencyName =
        agencyRepo.findById(application.getAgencyId()).map(Agency::getName).orElse(null);
    return toResponse(application, agencyName);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BailleurApplicationResponse> listAll(Pageable pageable) {
    return applicationRepo
        .findAll(pageable)
        .map(
            app -> {
              String agencyName =
                  agencyRepo.findById(app.getAgencyId()).map(Agency::getName).orElse(null);
              return toResponse(app, agencyName);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BailleurApplicationResponse> getMyApplications(
      String callerKeycloakId, BailleurApplicationStatus status, Pageable pageable)
      throws CustomException {
    User caller =
        userRepo
            .findByKeycloakId(callerKeycloakId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND),
                        ResponseMessageConstants.USER_NOT_FOUND));

    Page<BailleurApplication> page =
        status != null
            ? applicationRepo.findByUserIdAndStatus(caller.getId(), status, pageable)
            : applicationRepo.findByUserId(caller.getId(), pageable);

    return page.map(
        app -> {
          String agencyName =
              agencyRepo.findById(app.getAgencyId()).map(Agency::getName).orElse(null);
          return toResponse(app, agencyName);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public List<BailleurAgencyResponse> getMyAgencies(String callerKeycloakId)
      throws CustomException {
    User user =
        userRepo
            .findByKeycloakId(callerKeycloakId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND),
                        ResponseMessageConstants.USER_NOT_FOUND));
    return linkRepo.findByBailleurUserId(user.getId()).stream()
        .map(
            link -> {
              Agency agency = agencyRepo.findById(link.getAgencyId()).orElse(null);
              return BailleurAgencyResponse.builder()
                  .agencyId(link.getAgencyId())
                  .agencyName(agency != null ? agency.getName() : null)
                  .agencyLogo(agency != null ? agency.getLogoUrl() : null)
                  .agencyPhone(agency != null ? agency.getPhone() : null)
                  .agencyEmail(agency != null ? agency.getEmail() : null)
                  .agencyDescription(agency != null ? agency.getDescription() : null)
                  .linkedAt(link.getJoinedAt())
                  .build();
            })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AgencyBailleurResponse> listAgencyBailleurs(UUID agencyId, Pageable pageable) {
    return linkRepo
        .findByAgencyId(agencyId, pageable)
        .map(
            link ->
                userRepo
                    .findById(link.getBailleurUserId())
                    .map(
                        user ->
                            AgencyBailleurResponse.builder()
                                .id(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .phone(user.getPhone())
                                .email(user.getEmail())
                                .avatarUrl(user.getAvatarUrl())
                                .joinedAt(link.getJoinedAt())
                                .build())
                    .orElse(null));
  }

  // ── Helpers privés ─────────────────────────────────────────────

  private void approveApplication(BailleurApplication application, Agency agency, User reviewer)
      throws CustomException {

    User bailleur =
        userRepo
            .findById(application.getUserId())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND),
                        ResponseMessageConstants.USER_NOT_FOUND));

    if (!bailleur.getRoles().contains(UserRole.OWNER)) {
      keycloakAdminService.assignRole(bailleur.getKeycloakId(), UserRole.OWNER);
      bailleur.getRoles().add(UserRole.OWNER);
      userRepo.save(bailleur);
      log.info(
          "Rôle OWNER ajouté : userId={}, keycloakId={}",
          bailleur.getId(),
          bailleur.getKeycloakId());
    }

    if (!linkRepo.existsByBailleurUserIdAndAgencyId(bailleur.getId(), application.getAgencyId())) {
      BailleurAgencyLink link =
          BailleurAgencyLink.builder()
              .bailleurUserId(bailleur.getId())
              .agencyId(application.getAgencyId())
              .joinedAt(LocalDateTime.now())
              .build();
      linkRepo.save(link);
    }

    application.setStatus(BailleurApplicationStatus.APPROVED);
    application.setReviewedBy(reviewer);
    application.setReviewedAt(LocalDateTime.now());
    applicationRepo.save(application);

    log.info(
        "Demande bailleur approuvée : applicationId={}, bailleurUserId={}",
        application.getId(),
        bailleur.getId());
  }

  private BailleurApplicationResponse toResponse(BailleurApplication app, String agencyName) {
    BailleurApplicationResponse.BailleurApplicationResponseBuilder builder =
        BailleurApplicationResponse.builder()
            .id(app.getId())
            .agencyId(app.getAgencyId())
            .agencyName(agencyName)
            .firstName(app.getFirstName())
            .lastName(app.getLastName())
            .phone(app.getPhone())
            .email(app.getEmail())
            .idType(app.getIdType())
            .idNumber(app.getIdNumber())
            .idDocRectoUrl(app.getIdDocRectoUrl())
            .idDocVersoUrl(app.getIdDocVersoUrl())
            .description(app.getDescription())
            .status(app.getStatus())
            .rejectionReason(app.getRejectionReason())
            .reviewedAt(app.getReviewedAt())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt());

    if (app.getReviewedBy() != null) {
      builder.reviewedByName(app.getReviewedBy().getFullName());
    }

    return builder.build();
  }
}
