package com.africa.ubaxplatform.unit.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.mapper.AgencyMapper;
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
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import com.africa.ubaxplatform.partner.entity.ApplicationStatusLog;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import com.africa.ubaxplatform.partner.mapper.PartnerApplicationMapper;
import com.africa.ubaxplatform.partner.repository.ApplicationStatusLogRepository;
import com.africa.ubaxplatform.partner.repository.PartnerApplicationRepository;
import com.africa.ubaxplatform.partner.service.impl.PartnerApplicationServiceImpl;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import com.africa.ubaxplatform.testHelper.PartnerTestFixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartnerApplicationServiceImpl – tests unitaires")
class PartnerApplicationServiceImplTest {

  @Mock private PartnerApplicationRepository applicationRepo;
  @Mock private ApplicationStatusLogRepository statusLogRepo;
  @Mock private UserRepository userRepo;
  @Mock private AgencyRepository agencyRepo;
  @Mock private KeycloakAdminService keycloakAdminService;
  @Mock private EmailService emailService;
  @Mock private MinioService minioService;
  @Spy private PartnerApplicationMapper mapper = new PartnerApplicationMapper();
  @Spy private AgencyMapper agencyMapper = new AgencyMapper();
  @InjectMocks private PartnerApplicationServiceImpl service;

