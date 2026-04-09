package com.africa.ubaxplatform.partner.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.ApplicationStatusLogResponse;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import com.africa.ubaxplatform.partner.entity.ApplicationStatusLog;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import com.africa.ubaxplatform.partner.repository.ApplicationStatusLogRepository;
import com.africa.ubaxplatform.partner.repository.PartnerApplicationRepository;
import com.africa.ubaxplatform.partner.service.interfaces.PartnerApplicationService;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implémentation du service de gestion des demandes d'adhésion partenaire.
 *
 * <p>Orchestration du cycle de vie : soumission publique → examen admin → décision (approbation,
 * rejet, dossier incomplet). Chaque transition est auditée dans {@link ApplicationStatusLog} et
 * déclenche un email transactionnel au partenaire.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerApplicationServiceImpl implements PartnerApplicationService {

  private final PartnerApplicationRepository applicationRepo;
  private final ApplicationStatusLogRepository statusLogRepo;
  private final UserRepository userRepo;
  private final AgencyRepository agencyRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final EmailService emailService;
  private final MinioService minioService;

  private static final Set<String> ALLOWED_DOC_TYPES =
      Set.of("application/pdf", "image/jpeg", "image/png");
  private static final Set<String> ALLOWED_LOGO_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_DOC_BYTES = 10L * 1024 * 1024; // 10 Mo
  private static final long MAX_LOGO_BYTES = 5L * 1024 * 1024; //  5 Mo

  @Value("${ubax.mail.admin-email}")
  private String adminEmail;

  // ── Soumission publique ────────────────────────────────────────

  @Override
  @Transactional
  public PartnerApplicationResponse apply(
      PartnerApplicationRequest request,
      MultipartFile rccm,
      MultipartFile dfe,
      MultipartFile bail,
      MultipartFile logo) {
    if (applicationRepo.existsByEmailAndStatusNot(request.getEmail(), ApplicationStatus.REJECTED)) {
      throw new ConflictException(ResponseMessageConstants.PARTNER_APPLICATION_ALREADY_EXISTS);
    }

    // 1. Créer la structure de répertoires MinIO
    String slug = minioService.initPartnerDirectory(request.getCompanyName());

    // 2. Uploader les documents dans les bons sous-répertoires
    String rccmUrl = uploadLegal(slug, rccm, "rccm");
    String dfeUrl = uploadLegal(slug, dfe, "dfe");
    String bailUrl = uploadLegal(slug, bail, "bail");
    String logoUrl = uploadLogo(slug, logo);

    LocalDateTime now = LocalDateTime.now();

    PartnerApplication application =
        PartnerApplication.builder()
            .partnerType(request.getPartnerType())
            .companyName(request.getCompanyName())
            .legalRepresentative(request.getLegalRepresentative())
            .phone(request.getPhone())
            .email(request.getEmail())
            .country(request.getCountry())
            .city(request.getCity())
            .postalAddress(request.getPostalAddress())
            .zone(request.getZone())
            .description(request.getDescription())
            .legalStatus(request.getLegalStatus())
            .registrationNumber(request.getRegistrationNumber())
            .storageSlug(slug)
            .rccmUrl(rccmUrl)
            .dfeUrl(dfeUrl)
            .bailUrl(bailUrl)
            .logoUrl(logoUrl)
            .status(ApplicationStatus.PENDING)
            .submittedAt(now)
            .build();

    application = applicationRepo.save(application);

    // Audit : première entrée du journal
    saveStatusLog(application, null, ApplicationStatus.PENDING, null, null, now);

    // Notifications
    emailService.sendPartnerApplicationAcknowledge(
        request.getEmail(), request.getCompanyName(), application.getId().toString());
    emailService.sendPartnerApplicationAdminNotif(
        adminEmail,
        request.getCompanyName(),
        request.getPartnerType(),
        request.getEmail(),
        application.getId().toString());

    log.info(
        "Nouvelle demande d'adhésion partenaire soumise : {} ({})",
        request.getCompanyName(),
        application.getId());

    return toResponse(application, null);
  }

  // ── Consultation admin ─────────────────────────────────────────

  @Override
  public Page<PartnerApplicationResponse> listApplications(
      ApplicationStatus status, Pageable pageable) {
    Page<PartnerApplication> page =
        status != null
            ? applicationRepo.findByStatus(status, pageable)
            : applicationRepo.findAll(pageable);
    return page.map(app -> toResponse(app, null));
  }

  @Override
  public PartnerApplicationResponse getApplication(UUID id) {
    PartnerApplication application = findById(id);
    List<ApplicationStatusLog> logs = statusLogRepo.findByApplicationIdOrderByChangedAtAsc(id);
    return toResponse(application, logs);
  }

  // ── Décision admin ─────────────────────────────────────────────

  @Override
  @Transactional
  public PartnerApplicationResponse decide(
      UUID id, String adminKeycloakId, ApplicationStatus newStatus, String comment) {

    validateDecisionTransition(newStatus);

    PartnerApplication application = findById(id);

    // Commentaire obligatoire pour REJECTED et INCOMPLETE
    if ((newStatus == ApplicationStatus.REJECTED || newStatus == ApplicationStatus.INCOMPLETE)
        && (comment == null || comment.isBlank())) {
      throw new BadRequestException(ResponseMessageConstants.PARTNER_APPLICATION_COMMENT_REQUIRED);
    }

    User admin =
        userRepo
            .findByKeycloakId(adminKeycloakId)
            .orElseThrow(() -> new NotFoundException("Administrateur introuvable"));

    ApplicationStatus previousStatus = application.getStatus();
    LocalDateTime now = LocalDateTime.now();

    application.setStatus(newStatus);
    application.setReviewedBy(admin);
    application.setReviewedAt(now);
    if (newStatus == ApplicationStatus.REJECTED || newStatus == ApplicationStatus.INCOMPLETE) {
      application.setRejectionReason(comment);
    }

    application = applicationRepo.save(application);

    saveStatusLog(application, previousStatus, newStatus, admin, comment, now);

    // Création automatique du compte partenaire lors de l'approbation
    if (newStatus == ApplicationStatus.APPROVED) {
      provisionPartnerAccount(application);
    }

    // Email au partenaire selon la décision
    sendDecisionEmail(application, newStatus, comment);

    log.info("Décision admin : {} → {} pour la demande {}", previousStatus, newStatus, id);

    return toResponse(application, null);
  }

  // ── Provisionnement compte partenaire ─────────────────────────

  private void provisionPartnerAccount(PartnerApplication application) {
    try {
      boolean isAgency =
          Constants.CodeList.PartnerType.AGENCE_IMMOBILIERE.equals(application.getPartnerType());

      UserRole assignedRole = isAgency ? UserRole.AGENCY : UserRole.PARTNER;

      // 1. Créer le compte Keycloak (username = email, sans mot de passe)
      String keycloakId =
          keycloakAdminService.createPartnerAccount(
              application.getEmail(),
              application.getCompanyName(),
              application.getLegalRepresentative(),
              application.getPhone());

      // 2. Attribuer le rôle approprié
      keycloakAdminService.assignRole(keycloakId, assignedRole);

      // 3a. Pour une agence immobilière : créer l'entité Agency
      Agency agency = null;
      if (isAgency) {
        agency =
            Agency.builder()
                .name(application.getCompanyName())
                .registrationNumber(application.getRegistrationNumber())
                .logoUrl(application.getLogoUrl())
                .address(application.getPostalAddress())
                .city(application.getCity())
                .country(application.getCountry())
                .phone(application.getPhone())
                .email(application.getEmail())
                .build();
        agency = agencyRepo.save(agency);
      }

      // 3b. Persister l'utilisateur en base
      User.UserBuilder<?, ?> userBuilder =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(application.getCompanyName())
              .lastName(application.getLegalRepresentative())
              .email(application.getEmail())
              .phone(application.getPhone())
              .roles(new HashSet<>(Set.of(assignedRole)))
              .emailVerified(true)
              .country(application.getCountry())
              .city(application.getCity());

      if (agency != null) {
        userBuilder.agency(agency);
      }

      userRepo.save(userBuilder.build());

      // 4. Envoyer le lien "Définir mon mot de passe" via Keycloak
      keycloakAdminService.sendSetPasswordLink(keycloakId);

      log.info(
          "Compte {} provisionné avec succès : keycloakId={}, email={}",
          assignedRole,
          keycloakId,
          application.getEmail());

    } catch (CustomException e) {
      // Ne pas faire échouer la décision si le provisionnement Keycloak échoue.
      // L'admin est alerté via les logs ; le compte peut être créé manuellement.
      log.error(
          "[⚠️ PROVISIONNEMENT] Échec création compte pour la demande {} : {}",
          application.getId(),
          e.getMessage());
    }
  }

  // ── Méthodes privées ───────────────────────────────────────────

  private PartnerApplication findById(UUID id) {
    return applicationRepo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Demande partenaire introuvable : " + id));
  }

  private void validateDecisionTransition(ApplicationStatus newStatus) {
    if (newStatus == ApplicationStatus.PENDING) {
      throw new BadRequestException(
          ResponseMessageConstants.PARTNER_APPLICATION_INVALID_TRANSITION);
    }
  }

  private void saveStatusLog(
      PartnerApplication application,
      ApplicationStatus previousStatus,
      ApplicationStatus newStatus,
      User changedBy,
      String comment,
      LocalDateTime changedAt) {

    ApplicationStatusLog log =
        ApplicationStatusLog.builder()
            .application(application)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .changedBy(changedBy)
            .comment(comment)
            .changedAt(changedAt)
            .build();
    statusLogRepo.save(log);
  }

  private void sendDecisionEmail(
      PartnerApplication application, ApplicationStatus status, String comment) {
    switch (status) {
      case APPROVED ->
          emailService.sendPartnerApplicationApproved(
              application.getEmail(), application.getCompanyName(), application.getEmail());
      case REJECTED ->
          emailService.sendPartnerApplicationRejected(
              application.getEmail(), application.getCompanyName(), comment);
      case INCOMPLETE ->
          emailService.sendPartnerApplicationIncomplete(
              application.getEmail(), application.getCompanyName(), comment);
      case UNDER_REVIEW ->
          emailService.sendPartnerApplicationUnderReview(
              application.getEmail(), application.getCompanyName());
      default -> {
        // PENDING géré à la soumission
      }
    }
  }

  // ── Mapping entity → DTO ───────────────────────────────────────

  private PartnerApplicationResponse toResponse(
      PartnerApplication app, List<ApplicationStatusLog> logs) {

    PartnerApplicationResponse.PartnerApplicationResponseBuilder builder =
        PartnerApplicationResponse.builder()
            .id(app.getId())
            .partnerType(app.getPartnerType())
            .companyName(app.getCompanyName())
            .legalRepresentative(app.getLegalRepresentative())
            .phone(app.getPhone())
            .email(app.getEmail())
            .country(app.getCountry())
            .city(app.getCity())
            .postalAddress(app.getPostalAddress())
            .zone(app.getZone())
            .description(app.getDescription())
            .legalStatus(app.getLegalStatus())
            .registrationNumber(app.getRegistrationNumber())
            .rccmUrl(app.getRccmUrl())
            .dfeUrl(app.getDfeUrl())
            .bailUrl(app.getBailUrl())
            .logoUrl(app.getLogoUrl())
            .status(app.getStatus())
            .submittedAt(app.getSubmittedAt())
            .reviewedAt(app.getReviewedAt())
            .rejectionReason(app.getRejectionReason())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt());

    if (app.getReviewedBy() != null) {
      builder.reviewedByName(
          app.getReviewedBy().getFirstName() + " " + app.getReviewedBy().getLastName());
    }

    if (logs != null) {
      builder.statusHistory(
          logs.stream()
              .map(
                  l ->
                      ApplicationStatusLogResponse.builder()
                          .id(l.getId())
                          .previousStatus(l.getPreviousStatus())
                          .newStatus(l.getNewStatus())
                          .changedByName(
                              l.getChangedBy() != null
                                  ? l.getChangedBy().getFirstName()
                                      + " "
                                      + l.getChangedBy().getLastName()
                                  : "Système")
                          .comment(l.getComment())
                          .changedAt(l.getChangedAt())
                          .build())
              .toList());
    }

    return builder.build();
  }

  // ── Helpers upload partner ──────────────────────────────────────────

  private String uploadLegal(String slug, MultipartFile file, String docKey) {
    if (file == null || file.isEmpty()) return null;
    validateFile(file, docKey, ALLOWED_DOC_TYPES, MAX_DOC_BYTES);
    try {
      return minioService.uploadPartnerLegalDoc(
          slug, docKey, file.getInputStream(), file.getSize(), file.getContentType());
    } catch (Exception e) {
      throw new BadRequestException("Erreur upload '" + docKey + "' : " + e.getMessage());
    }
  }

  private String uploadLogo(String slug, MultipartFile file) {
    if (file == null || file.isEmpty()) return null;
    validateFile(file, "logo", ALLOWED_LOGO_TYPES, MAX_LOGO_BYTES);
    try {
      return minioService.uploadPartnerLogo(
          slug, file.getInputStream(), file.getSize(), file.getContentType());
    } catch (Exception e) {
      throw new BadRequestException("Erreur upload 'logo' : " + e.getMessage());
    }
  }

  private void validateFile(
      MultipartFile file, String docKey, Set<String> allowedTypes, long maxBytes) {
    String ct = file.getContentType();
    if (ct == null || !allowedTypes.contains(ct)) {
      throw new BadRequestException(
          "Format non supporté pour '" + docKey + "'. Acceptés : " + allowedTypes);
    }
    if (file.getSize() > maxBytes) {
      throw new BadRequestException(
          "Fichier '" + docKey + "' trop volumineux (max " + (maxBytes / 1024 / 1024) + " Mo)");
    }
  }
}
