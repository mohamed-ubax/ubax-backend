package com.africa.ubaxplatform.partner.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.ApplicationDecisionRequest;
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
  private final KeycloakAdminService keycloakAdminService;
  private final EmailService emailService;
  private final MinioService minioService;

  private static final String BUCKET_PARTNER_DOCS = "partner-documents";

  @Value("${ubax.mail.admin-email}")
  private String adminEmail;

  // ── Soumission publique ────────────────────────────────────────

  @Override
  @Transactional
  public PartnerApplicationResponse apply(
      PartnerApplicationRequest request,
      String rccmUrl,
      String dfeUrl,
      String bailUrl,
      String logoUrl) {
    if (applicationRepo.existsByEmailAndStatusNot(request.getEmail(), ApplicationStatus.REJECTED)) {
      throw new ConflictException(ResponseMessageConstants.PARTNER_APPLICATION_ALREADY_EXISTS);
    }

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
        request.getPartnerType().name(),
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
      UUID id, String adminKeycloakId, ApplicationDecisionRequest decision) {

    validateDecisionTransition(decision.getNewStatus());

    PartnerApplication application = findById(id);

    // Commentaire obligatoire pour REJECTED et INCOMPLETE
    if ((decision.getNewStatus() == ApplicationStatus.REJECTED
            || decision.getNewStatus() == ApplicationStatus.INCOMPLETE)
        && (decision.getComment() == null || decision.getComment().isBlank())) {
      throw new BadRequestException(ResponseMessageConstants.PARTNER_APPLICATION_COMMENT_REQUIRED);
    }

    User admin =
        userRepo
            .findByKeycloakId(adminKeycloakId)
            .orElseThrow(() -> new NotFoundException("Administrateur introuvable"));

    ApplicationStatus previousStatus = application.getStatus();
    LocalDateTime now = LocalDateTime.now();

    application.setStatus(decision.getNewStatus());
    application.setReviewedBy(admin);
    application.setReviewedAt(now);
    if (decision.getNewStatus() == ApplicationStatus.REJECTED
        || decision.getNewStatus() == ApplicationStatus.INCOMPLETE) {
      application.setRejectionReason(decision.getComment());
    }

    application = applicationRepo.save(application);

    saveStatusLog(
        application, previousStatus, decision.getNewStatus(), admin, decision.getComment(), now);

    // Création automatique du compte partenaire lors de l'approbation
    if (decision.getNewStatus() == ApplicationStatus.APPROVED) {
      provisionPartnerAccount(application);
    }

    // Email au partenaire selon la décision
    sendDecisionEmail(application, decision.getNewStatus(), decision.getComment());

    log.info(
        "Décision admin : {} → {} pour la demande {}", previousStatus, decision.getNewStatus(), id);

    return toResponse(application, null);
  }

  // ── Provisionnement compte partenaire ─────────────────────────

  private void provisionPartnerAccount(PartnerApplication application) {
    try {
      // 1. Créer le compte Keycloak (username = email, sans mot de passe)
      String keycloakId =
          keycloakAdminService.createPartnerAccount(
              application.getEmail(),
              application.getCompanyName(),
              application.getLegalRepresentative(),
              application.getPhone());

      // 2. Attribuer le rôle PARTNER
      keycloakAdminService.assignRole(keycloakId, UserRole.PARTNER);

      // 3. Persister l'utilisateur en base
      User partnerUser =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(application.getCompanyName())
              .lastName(application.getLegalRepresentative())
              .email(application.getEmail())
              .phone(application.getPhone())
              .roles(new HashSet<>(Set.of(UserRole.PARTNER)))
              .emailVerified(true)
              .country(application.getCountry())
              .city(application.getCity())
              .build();
      userRepo.save(partnerUser);

      // 4. Envoyer le lien "Définir mon mot de passe" via Keycloak
      keycloakAdminService.sendSetPasswordLink(keycloakId);

      log.info(
          "Compte partenaire provisionné avec succès : keycloakId={}, email={}",
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

  @Override
  public String uploadIfPresent(
      MultipartFile file, String docKey, Set<String> allowedTypes, long maxBytes) {
    if (file == null || file.isEmpty()) return null;
    String ct = file.getContentType();
    if (ct == null || !allowedTypes.contains(ct)) {
      throw new BadRequestException(
          "Format non supporté pour '" + docKey + "'. Acceptés : " + allowedTypes);
    }
    if (file.getSize() > maxBytes) {
      throw new BadRequestException(
          "Fichier '" + docKey + "' trop volumineux (max " + (maxBytes / 1024 / 1024) + " Mo)");
    }
    String ext =
        switch (ct) {
          case "application/pdf" -> ".pdf";
          case "image/png" -> ".png";
          case "image/webp" -> ".webp";
          default -> ".jpg";
        };
    String objectName = UUID.randomUUID() + "-" + docKey + ext;
    try {
      return minioService.uploadFile(
          BUCKET_PARTNER_DOCS, objectName, file.getInputStream(), file.getSize(), ct);
    } catch (Exception e) {
      throw new BadRequestException("Erreur upload '" + docKey + "' : " + e.getMessage());
    }
  }
}
