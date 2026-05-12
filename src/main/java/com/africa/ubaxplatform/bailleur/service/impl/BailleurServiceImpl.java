package com.africa.ubaxplatform.bailleur.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.africa.ubaxplatform.bailleur.dto.BailleurAgencyResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplicationResponse;
import com.africa.ubaxplatform.bailleur.dto.BailleurApplyRequest;
import com.africa.ubaxplatform.bailleur.dto.BailleurDecisionRequest;
import com.africa.ubaxplatform.bailleur.dto.BailleurPropertyResponse;
import com.africa.ubaxplatform.bailleur.entity.BailleurAgencyLink;
import com.africa.ubaxplatform.bailleur.entity.BailleurApplication;
import com.africa.ubaxplatform.bailleur.entity.BailleurApplicationProperty;
import com.africa.ubaxplatform.bailleur.repository.BailleurAgencyLinkRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurApplicationPropertyRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurApplicationRepository;
import com.africa.ubaxplatform.bailleur.service.interfaces.BailleurService;
import com.africa.ubaxplatform.common.codelist.repository.LaCodeListRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.util.OtpUtils;
import com.africa.ubaxplatform.notification.service.SmsService;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  private static final String GEO_RADIUS_TYPE = "GEO_CONFLICT_RADIUS_METERS";
  private static final double DEFAULT_GEO_RADIUS = 50.0;

  private final BailleurApplicationRepository applicationRepo;
  private final BailleurApplicationPropertyRepository propertyRepo;
  private final BailleurAgencyLinkRepository linkRepo;
  private final UserRepository userRepo;
  private final AgencyRepository agencyRepo;
  private final PropertyRepository propertyRepository;
  private final LaCodeListRepository codeListRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final SmsService smsService;

  // ── Soumission publique ────────────────────────────────────────

  @Override
  @Transactional
  public BailleurApplicationResponse apply(BailleurApplyRequest request) throws CustomException {

    Agency agency =
        agencyRepo
            .findById(request.getAgencyId())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND),
                        ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND));

    double radius = resolveGeoRadius();
    boolean conflictDetected = false;
    StringBuilder conflictNote = new StringBuilder();

    Optional<User> existingBailleur = userRepo.findByPhone(request.getPhone());

    if (existingBailleur.isPresent()) {
      User bailleur = existingBailleur.get();
      if (propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
          bailleur.getId(), request.getAgencyId())) {
        throw new CustomException(
            new IllegalArgumentException(
                "Un bien de ce bailleur est déjà géré par une autre agence"),
            ResponseMessageConstants.BAILLEUR_APPLICATION_CONFLICT);
      }
    } else {
      for (var prop : request.getProperties()) {
        if (prop.getLatitude() != null && prop.getLongitude() != null) {
          boolean conflict =
              propertyRepository.existsNearLocationForOtherAgency(
                  prop.getLatitude().doubleValue(),
                  prop.getLongitude().doubleValue(),
                  request.getAgencyId().toString(),
                  radius);
          if (conflict) {
            conflictDetected = true;
            if (!conflictNote.isEmpty()) conflictNote.append(" | ");
            conflictNote
                .append("Bien à proximité (")
                .append(prop.getAddress())
                .append(") déjà géré par une autre agence");
          }
        } else {
          conflictDetected = true;
          if (!conflictNote.isEmpty()) conflictNote.append(" | ");
          conflictNote
              .append("Coordonnées manquantes pour \"")
              .append(prop.getAddress())
              .append("\" — vérification manuelle requise");
        }
      }
    }

    BailleurApplication application =
        BailleurApplication.builder()
            .agencyId(request.getAgencyId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .email(request.getEmail())
            .idType(request.getIdType())
            .idNumber(request.getIdNumber())
            .status(BailleurApplicationStatus.PENDING)
            .conflictDetected(conflictDetected)
            .conflictNote(conflictDetected ? conflictNote.toString() : null)
            .build();

    application = applicationRepo.save(application);
    List<BailleurApplicationProperty> savedProps =
        saveProperties(application.getId(), request, existingBailleur.isPresent());

    log.info(
        "Demande bailleur soumise : applicationId={}, agencyId={}, phone={}, conflictDetected={}",
        application.getId(),
        request.getAgencyId(),
        request.getPhone(),
        conflictDetected);

    return toResponse(application, agency.getName(), savedProps);
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

    List<BailleurApplicationProperty> props = propertyRepo.findByApplicationId(applicationId);
    return toResponse(application, agency.getName(), props);
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
              List<BailleurApplicationProperty> props =
                  propertyRepo.findByApplicationId(app.getId());
              return toResponse(app, agencyName, props);
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
    List<BailleurApplicationProperty> props = propertyRepo.findByApplicationId(applicationId);
    return toResponse(application, agencyName, props);
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
              List<BailleurApplicationProperty> props =
                  propertyRepo.findByApplicationId(app.getId());
              return toResponse(app, agencyName, props);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BailleurApplicationResponse> getMyApplications(
      String callerEmail, BailleurApplicationStatus status, Pageable pageable) {
    Page<BailleurApplication> page =
        status != null
            ? applicationRepo.findByEmailAndStatus(callerEmail, status, pageable)
            : applicationRepo.findByEmail(callerEmail, pageable);
    return page.map(
        app -> {
          String agencyName =
              agencyRepo.findById(app.getAgencyId()).map(Agency::getName).orElse(null);
          List<BailleurApplicationProperty> props = propertyRepo.findByApplicationId(app.getId());
          return toResponse(app, agencyName, props);
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
                  .linkedAt(link.getJoinedAt())
                  .build();
            })
        .toList();
  }

  // ── Helpers privés ─────────────────────────────────────────────

  private void approveApplication(BailleurApplication application, Agency agency, User reviewer)
      throws CustomException {

    Optional<User> existingUser = userRepo.findByPhone(application.getPhone());
    User bailleur;

    if (existingUser.isEmpty()) {
      String tempPassword = OtpUtils.generateTempPassword();
      String keycloakId =
          keycloakAdminService.createBailleurAccount(
              application.getFirstName(),
              application.getLastName(),
              application.getPhone(),
              application.getEmail(),
              tempPassword);

      keycloakAdminService.assignRole(keycloakId, UserRole.OWNER);

      bailleur =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(application.getFirstName())
              .lastName(application.getLastName())
              .phone(application.getPhone())
              .email(application.getEmail())
              .roles(new HashSet<>(Set.of(UserRole.OWNER)))
              .phoneVerified(false)
              .emailVerified(false)
              .build();
      bailleur = userRepo.save(bailleur);

      smsService.sendSms(
          application.getPhone(),
          "Bienvenue sur UBAX ! Votre compte bailleur a été approuvé par "
              + agency.getName()
              + ". Connectez-vous sur l'app mobile Ubax avec votre numéro et le mot de passe : "
              + tempPassword);

      log.info("Compte bailleur créé : userId={}, keycloakId={}", bailleur.getId(), keycloakId);
    } else {
      bailleur = existingUser.get();
      log.info("Bailleur existant rattaché à l'agence : userId={}", bailleur.getId());
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

  private List<BailleurApplicationProperty> saveProperties(
      UUID applicationId, BailleurApplyRequest request, boolean bailleurKnown) {
    return request.getProperties().stream()
        .map(
            p -> {
              boolean geoVerified = bailleurKnown || p.getLatitude() != null;
              BailleurApplicationProperty prop =
                  BailleurApplicationProperty.builder()
                      .applicationId(applicationId)
                      .address(p.getAddress())
                      .propertyType(p.getPropertyType())
                      .rooms(p.getRooms())
                      .surface(p.getSurface())
                      .desiredRent(p.getDesiredRent())
                      .description(p.getDescription())
                      .latitude(p.getLatitude())
                      .longitude(p.getLongitude())
                      .geoVerified(geoVerified)
                      .build();
              return propertyRepo.save(prop);
            })
        .toList();
  }

  private double resolveGeoRadius() {
    return codeListRepo.findAllByType(GEO_RADIUS_TYPE).stream()
        .findFirst()
        .map(c -> parseRadius(c.getValue()))
        .orElse(DEFAULT_GEO_RADIUS);
  }

  private double parseRadius(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      log.warn(
          "Valeur de rayon invalide en base : '{}', utilisation du défaut {}m",
          value,
          DEFAULT_GEO_RADIUS);
      return DEFAULT_GEO_RADIUS;
    }
  }

  private BailleurApplicationResponse toResponse(
      BailleurApplication app, String agencyName, List<BailleurApplicationProperty> props) {

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
            .status(app.getStatus())
            .conflictDetected(app.isConflictDetected())
            .conflictNote(app.getConflictNote())
            .rejectionReason(app.getRejectionReason())
            .reviewedAt(app.getReviewedAt())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt())
            .properties(props.stream().map(this::toPropertyResponse).toList());

    if (app.getReviewedBy() != null) {
      builder.reviewedByName(app.getReviewedBy().getFullName());
    }

    return builder.build();
  }

  private BailleurPropertyResponse toPropertyResponse(BailleurApplicationProperty p) {
    return BailleurPropertyResponse.builder()
        .id(p.getId())
        .address(p.getAddress())
        .propertyType(p.getPropertyType())
        .rooms(p.getRooms())
        .surface(p.getSurface())
        .desiredRent(p.getDesiredRent())
        .description(p.getDescription())
        .latitude(p.getLatitude())
        .longitude(p.getLongitude())
        .geoVerified(p.isGeoVerified())
        .build();
  }
}
