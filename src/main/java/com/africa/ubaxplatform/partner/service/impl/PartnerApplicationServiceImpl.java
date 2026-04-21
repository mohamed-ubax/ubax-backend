package com.africa.ubaxplatform.partner.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.mapper.AgencyMapper;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
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
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import com.africa.ubaxplatform.partner.entity.ApplicationStatusLog;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import com.africa.ubaxplatform.partner.mapper.PartnerApplicationMapper;
import com.africa.ubaxplatform.partner.repository.ApplicationStatusLogRepository;
import com.africa.ubaxplatform.partner.repository.PartnerApplicationRepository;
import com.africa.ubaxplatform.partner.service.interfaces.PartnerApplicationService;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import java.time.LocalDateTime;
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
  private final HotelRepository hotelRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final EmailService emailService;
  private final MinioService minioService;
  private final PartnerApplicationMapper mapper;
  private final AgencyMapper agencyMapper;

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
        mapper.toEntity(request, slug, rccmUrl, dfeUrl, bailUrl, logoUrl, now);

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

    return mapper.toResponse(application, null);
  }

  // ── Consultation admin ─────────────────────────────────────────

  @Override
  public Page<PartnerApplicationResponse> listApplications(
      ApplicationStatus status, Pageable pageable) {
    Page<PartnerApplication> page =
        status != null
            ? applicationRepo.findByStatus(status, pageable)
            : applicationRepo.findAll(pageable);
    return page.map(app -> mapper.toResponse(app, null));
  }

  @Override
  public PartnerApplicationResponse getApplication(UUID id) {
    PartnerApplication application = findById(id);
    List<ApplicationStatusLog> logs = statusLogRepo.findByApplicationIdOrderByChangedAtAsc(id);
    return mapper.toResponse(application, logs);
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

    return mapper.toResponse(application, null);
  }

  // ── Provisionnement compte partenaire ─────────────────────────

  private void provisionPartnerAccount(PartnerApplication application) {
    try {
      boolean isAgency =
          Constants.CodeList.PartnerType.AGENCE_IMMOBILIERE.equals(application.getPartnerType());

      UserRole assignedRole = UserRole.PARTNER;

      // 1. Créer le compte Keycloak (username = email, sans mot de passe)
      String keycloakId =
          keycloakAdminService.createPartnerAccount(
              application.getEmail(),
              application.getCompanyName(),
              application.getLegalRepresentative(),
              application.getPhone());

      // 2. Attribuer le rôle Keycloak approprié
      keycloakAdminService.assignRole(keycloakId, assignedRole);

      // 3. Créer l'entité de structure (Agency ou Hotel) et persister le User
      Agency agency = null;
      Hotel hotel = null;

      if (isAgency) {
        agency = agencyRepo.save(agencyMapper.toAgency(application));
      } else {
        hotel = hotelRepo.save(agencyMapper.toHotel(application));
      }

      // partnerRole initial (DIRECTEUR_AGENCE ou GERANT_HOTEL) est assigné dans le mapper
      userRepo.save(
          agencyMapper.toPartnerUser(application, keycloakId, assignedRole, agency, hotel));

      // 4. Envoyer le lien "Définir mon mot de passe" via Keycloak
      keycloakAdminService.sendSetPasswordLink(keycloakId);

      log.info(
          "Compte {} provisionné avec succès : keycloakId={}, email={}, partnerType={}",
          assignedRole,
          keycloakId,
          application.getEmail(),
          application.getPartnerType());

    } catch (CustomException e) {
      // Ne pas faire échouer la décision si le provisionnement Keycloak échoue.
      // L'admin est alerté via les logs ; le compte peut être créé manuellement.
      log.error(
          "[PROVISIONNEMENT] Echec creation compte pour la demande {} : {}",
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
    statusLogRepo.save(
        mapper.toStatusLog(application, previousStatus, newStatus, changedBy, comment, changedAt));
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