  private static final String ADMIN_EMAIL = "admin@ubax.com";
  private static final String ADMIN_KEYCLOAK_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "adminEmail", ADMIN_EMAIL);
  }

  @Nested
  @DisplayName("apply()")
  class Apply {

    @Test
    @DisplayName("Succès – application créée, logs et emails envoyés")
    void apply_success_createsApplicationAndNotifies() throws Exception {
      PartnerApplicationRequest req = PartnerTestFixtures.buildRequest();
      UUID applicationId = UUID.randomUUID();

      when(applicationRepo.existsByEmailAndStatusNot(req.getEmail(), ApplicationStatus.REJECTED))
          .thenReturn(false);
      when(minioService.initPartnerDirectory(req.getCompanyName())).thenReturn("acme-sarl");
      when(minioService.uploadPartnerLegalDoc(
              anyString(), anyString(), any(), any(long.class), anyString()))
          .thenReturn("https://minio/partner-documents/acme-sarl/legal/rccm.pdf");
      when(minioService.uploadPartnerLogo(anyString(), any(), any(long.class), anyString()))
          .thenReturn("https://minio/partner-documents/acme-sarl/logo/logo.png");
      when(applicationRepo.save(any(PartnerApplication.class)))
          .thenAnswer(
              inv -> {
                PartnerApplication app = inv.getArgument(0);
                try {
                  var idField =
                      com.africa.ubaxplatform.common.base.BaseEntity.class.getDeclaredField("id");
                  idField.setAccessible(true);
                  idField.set(app, applicationId);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
                return app;
              });
      when(statusLogRepo.save(any(ApplicationStatusLog.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      PartnerApplicationResponse response =
          service.apply(
              req,
              PartnerTestFixtures.pdfFile("rccm"),
              PartnerTestFixtures.pdfFile("dfe"),
              PartnerTestFixtures.pdfFile("bail"),
              PartnerTestFixtures.pngFile("logo"));

      assertThat(response).isNotNull();
      assertThat(response.getCompanyName()).isEqualTo("Acme SARL");
      assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PENDING);

      verify(applicationRepo).save(any(PartnerApplication.class));
      verify(statusLogRepo).save(any(ApplicationStatusLog.class));
      verify(emailService)
          .sendPartnerApplicationAcknowledge(
              eq(req.getEmail()), eq(req.getCompanyName()), anyString());
      verify(emailService)
          .sendPartnerApplicationAdminNotif(
              eq(ADMIN_EMAIL),
              eq(req.getCompanyName()),
              eq(req.getPartnerType()),
              eq(req.getEmail()),
              anyString());
    }

    @Test
    @DisplayName("Succès – sans fichiers optionnels (null)")
    void apply_success_withNullFiles_createsApplication() throws Exception {
      PartnerApplicationRequest req = PartnerTestFixtures.buildRequest();
      UUID applicationId = UUID.randomUUID();

      when(applicationRepo.existsByEmailAndStatusNot(req.getEmail(), ApplicationStatus.REJECTED))
          .thenReturn(false);
      when(minioService.initPartnerDirectory(req.getCompanyName())).thenReturn("acme-sarl");
      when(applicationRepo.save(any(PartnerApplication.class)))
          .thenAnswer(
              inv -> {
                PartnerApplication app = inv.getArgument(0);
                try {
                  var idField =
                      com.africa.ubaxplatform.common.base.BaseEntity.class.getDeclaredField("id");
                  idField.setAccessible(true);
                  idField.set(app, applicationId);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
                return app;
              });
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      // Fichiers null → uploadLegal/uploadLogo retournent null sans appeler MinIO
      PartnerApplicationResponse response = service.apply(req, null, null, null, null);

      assertThat(response).isNotNull();
      verify(minioService, never())
          .uploadPartnerLegalDoc(anyString(), anyString(), any(), any(long.class), anyString());
      verify(minioService, never())
          .uploadPartnerLogo(anyString(), any(), any(long.class), anyString());
    }

    @Test
    @DisplayName("Échec – email déjà utilisé (non rejeté) → ConflictException")
    void apply_emailAlreadyExists_throwsConflictException() {
      PartnerApplicationRequest req = PartnerTestFixtures.buildRequest();
      when(applicationRepo.existsByEmailAndStatusNot(req.getEmail(), ApplicationStatus.REJECTED))
          .thenReturn(true);

      assertThatThrownBy(() -> service.apply(req, null, null, null, null))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_APPLICATION_ALREADY_EXISTS);

      verify(applicationRepo, never()).save(any());
      verify(emailService, never())
          .sendPartnerApplicationAcknowledge(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Échec – type MIME document invalide → BadRequestException")
    void apply_invalidDocumentMimeType_throwsBadRequestException() {
      PartnerApplicationRequest req = PartnerTestFixtures.buildRequest();
      when(applicationRepo.existsByEmailAndStatusNot(req.getEmail(), ApplicationStatus.REJECTED))
          .thenReturn(false);
      when(minioService.initPartnerDirectory(anyString())).thenReturn("acme-sarl");

      // Type MIME invalide pour un document légal
      MockMultipartFile invalidFile =
          new MockMultipartFile("rccm", "rccm.exe", "application/x-msdownload", new byte[100]);

      assertThatThrownBy(() -> service.apply(req, invalidFile, null, null, null))
          .isInstanceOf(BadRequestException.class);

      verify(applicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – fichier document trop volumineux → BadRequestException")
    void apply_documentTooLarge_throwsBadRequestException() {
      PartnerApplicationRequest req = PartnerTestFixtures.buildRequest();
      when(applicationRepo.existsByEmailAndStatusNot(req.getEmail(), ApplicationStatus.REJECTED))
          .thenReturn(false);
      when(minioService.initPartnerDirectory(anyString())).thenReturn("acme-sarl");

      // Fichier > 10 Mo
      byte[] largeContent = new byte[11 * 1024 * 1024];
      MockMultipartFile largeFile =
          new MockMultipartFile("rccm", "rccm.pdf", "application/pdf", largeContent);

      assertThatThrownBy(() -> service.apply(req, largeFile, null, null, null))
          .isInstanceOf(BadRequestException.class);

      verify(applicationRepo, never()).save(any());
    }
  }

  @Nested
  @DisplayName("listApplications()")
  class ListApplications {

    @Test
    @DisplayName("Succès – avec filtre de statut")
    void listApplications_withStatus_returnsFilteredPage() {
      Pageable pageable = PageRequest.of(0, 10);
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(UUID.randomUUID(), ApplicationStatus.PENDING);
      Page<PartnerApplication> page = new PageImpl<>(List.of(app));

      when(applicationRepo.findByStatus(ApplicationStatus.PENDING, pageable)).thenReturn(page);

      Page<PartnerApplicationResponse> result =
          service.listApplications(ApplicationStatus.PENDING, pageable);

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getStatus()).isEqualTo(ApplicationStatus.PENDING);
      verify(applicationRepo).findByStatus(ApplicationStatus.PENDING, pageable);
      verify(applicationRepo, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Succès – sans filtre, retourne toutes les demandes")
    void listApplications_withoutStatus_returnsAll() {
      Pageable pageable = PageRequest.of(0, 10);
      Page<PartnerApplication> page =
          new PageImpl<>(
              List.of(
                  PartnerTestFixtures.buildApplicationWithId(
                      UUID.randomUUID(), ApplicationStatus.PENDING),
                  PartnerTestFixtures.buildApplicationWithId(
                      UUID.randomUUID(), ApplicationStatus.APPROVED)));

      when(applicationRepo.findAll(pageable)).thenReturn(page);

      Page<PartnerApplicationResponse> result = service.listApplications(null, pageable);

      assertThat(result.getContent()).hasSize(2);
      verify(applicationRepo).findAll(pageable);
      verify(applicationRepo, never()).findByStatus(any(), any());
    }
  }

  @Nested
  @DisplayName("getApplication()")
  class GetApplication {

    @Test
    @DisplayName("Succès – retourne la demande avec son historique de statut")
    void getApplication_found_returnsResponseWithHistory() {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      ApplicationStatusLog log =
          ApplicationStatusLog.builder()
              .application(app)
              .previousStatus(null)
              .newStatus(ApplicationStatus.PENDING)
              .build();

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(statusLogRepo.findByApplicationIdOrderByChangedAtAsc(id)).thenReturn(List.of(log));

      PartnerApplicationResponse response = service.getApplication(id);

      assertThat(response).isNotNull();
      assertThat(response.getStatusHistory()).hasSize(1);
      assertThat(response.getStatusHistory().getFirst().getNewStatus())
          .isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("Échec – demande introuvable → NotFoundException")
    void getApplication_notFound_throwsNotFoundException() {
      UUID id = UUID.randomUUID();
      when(applicationRepo.findById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getApplication(id)).isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("decide()")
  class Decide {

    @Test
    @DisplayName("Succès – APPROVED sans type agence, email envoyé")
    void decide_approved_nonAgency_sendsApprovedEmail() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(keycloakAdminService.createPartnerAccount(
              anyString(), anyString(), anyString(), anyString()))
          .thenReturn("kc-partner-id");

      PartnerApplicationResponse response =
          service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.APPROVED, null);

      assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
      verify(keycloakAdminService)
          .createPartnerAccount(
              eq(app.getEmail()), eq(app.getCompanyName()),
              eq(app.getLegalRepresentative()), eq(app.getPhone()));
      verify(keycloakAdminService).assignRole("kc-partner-id", UserRole.PARTNER);
      verify(keycloakAdminService).sendSetPasswordLink("kc-partner-id");
      verify(userRepo).save(any(User.class));
      verify(emailService)
          .sendPartnerApplicationApproved(
              eq(app.getEmail()), eq(app.getCompanyName()), anyString());
    }

    @Test
    @DisplayName("Succès – APPROVED type agence, Agency créée en base")
    void decide_approved_agencyType_createsAgencyEntity() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerApplication.builder()
              .companyName("Agence Immo CI")
              .legalRepresentative("Marie Martin")
              .email("immo@agence.ci")
              .phone("+2250722222222")
              .country("CI")
              .city("Abidjan")
              .postalAddress("BP 456")
              .registrationNumber("CI-IMM-001")
              .partnerType(Constants.CodeList.PartnerType.AGENCE_IMMOBILIERE)
              .status(ApplicationStatus.PENDING)
              .build();
      try {
        var f = com.africa.ubaxplatform.common.base.BaseEntity.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(app, id);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(keycloakAdminService.createPartnerAccount(
              anyString(), anyString(), anyString(), anyString()))
          .thenReturn("kc-agency-id");
      when(agencyRepo.save(any(Agency.class))).thenAnswer(inv -> inv.getArgument(0));

      service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.APPROVED, null);

      verify(agencyRepo).save(any(Agency.class));
      verify(keycloakAdminService).assignRole("kc-agency-id", UserRole.PARTNER);

      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepo).save(userCaptor.capture());
      assertThat(userCaptor.getValue().getAgency()).isNotNull();
    }

    @Test
    @DisplayName("Succès – REJECTED avec commentaire, email rejet envoyé")
    void decide_rejected_withComment_sendsRejectedEmail() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);
      String comment = "Dossier incomplet";

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      PartnerApplicationResponse response =
          service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.REJECTED, comment);

      assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
      assertThat(response.getRejectionReason()).isEqualTo(comment);
      verify(emailService)
          .sendPartnerApplicationRejected(
              eq(app.getEmail()), eq(app.getCompanyName()), eq(comment));
      // Pas de provisionnement Keycloak pour un rejet
      verify(keycloakAdminService, never())
          .createPartnerAccount(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Succès – INCOMPLETE avec commentaire, email incomplet envoyé")
    void decide_incomplete_withComment_sendsIncompleteEmail() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);
      String comment = "Merci de fournir le RCCM complet";

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.INCOMPLETE, comment);

      verify(emailService)
          .sendPartnerApplicationIncomplete(
              eq(app.getEmail()), eq(app.getCompanyName()), eq(comment));
    }

    @Test
    @DisplayName("Succès – UNDER_REVIEW, email sous examen envoyé")
    void decide_underReview_sendsUnderReviewEmail() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.UNDER_REVIEW, null);

      verify(emailService)
          .sendPartnerApplicationUnderReview(eq(app.getEmail()), eq(app.getCompanyName()));
      verify(keycloakAdminService, never())
          .createPartnerAccount(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Échec – transition PENDING interdite → BadRequestException")
    void decide_pendingTransition_throwsBadRequestException() {
      assertThatThrownBy(
              () ->
                  service.decide(
                      UUID.randomUUID(), ADMIN_KEYCLOAK_ID, ApplicationStatus.PENDING, null))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_APPLICATION_INVALID_TRANSITION);

      verify(applicationRepo, never()).findById(any());
    }

    @Test
    @DisplayName("Échec – REJECTED sans commentaire → BadRequestException")
    void decide_rejectedWithoutComment_throwsBadRequestException() {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));

      assertThatThrownBy(
              () -> service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.REJECTED, null))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_APPLICATION_COMMENT_REQUIRED);

      verify(applicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – INCOMPLETE avec commentaire vide → BadRequestException")
    void decide_incompleteWithBlankComment_throwsBadRequestException() {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));

      assertThatThrownBy(
              () -> service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.INCOMPLETE, "   "))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_APPLICATION_COMMENT_REQUIRED);
    }

    @Test
    @DisplayName("Échec – demande introuvable → NotFoundException")
    void decide_applicationNotFound_throwsNotFoundException() {
      UUID id = UUID.randomUUID();
      when(applicationRepo.findById(id)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.APPROVED, null))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Échec – admin introuvable → NotFoundException")
    void decide_adminNotFound_throwsNotFoundException() {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.APPROVED, null))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName(
        "Resilience – échec provisionnement Keycloak lors APPROVED → décision persistée quand même")
    void decide_approved_keycloakProvisioningFails_decisionStillPersisted() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      // Keycloak échoue → CustomException absorbée par provisionPartnerAccount
      when(keycloakAdminService.createPartnerAccount(
              anyString(), anyString(), anyString(), anyString()))
          .thenThrow(new CustomException(new RuntimeException(), "Erreur Keycloak"));

      // La décision ne doit pas échouer malgré l'erreur de provisionnement
      PartnerApplicationResponse response =
          service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.APPROVED, null);

      assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
      verify(applicationRepo).save(any());
      verify(statusLogRepo).save(any());
      // Email de décision envoyé quand même
      verify(emailService).sendPartnerApplicationApproved(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Audit – log de statut créé avec les bons attributs")
    void decide_approved_statusLogSavedWithCorrectAttributes() throws CustomException {
      UUID id = UUID.randomUUID();
      PartnerApplication app =
          PartnerTestFixtures.buildApplicationWithId(id, ApplicationStatus.PENDING);
      User admin = PartnerTestFixtures.buildAdmin(ADMIN_KEYCLOAK_ID);

      when(applicationRepo.findById(id)).thenReturn(Optional.of(app));
      when(userRepo.findByKeycloakId(ADMIN_KEYCLOAK_ID)).thenReturn(Optional.of(admin));
      when(applicationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(statusLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(keycloakAdminService.createPartnerAccount(
              anyString(), anyString(), anyString(), anyString()))
          .thenReturn("kc-id");

      service.decide(id, ADMIN_KEYCLOAK_ID, ApplicationStatus.APPROVED, null);

      ArgumentCaptor<ApplicationStatusLog> logCaptor =
          ArgumentCaptor.forClass(ApplicationStatusLog.class);
      verify(statusLogRepo).save(logCaptor.capture());
      ApplicationStatusLog savedLog = logCaptor.getValue();
      assertThat(savedLog.getPreviousStatus()).isEqualTo(ApplicationStatus.PENDING);
      assertThat(savedLog.getNewStatus()).isEqualTo(ApplicationStatus.APPROVED);
      assertThat(savedLog.getChangedBy()).isEqualTo(admin);
    }
  }
}
