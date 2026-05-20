package com.africa.ubaxplatform.unit.mandate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurAgencyLinkRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import com.africa.ubaxplatform.mandate.dto.CreateMandateRequest;
import com.africa.ubaxplatform.mandate.dto.MandateResponse;
import com.africa.ubaxplatform.mandate.dto.TerminateMandateRequest;
import com.africa.ubaxplatform.mandate.entity.ManagementContract;
import com.africa.ubaxplatform.mandate.repository.ManagementContractRepository;
import com.africa.ubaxplatform.mandate.service.impl.MandateServiceImpl;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("MandateServiceImpl – tests unitaires")
class MandateServiceImplTest {

  @Mock private ManagementContractRepository mandateRepo;
  @Mock private UserRepository userRepo;
  @Mock private BailleurAgencyLinkRepository bailleurAgencyLinkRepo;

  @InjectMocks private MandateServiceImpl service;

  private static final UUID MANDATE_ID = UUID.randomUUID();
  private static final UUID OWNER_ID = UUID.randomUUID();

  private Agency agency;
  private User caller;
  private User owner;
  private ManagementContract draftMandate;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    caller = SharedTestFixtures.buildPartnerUser();

    owner = SharedTestFixtures.buildPartnerUser("kc-owner-" + UUID.randomUUID(), OWNER_ID, agency);

