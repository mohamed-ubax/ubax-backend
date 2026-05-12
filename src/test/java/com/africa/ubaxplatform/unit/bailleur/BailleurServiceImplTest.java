package com.africa.ubaxplatform.unit.bailleur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.africa.ubaxplatform.bailleur.dto.BailleurPropertyRequest;
import com.africa.ubaxplatform.bailleur.entity.BailleurAgencyLink;
import com.africa.ubaxplatform.bailleur.entity.BailleurApplication;
import com.africa.ubaxplatform.bailleur.entity.BailleurApplicationProperty;
import com.africa.ubaxplatform.bailleur.repository.BailleurAgencyLinkRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurApplicationPropertyRepository;
import com.africa.ubaxplatform.bailleur.repository.BailleurApplicationRepository;
import com.africa.ubaxplatform.bailleur.service.impl.BailleurServiceImpl;
import com.africa.ubaxplatform.common.codelist.repository.LaCodeListRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("BailleurServiceImpl")
class BailleurServiceImplTest {

  @Mock BailleurApplicationRepository applicationRepo;
  @Mock BailleurApplicationPropertyRepository bailleurPropRepo;
  @Mock BailleurAgencyLinkRepository linkRepo;
  @Mock UserRepository userRepo;
  @Mock AgencyRepository agencyRepo;
  @Mock PropertyRepository propertyRepository;
  @Mock LaCodeListRepository codeListRepo;
  @Mock KeycloakAdminService keycloakAdminService;

  @InjectMocks BailleurServiceImpl service;

  private static final UUID APPLICATION_ID = UUID.randomUUID();
  private static final String REVIEWER_KC_ID = "kc-reviewer-001";

  private Agency agency;
  private BailleurApplication pendingApp;
  private User reviewer;
  private BailleurApplicationProperty savedProp;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();

    // userId null → legacy application (no authenticated account linked)
    pendingApp =
        BailleurApplication.builder()
            .agencyId(SharedTestFixtures.AGENCY_ID)
            .firstName("Mamadou")
            .lastName("Coulibaly")
            .phone("+2250700000099")
            .email("mamadou@example.ci")
            .idType("CNI")
            .idNumber("CI-9999")
            .status(BailleurApplicationStatus.PENDING)
            .conflictDetected(false)
            .build();
    SharedTestFixtures.injectId(pendingApp, APPLICATION_ID);

    reviewer = SharedTestFixtures.buildAdminUser(REVIEWER_KC_ID, UUID.randomUUID(), UserRole.ADMIN);