    draftMandate =
        ManagementContract.builder()
            .agency(agency)
            .owner(owner)
            .createdBy(caller)
            .status(MandateStatus.DRAFT)
            .startDate(LocalDate.now())
            .build();
    SharedTestFixtures.injectId(draftMandate, MANDATE_ID);
  }

  private CreateMandateRequest buildCreateRequest() {
    CreateMandateRequest req = new CreateMandateRequest();
    req.setOwnerId(OWNER_ID);
    req.setStartDate(LocalDate.now());
    return req;
  }

  // ── create() ────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – mandat DRAFT créé pour un bailleur lié à l'agence")
    void create_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(userRepo.findById(OWNER_ID)).thenReturn(Optional.of(owner));
      when(bailleurAgencyLinkRepo.existsByBailleurUserIdAndAgencyId(
              OWNER_ID, SharedTestFixtures.AGENCY_ID))
          .thenReturn(true);
      when(mandateRepo.existsByAgencyIdAndOwnerIdAndStatusIn(
              eq(SharedTestFixtures.AGENCY_ID), eq(OWNER_ID), any()))
          .thenReturn(false);
      when(mandateRepo.save(any(ManagementContract.class)))
          .thenAnswer(
              inv -> {
                ManagementContract m = inv.getArgument(0);
                SharedTestFixtures.injectId(m, MANDATE_ID);
                return m;
              });

      MandateResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest());

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(MandateStatus.DRAFT);
      assertThat(resp.referenceNumber()).startsWith("MAN-");
    }

    @Test
    @DisplayName("Echec – caller sans agence → MANDATE_ACCESS_DENIED")
    void create_callerWithoutAgency_throwsAccessDenied() {
      User noAgencyUser =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgencyUser));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_ACCESS_DENIED);

      verify(mandateRepo, never()).save(any());
    }

    @Test
    @DisplayName("Echec – caller introuvable → USER_NOT_FOUND")
    void create_callerNotFound_throwsUserNotFound() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bailleur introuvable → USER_NOT_FOUND")
    void create_ownerNotFound_throwsUserNotFound() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(userRepo.findById(OWNER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bailleur non lié à l'agence → MANDATE_ACCESS_DENIED")
    void create_bailleurNotLinked_throwsAccessDenied() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(userRepo.findById(OWNER_ID)).thenReturn(Optional.of(owner));
      when(bailleurAgencyLinkRepo.existsByBailleurUserIdAndAgencyId(
              OWNER_ID, SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_ACCESS_DENIED);

      verify(mandateRepo, never()).save(any());
    }

    @Test
    @DisplayName("Echec – mandat ACTIVE ou PENDING_SIGNATURE déjà existant → MANDATE_CONFLICT")
    void create_duplicateActiveMandate_throwsConflict() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(userRepo.findById(OWNER_ID)).thenReturn(Optional.of(owner));
      when(bailleurAgencyLinkRepo.existsByBailleurUserIdAndAgencyId(
              OWNER_ID, SharedTestFixtures.AGENCY_ID))
          .thenReturn(true);
      when(mandateRepo.existsByAgencyIdAndOwnerIdAndStatusIn(
              eq(SharedTestFixtures.AGENCY_ID), eq(OWNER_ID), any()))
          .thenReturn(true);

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_CONFLICT);

      verify(mandateRepo, never()).save(any());
    }
  }

  // ── list() ──────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("list()")
  class ListMandates {

    @Test
    @DisplayName("Succès – retourne la page de mandats de l'agence")
    void list_success() throws CustomException {
      Page<ManagementContract> page = new PageImpl<>(List.of(draftMandate));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findByAgencyId(eq(SharedTestFixtures.AGENCY_ID), eq(null), any()))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, null, PageRequest.of(0, 20));

      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).status()).isEqualTo(MandateStatus.DRAFT);
    }

    @Test
    @DisplayName("Succès – filtre par statut ACTIVE")
    void list_filteredByStatus() throws CustomException {
      Page<ManagementContract> empty = Page.empty();
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findByAgencyId(
              eq(SharedTestFixtures.AGENCY_ID), eq(MandateStatus.ACTIVE), any()))
          .thenReturn(empty);

      var result =
          service.list(SharedTestFixtures.KEYCLOAK_ID, MandateStatus.ACTIVE, PageRequest.of(0, 20));

      assertThat(result.getTotalElements()).isEqualTo(0);
    }
  }

  // ── getById() ───────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – retourne le détail du mandat")
    void getById_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      MandateResponse resp = service.getById(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(MandateStatus.DRAFT);
    }

    @Test
    @DisplayName("Echec – mandat introuvable → MANDATE_NOT_FOUND")
    void getById_notFound_throwsMandateNotFound() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – caller d'une autre agence → MANDATE_ACCESS_DENIED")
    void getById_wrongAgency_throwsAccessDenied() {
      Agency otherAgency = Agency.builder().name("Autre Agence").city("Bouaké").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());
      User otherCaller =
          SharedTestFixtures.buildPartnerUser(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID, otherAgency);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(otherCaller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.getById(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_ACCESS_DENIED);
    }
  }

  // ── submit() ────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("submit()")
  class Submit {

    @Test
    @DisplayName("Succès – DRAFT → PENDING_SIGNATURE")
    void submit_draft_toPendingSignature() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));
      when(mandateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      MandateResponse resp = service.submit(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID);

      assertThat(resp.status()).isEqualTo(MandateStatus.PENDING_SIGNATURE);
    }

    @Test
    @DisplayName(
        "Echec – statut PENDING_SIGNATURE → transition invalide → MANDATE_INVALID_TRANSITION")
    void submit_alreadyPending_throwsInvalidTransition() {
      draftMandate.setStatus(MandateStatus.PENDING_SIGNATURE);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.submit(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("Echec – statut ACTIVE → transition invalide → MANDATE_INVALID_TRANSITION")
    void submit_activeMandate_throwsInvalidTransition() {
      draftMandate.setStatus(MandateStatus.ACTIVE);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.submit(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }
  }

  // ── activate() ──────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("activate()")
  class Activate {

    @Test
    @DisplayName("Succès – PENDING_SIGNATURE → ACTIVE")
    void activate_pendingSignature_toActive() throws CustomException {
      draftMandate.setStatus(MandateStatus.PENDING_SIGNATURE);
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));
      when(mandateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      MandateResponse resp = service.activate(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID);

      assertThat(resp.status()).isEqualTo(MandateStatus.ACTIVE);
    }

    @Test
    @DisplayName("Echec – statut DRAFT → transition invalide → MANDATE_INVALID_TRANSITION")
    void activate_draftMandate_throwsInvalidTransition() {
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.activate(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("Echec – mandat introuvable → MANDATE_NOT_FOUND")
    void activate_notFound_throwsMandateNotFound() {
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.activate(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_NOT_FOUND);
    }
  }

  // ── terminate() ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("terminate()")
  class Terminate {

    @Test
    @DisplayName("Succès – ACTIVE → TERMINATED avec motif")
    void terminate_active_toTerminated() throws CustomException {
      draftMandate.setStatus(MandateStatus.ACTIVE);
      TerminateMandateRequest req = new TerminateMandateRequest();
      req.setTerminationReason("Non-respect des clauses contractuelles.");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));
      when(mandateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      MandateResponse resp = service.terminate(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID, req);

      assertThat(resp.status()).isEqualTo(MandateStatus.TERMINATED);
      assertThat(resp.terminationReason()).isEqualTo("Non-respect des clauses contractuelles.");
      assertThat(resp.terminatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Echec – statut DRAFT → transition invalide → MANDATE_INVALID_TRANSITION")
    void terminate_draftMandate_throwsInvalidTransition() {
      TerminateMandateRequest req = new TerminateMandateRequest();
      req.setTerminationReason("Motif quelconque");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.terminate(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName(
        "Echec – statut PENDING_SIGNATURE → transition invalide → MANDATE_INVALID_TRANSITION")
    void terminate_pendingSignature_throwsInvalidTransition() {
      draftMandate.setStatus(MandateStatus.PENDING_SIGNATURE);
      TerminateMandateRequest req = new TerminateMandateRequest();
      req.setTerminationReason("Motif quelconque");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.terminate(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }
  }

  // ── cancel() ────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("cancel()")
  class Cancel {

    @Test
    @DisplayName("Succès – DRAFT → CANCELLED")
    void cancel_draft_toCancelled() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));
      when(mandateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      MandateResponse resp = service.cancel(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID);

      assertThat(resp.status()).isEqualTo(MandateStatus.CANCELLED);
    }

    @Test
    @DisplayName("Succès – PENDING_SIGNATURE → CANCELLED")
    void cancel_pendingSignature_toCancelled() throws CustomException {
      draftMandate.setStatus(MandateStatus.PENDING_SIGNATURE);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));
      when(mandateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      MandateResponse resp = service.cancel(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID);

      assertThat(resp.status()).isEqualTo(MandateStatus.CANCELLED);
    }

    @Test
    @DisplayName("Echec – statut ACTIVE → ne peut pas être annulé → MANDATE_INVALID_TRANSITION")
    void cancel_activeMandate_throwsInvalidTransition() {
      draftMandate.setStatus(MandateStatus.ACTIVE);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.cancel(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("Echec – statut TERMINATED → ne peut pas être annulé → MANDATE_INVALID_TRANSITION")
    void cancel_terminatedMandate_throwsInvalidTransition() {
      draftMandate.setStatus(MandateStatus.TERMINATED);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.cancel(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("Echec – caller d'une autre agence → MANDATE_ACCESS_DENIED")
    void cancel_wrongAgency_throwsAccessDenied() {
      Agency otherAgency = Agency.builder().name("Autre Agence").city("Bouaké").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());
      User otherCaller =
          SharedTestFixtures.buildPartnerUser(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID, otherAgency);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(otherCaller));
      when(mandateRepo.findById(MANDATE_ID)).thenReturn(Optional.of(draftMandate));

      assertThatThrownBy(() -> service.cancel(SharedTestFixtures.KEYCLOAK_ID, MANDATE_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.MANDATE_ACCESS_DENIED);
    }
  }
}