    savedProp =
        BailleurApplicationProperty.builder()
            .applicationId(APPLICATION_ID)
            .address("12 Rue Test, Abidjan")
            .propertyType("VILLA")
            .rooms(4)
            .build();
    SharedTestFixtures.injectId(savedProp, UUID.randomUUID());
  }

  // apply

  @Nested
  @DisplayName("apply()")
  class Apply {

    private static final String CALLER_KC_ID = "kc-caller-apply";
    private User callerUser;

    @BeforeEach
    void stubCommon() {
      callerUser = SharedTestFixtures.buildUserWithoutAgency(CALLER_KC_ID, UUID.randomUUID());
      lenient().when(userRepo.findByKeycloakId(CALLER_KC_ID)).thenReturn(Optional.of(callerUser));
      lenient().when(codeListRepo.findAllByType(any())).thenReturn(List.of());
      lenient().when(applicationRepo.save(any())).thenReturn(pendingApp);
      lenient().when(bailleurPropRepo.save(any())).thenReturn(savedProp);
    }

    private BailleurApplyRequest buildRequest(boolean withCoords) {
      BailleurPropertyRequest prop = new BailleurPropertyRequest();
      prop.setAddress("12 Rue Test, Abidjan");
      prop.setPropertyType("VILLA");
      prop.setRooms(4);
      if (withCoords) {
        prop.setLatitude(BigDecimal.valueOf(5.345));
        prop.setLongitude(BigDecimal.valueOf(-4.007));
      }

      BailleurApplyRequest req = new BailleurApplyRequest();
      req.setAgencyId(SharedTestFixtures.AGENCY_ID);
      req.setIdType("CNI");
      req.setIdNumber("CI-9999");
      req.setProperties(List.of(prop));
      return req;
    }

    @Test
    @DisplayName("Echec – appelant introuvable en base → USER_NOT_FOUND")
    void apply_userNotFound_throws() {
      when(userRepo.findByKeycloakId(CALLER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.apply(CALLER_KC_ID, buildRequest(true)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – agence introuvable → BAILLEUR_AGENCY_NOT_FOUND")
    void apply_agencyNotFound_throws() {
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.apply(CALLER_KC_ID, buildRequest(true)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bailleur avec un bien géré par une autre agence → CONFLICT")
    void apply_callerHasPropertyAtOtherAgency_throws() {
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              callerUser.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(true);

      assertThatThrownBy(() -> service.apply(CALLER_KC_ID, buildRequest(true)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_APPLICATION_CONFLICT);
    }

    @Test
    @DisplayName("Succès – coordonnées présentes, pas de conflit → conflictDetected=false")
    void apply_withCoords_noConflict_success() throws CustomException {
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              callerUser.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);
      when(propertyRepository.existsNearLocationForOtherAgency(
              anyDouble(), anyDouble(), anyString(), anyDouble()))
          .thenReturn(false);

      BailleurApplicationResponse result = service.apply(CALLER_KC_ID, buildRequest(true));

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(BailleurApplicationStatus.PENDING);
      assertThat(result.isConflictDetected()).isFalse();
    }

    @Test
    @DisplayName("Succès – coordonnées absentes → conflictDetected=true (vérification manuelle)")
    void apply_withoutCoords_conflictFlagged() throws CustomException {
      pendingApp.setConflictDetected(true);
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              callerUser.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);

      BailleurApplicationResponse result = service.apply(CALLER_KC_ID, buildRequest(false));

      assertThat(result).isNotNull();
      assertThat(result.isConflictDetected()).isTrue();
      verify(applicationRepo).save(any(BailleurApplication.class));
    }

    @Test
    @DisplayName("Succès – conflit géo détecté → conflictDetected=true signalé")
    void apply_withCoords_geoConflict_flagged() throws CustomException {
      pendingApp.setConflictDetected(true);
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              callerUser.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);
      when(propertyRepository.existsNearLocationForOtherAgency(
              anyDouble(), anyDouble(), anyString(), anyDouble()))
          .thenReturn(true);

      BailleurApplicationResponse result = service.apply(CALLER_KC_ID, buildRequest(true));

      assertThat(result).isNotNull();
      assertThat(result.isConflictDetected()).isTrue();
    }

    @Test
    @DisplayName("Succès – userId de l'appelant persisté dans la demande")
    void apply_storesCallerUserId() throws CustomException {
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              callerUser.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);
      when(propertyRepository.existsNearLocationForOtherAgency(
              anyDouble(), anyDouble(), anyString(), anyDouble()))
          .thenReturn(false);

      service.apply(CALLER_KC_ID, buildRequest(true));

      verify(applicationRepo).save(argThat(app -> callerUser.getId().equals(app.getUserId())));
    }
  }

  // processDecision

  @Nested
  @DisplayName("processDecision()")
  class ProcessDecision {

    @Test
    @DisplayName("Echec – demande introuvable → BAILLEUR_APPLICATION_NOT_FOUND")
    void processDecision_notFound_throws() {
      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.empty());
      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.REJECT);

      assertThatThrownBy(() -> service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – demande déjà traitée → BAILLEUR_APPLICATION_INVALID_STATUS")
    void processDecision_alreadyProcessed_throws() {
      pendingApp.setStatus(BailleurApplicationStatus.APPROVED);
      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      assertThatThrownBy(() -> service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_APPLICATION_INVALID_STATUS);
    }

    @Test
    @DisplayName("Succès – REJECT : statut REJECTED, motif enregistré, Keycloak non touché")
    void processDecision_reject_success() throws CustomException {
      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(applicationRepo.save(pendingApp)).thenReturn(pendingApp);
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.REJECT);
      req.setComment("Pièces manquantes");

      BailleurApplicationResponse result =
          service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID);

      assertThat(result).isNotNull();
      assertThat(pendingApp.getStatus()).isEqualTo(BailleurApplicationStatus.REJECTED);
      assertThat(pendingApp.getRejectionReason()).isEqualTo("Pièces manquantes");
      verify(keycloakAdminService, never()).assignRole(any(), any());
    }

    @Test
    @DisplayName("Succès – APPROVE nouveau flux (userId présent) : rôle OWNER assigné, lien créé")
    void processDecision_approve_newPath_ownerRoleAssigned_linkCreated() throws CustomException {
      UUID bailleurUserId = UUID.randomUUID();
      User bailleur = SharedTestFixtures.buildUserWithoutAgency("kc-bailleur-new", bailleurUserId);
      pendingApp.setUserId(bailleurUserId);

      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findById(bailleurUserId)).thenReturn(Optional.of(bailleur));
      when(linkRepo.existsByBailleurUserIdAndAgencyId(bailleurUserId, SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);
      when(applicationRepo.save(pendingApp)).thenReturn(pendingApp);
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      BailleurApplicationResponse result =
          service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID);

      assertThat(result).isNotNull();
      assertThat(pendingApp.getStatus()).isEqualTo(BailleurApplicationStatus.APPROVED);
      verify(keycloakAdminService).assignRole("kc-bailleur-new", UserRole.OWNER);
      verify(userRepo).save(bailleur);
      verify(linkRepo).save(any());
    }

    @Test
    @DisplayName("Succès – APPROVE rôle OWNER déjà présent : assignRole et save user ignorés")
    void processDecision_approve_ownerAlreadyPresent_skipRoleAssignment() throws CustomException {
      UUID bailleurUserId = UUID.randomUUID();
      User bailleur =
          SharedTestFixtures.buildUserWithoutAgency("kc-bailleur-owner", bailleurUserId);
      bailleur.getRoles().add(UserRole.OWNER);
      pendingApp.setUserId(bailleurUserId);

      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findById(bailleurUserId)).thenReturn(Optional.of(bailleur));
      when(linkRepo.existsByBailleurUserIdAndAgencyId(bailleurUserId, SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);
      when(applicationRepo.save(pendingApp)).thenReturn(pendingApp);
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID);

      verify(keycloakAdminService, never()).assignRole(any(), any());
      verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("Succès – APPROVE lien déjà existant : linkRepo.save non appelé")
    void processDecision_approve_linkAlreadyExists_skipLinkCreation() throws CustomException {
      UUID bailleurUserId = UUID.randomUUID();
      User bailleur =
          SharedTestFixtures.buildUserWithoutAgency("kc-bailleur-linked", bailleurUserId);
      pendingApp.setUserId(bailleurUserId);

      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findById(bailleurUserId)).thenReturn(Optional.of(bailleur));
      when(linkRepo.existsByBailleurUserIdAndAgencyId(bailleurUserId, SharedTestFixtures.AGENCY_ID))
          .thenReturn(true);
      when(applicationRepo.save(pendingApp)).thenReturn(pendingApp);
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      BailleurApplicationResponse result =
          service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID);

      assertThat(result).isNotNull();
      assertThat(pendingApp.getStatus()).isEqualTo(BailleurApplicationStatus.APPROVED);
      verify(linkRepo, never()).save(any());
    }

    @Test
    @DisplayName(
        "Succès – APPROVE flux legacy (userId null, compte trouvé par téléphone) : OWNER assigné")
    void processDecision_approve_legacyPath_userFoundByPhone_ownerRoleAssigned()
        throws CustomException {
      // pendingApp.userId is null → legacy path
      User legacyUser = SharedTestFixtures.buildUserWithoutAgency("kc-legacy", UUID.randomUUID());

      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findByPhone(pendingApp.getPhone())).thenReturn(Optional.of(legacyUser));
      when(linkRepo.existsByBailleurUserIdAndAgencyId(
              legacyUser.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);
      when(applicationRepo.save(pendingApp)).thenReturn(pendingApp);
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      BailleurApplicationResponse result =
          service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID);

      assertThat(result).isNotNull();
      assertThat(pendingApp.getStatus()).isEqualTo(BailleurApplicationStatus.APPROVED);
      verify(keycloakAdminService).assignRole(legacyUser.getKeycloakId(), UserRole.OWNER);
      verify(linkRepo).save(any());
    }

    @Test
    @DisplayName(
        "Echec – APPROVE flux legacy (userId null, téléphone introuvable) → USER_NOT_FOUND")
    void processDecision_approve_legacyPath_userNotFound_throws() {
      // pendingApp.userId is null → legacy path
      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findByPhone(pendingApp.getPhone())).thenReturn(Optional.empty());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      assertThatThrownBy(() -> service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  // getById

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – retourne la réponse de la demande")
    void getById_success() throws CustomException {
      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(savedProp));

      BailleurApplicationResponse result = service.getById(APPLICATION_ID);

      assertThat(result).isNotNull();
      assertThat(result.getFirstName()).isEqualTo("Mamadou");
      assertThat(result.getProperties()).hasSize(1);
    }

    @Test
    @DisplayName("Echec – demande introuvable → BAILLEUR_APPLICATION_NOT_FOUND")
    void getById_notFound_throws() {
      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(APPLICATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_APPLICATION_NOT_FOUND);
    }
  }

  // listByAgency

  @Nested
  @DisplayName("listByAgency()")
  class ListByAgency {

    @Test
    @DisplayName("Retourne une page de demandes pour l'agence")
    void listByAgency_returnsPage() {
      when(applicationRepo.findByAgencyId(SharedTestFixtures.AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(pendingApp)));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      var result = service.listByAgency(SharedTestFixtures.AGENCY_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getAgencyName()).isEqualTo("Agence Test CI");
    }
  }

  // listAll

  @Nested
  @DisplayName("listAll()")
  class ListAll {

    @Test
    @DisplayName("Retourne toutes les demandes (vue admin)")
    void listAll_returnsPage() {
      when(applicationRepo.findAll(Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(pendingApp)));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      var result = service.listAll(Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
    }
  }

  // getMyApplications

  @Nested
  @DisplayName("getMyApplications()")
  class GetMyApplications {

    private static final String CALLER_KC_ID = "kc-caller-myapps";
    private User callerUser;

    @BeforeEach
    void setUpCaller() {
      callerUser = SharedTestFixtures.buildUserWithoutAgency(CALLER_KC_ID, UUID.randomUUID());
      lenient().when(userRepo.findByKeycloakId(CALLER_KC_ID)).thenReturn(Optional.of(callerUser));
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void getMyApplications_userNotFound_throws() {
      when(userRepo.findByKeycloakId(CALLER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getMyApplications(CALLER_KC_ID, null, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Sans filtre statut – appelle findByUserId et retourne la page")
    void getMyApplications_withoutStatus_returnsAll() throws CustomException {
      when(applicationRepo.findByUserId(callerUser.getId(), Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(pendingApp)));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(savedProp));

      var result = service.getMyApplications(CALLER_KC_ID, null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      verify(applicationRepo).findByUserId(callerUser.getId(), Pageable.unpaged());
      verify(applicationRepo, never()).findByUserIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Avec filtre statut PENDING – appelle findByUserIdAndStatus")
    void getMyApplications_withStatus_filtersResults() throws CustomException {
      when(applicationRepo.findByUserIdAndStatus(
              callerUser.getId(), BailleurApplicationStatus.PENDING, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(pendingApp)));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      var result =
          service.getMyApplications(
              CALLER_KC_ID, BailleurApplicationStatus.PENDING, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getStatus())
          .isEqualTo(BailleurApplicationStatus.PENDING);
      verify(applicationRepo)
          .findByUserIdAndStatus(
              callerUser.getId(), BailleurApplicationStatus.PENDING, Pageable.unpaged());
      verify(applicationRepo, never()).findByUserId(any(), any());
    }

    @Test
    @DisplayName("Aucune demande trouvée – retourne page vide sans exception")
    void getMyApplications_emptyResults_returnsEmptyPage() throws CustomException {
      when(applicationRepo.findByUserId(callerUser.getId(), Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      var result = service.getMyApplications(CALLER_KC_ID, null, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Avec filtre statut APPROVED – retourne les demandes approuvées")
    void getMyApplications_withApprovedStatus_returnsFilteredList() throws CustomException {
      BailleurApplication approvedApp =
          BailleurApplication.builder()
              .agencyId(SharedTestFixtures.AGENCY_ID)
              .firstName("Mamadou")
              .lastName("Coulibaly")
              .phone("+2250700000099")
              .email("mamadou@example.ci")
              .idType("CNI")
              .idNumber("CI-9999")
              .status(BailleurApplicationStatus.APPROVED)
              .conflictDetected(false)
              .build();
      SharedTestFixtures.injectId(approvedApp, UUID.randomUUID());

      when(applicationRepo.findByUserIdAndStatus(
              callerUser.getId(), BailleurApplicationStatus.APPROVED, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(approvedApp)));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(bailleurPropRepo.findByApplicationId(approvedApp.getId())).thenReturn(List.of());

      var result =
          service.getMyApplications(
              CALLER_KC_ID, BailleurApplicationStatus.APPROVED, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getStatus())
          .isEqualTo(BailleurApplicationStatus.APPROVED);
    }
  }

  // getMyAgencies

  @Nested
  @DisplayName("getMyAgencies()")
  class GetMyAgencies {

    private static final String OWNER_KC_ID = "kc-owner-bailleur";
    private User ownerUser;

    @BeforeEach
    void setUpOwner() {
      ownerUser = SharedTestFixtures.buildUserWithoutAgency(OWNER_KC_ID, UUID.randomUUID());
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void getMyAgencies_userNotFound_throws() {
      when(userRepo.findByKeycloakId(OWNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getMyAgencies(OWNER_KC_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Succès – retourne les agences liées avec toutes leurs informations")
    void getMyAgencies_success_returnsLinkedAgencies() throws CustomException {
      Agency linkedAgency =
          Agency.builder()
              .name("Immobilier Dakar")
              .phone("+221771234567")
              .email("contact@immo-dakar.sn")
              .logoUrl("https://minio/agencies-logos/agency1.webp")
              .build();
      SharedTestFixtures.injectId(linkedAgency, SharedTestFixtures.AGENCY_ID);

      BailleurAgencyLink link =
          BailleurAgencyLink.builder()
              .bailleurUserId(ownerUser.getId())
              .agencyId(SharedTestFixtures.AGENCY_ID)
              .joinedAt(LocalDateTime.of(2025, 4, 15, 9, 0))
              .build();
      SharedTestFixtures.injectId(link, UUID.randomUUID());

      when(userRepo.findByKeycloakId(OWNER_KC_ID)).thenReturn(Optional.of(ownerUser));
      when(linkRepo.findByBailleurUserId(ownerUser.getId())).thenReturn(List.of(link));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(linkedAgency));

      List<BailleurAgencyResponse> result = service.getMyAgencies(OWNER_KC_ID);

      assertThat(result).hasSize(1);
      BailleurAgencyResponse response = result.getFirst();
      assertThat(response.getAgencyId()).isEqualTo(SharedTestFixtures.AGENCY_ID);
      assertThat(response.getAgencyName()).isEqualTo("Immobilier Dakar");
      assertThat(response.getAgencyPhone()).isEqualTo("+221771234567");
      assertThat(response.getAgencyEmail()).isEqualTo("contact@immo-dakar.sn");
      assertThat(response.getAgencyLogo()).isEqualTo("https://minio/agencies-logos/agency1.webp");
      assertThat(response.getLinkedAt()).isEqualTo(LocalDateTime.of(2025, 4, 15, 9, 0));
    }

    @Test
    @DisplayName("Succès – aucun lien agence → retourne liste vide sans exception")
    void getMyAgencies_noLinks_returnsEmptyList() throws CustomException {
      when(userRepo.findByKeycloakId(OWNER_KC_ID)).thenReturn(Optional.of(ownerUser));
      when(linkRepo.findByBailleurUserId(ownerUser.getId())).thenReturn(List.of());

      List<BailleurAgencyResponse> result = service.getMyAgencies(OWNER_KC_ID);

      assertThat(result).isEmpty();
      verify(agencyRepo, never()).findById(any());
    }

    @Test
    @DisplayName("Succès – agence introuvable en base → champs agency null dans la réponse")
    void getMyAgencies_agencyDeleted_returnsNullAgencyFields() throws CustomException {
      UUID missingAgencyId = UUID.randomUUID();
      BailleurAgencyLink link =
          BailleurAgencyLink.builder()
              .bailleurUserId(ownerUser.getId())
              .agencyId(missingAgencyId)
              .joinedAt(LocalDateTime.now())
              .build();
      SharedTestFixtures.injectId(link, UUID.randomUUID());

      when(userRepo.findByKeycloakId(OWNER_KC_ID)).thenReturn(Optional.of(ownerUser));
      when(linkRepo.findByBailleurUserId(ownerUser.getId())).thenReturn(List.of(link));
      when(agencyRepo.findById(missingAgencyId)).thenReturn(Optional.empty());

      List<BailleurAgencyResponse> result = service.getMyAgencies(OWNER_KC_ID);

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().getAgencyName()).isNull();
      assertThat(result.getFirst().getAgencyPhone()).isNull();
      assertThat(result.getFirst().getAgencyEmail()).isNull();
    }

    @Test
    @DisplayName("Succès – plusieurs liens agences → retourne toutes les agences")
    void getMyAgencies_multipleLinks_returnsAll() throws CustomException {
      UUID agencyId2 = UUID.randomUUID();
      Agency agency2 = Agency.builder().name("Agence Lomé").build();
      SharedTestFixtures.injectId(agency2, agencyId2);

      BailleurAgencyLink link1 =
          BailleurAgencyLink.builder()
              .bailleurUserId(ownerUser.getId())
              .agencyId(SharedTestFixtures.AGENCY_ID)
              .joinedAt(LocalDateTime.now().minusDays(30))
              .build();
      SharedTestFixtures.injectId(link1, UUID.randomUUID());

      BailleurAgencyLink link2 =
          BailleurAgencyLink.builder()
              .bailleurUserId(ownerUser.getId())
              .agencyId(agencyId2)
              .joinedAt(LocalDateTime.now().minusDays(10))
              .build();
      SharedTestFixtures.injectId(link2, UUID.randomUUID());

      when(userRepo.findByKeycloakId(OWNER_KC_ID)).thenReturn(Optional.of(ownerUser));
      when(linkRepo.findByBailleurUserId(ownerUser.getId())).thenReturn(List.of(link1, link2));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(agencyRepo.findById(agencyId2)).thenReturn(Optional.of(agency2));

      List<BailleurAgencyResponse> result = service.getMyAgencies(OWNER_KC_ID);

      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(BailleurAgencyResponse::getAgencyName)
          .containsExactlyInAnyOrder("Agence Test CI", "Agence Lomé");
    }
  }
}
