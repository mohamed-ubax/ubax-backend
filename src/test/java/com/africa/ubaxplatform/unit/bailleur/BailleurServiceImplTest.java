package com.africa.ubaxplatform.unit.bailleur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
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
import com.africa.ubaxplatform.notification.service.SmsService;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
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
  @Mock SmsService smsService;

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

  // ── apply ──────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("apply()")
  class Apply {

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
      req.setFirstName("Mamadou");
      req.setLastName("Coulibaly");
      req.setPhone("+2250700000099");
      req.setEmail("mamadou@example.ci");
      req.setIdType("CNI");
      req.setIdNumber("CI-9999");
      req.setProperties(List.of(prop));
      return req;
    }

    @BeforeEach
    void stubCommon() {
      lenient().when(codeListRepo.findAllByType(any())).thenReturn(List.of());
      lenient().when(applicationRepo.save(any())).thenReturn(pendingApp);
      lenient().when(bailleurPropRepo.save(any())).thenReturn(savedProp);
    }

    @Test
    @DisplayName("Echec – agence introuvable → BAILLEUR_AGENCY_NOT_FOUND")
    void apply_agencyNotFound_throws() {
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.apply(buildRequest(true)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_AGENCY_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bailleur existant avec bien chez une autre agence → CONFLICT")
    void apply_existingBailleurGeoConflict_throws() {
      User existingOwner = SharedTestFixtures.buildUserWithoutAgency("kc-owner", UUID.randomUUID());
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByPhone("+2250700000099")).thenReturn(Optional.of(existingOwner));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              existingOwner.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(true);

      assertThatThrownBy(() -> service.apply(buildRequest(true)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.BAILLEUR_APPLICATION_CONFLICT);
    }

    @Test
    @DisplayName("Succès – bailleur existant, pas de conflit")
    void apply_existingBailleur_noConflict_success() throws CustomException {
      User existingOwner = SharedTestFixtures.buildUserWithoutAgency("kc-owner", UUID.randomUUID());
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByPhone("+2250700000099")).thenReturn(Optional.of(existingOwner));
      when(propertyRepository.existsByOwnerIdAndAgencyIdNotNullAndAgencyIdNot(
              existingOwner.getId(), SharedTestFixtures.AGENCY_ID))
          .thenReturn(false);

      BailleurApplicationResponse result = service.apply(buildRequest(true));

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(BailleurApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("Succès – nouveau bailleur avec coordonnées, pas de conflit géo")
    void apply_newBailleur_withCoords_noGeoConflict_success() throws CustomException {
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByPhone("+2250700000099")).thenReturn(Optional.empty());
      when(propertyRepository.existsNearLocationForOtherAgency(
              anyDouble(), anyDouble(), anyString(), anyDouble()))
          .thenReturn(false);

      BailleurApplicationResponse result = service.apply(buildRequest(true));

      assertThat(result).isNotNull();
      assertThat(result.isConflictDetected()).isFalse();
    }

    @Test
    @DisplayName("Succès – nouveau bailleur sans coordonnées → conflictDetected=true en sortie")
    void apply_newBailleur_noCoords_conflictFlagged() throws CustomException {
      pendingApp.setConflictDetected(true);
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByPhone("+2250700000099")).thenReturn(Optional.empty());

      BailleurApplicationResponse result = service.apply(buildRequest(false));

      assertThat(result).isNotNull();
      verify(applicationRepo).save(any(BailleurApplication.class));
    }

    @Test
    @DisplayName("Succès – nouveau bailleur avec conflit géo → conflictDetected signalé")
    void apply_newBailleur_withCoords_geoConflict_flagged() throws CustomException {
      pendingApp.setConflictDetected(true);
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByPhone("+2250700000099")).thenReturn(Optional.empty());
      when(propertyRepository.existsNearLocationForOtherAgency(
              anyDouble(), anyDouble(), anyString(), anyDouble()))
          .thenReturn(true);

      BailleurApplicationResponse result = service.apply(buildRequest(true));

      assertThat(result).isNotNull();
      assertThat(result.isConflictDetected()).isTrue();
    }
  }

  // ── processDecision ────────────────────────────────────────────────────────

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
    @DisplayName("Succès – REJECT : statut mis à REJECTED et motif enregistré")
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
    }

    @Test
    @DisplayName("Succès – APPROVE nouveau bailleur : compte Keycloak créé et lien établi")
    void processDecision_approve_newBailleur_success() throws CustomException {
      String newKcId = "kc-new-bailleur";
      User savedBailleur = SharedTestFixtures.buildUserWithoutAgency(newKcId, UUID.randomUUID());

      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findByPhone(pendingApp.getPhone())).thenReturn(Optional.empty());
      when(keycloakAdminService.createBailleurAccount(any(), any(), any(), any(), any()))
          .thenReturn(newKcId);
      when(userRepo.save(any(User.class))).thenReturn(savedBailleur);
      when(linkRepo.existsByBailleurUserIdAndAgencyId(any(), any())).thenReturn(false);
      when(linkRepo.save(any())).thenReturn(mock(BailleurAgencyLink.class));
      when(applicationRepo.save(pendingApp)).thenReturn(pendingApp);
      when(bailleurPropRepo.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

      BailleurDecisionRequest req = new BailleurDecisionRequest();
      req.setDecision(BailleurDecisionRequest.Decision.APPROVE);

      BailleurApplicationResponse result =
          service.processDecision(APPLICATION_ID, req, REVIEWER_KC_ID);

      assertThat(result).isNotNull();
      assertThat(pendingApp.getStatus()).isEqualTo(BailleurApplicationStatus.APPROVED);
      verify(keycloakAdminService).createBailleurAccount(any(), any(), any(), any(), any());
      verify(keycloakAdminService).assignRole(newKcId, UserRole.OWNER);
      verify(smsService).sendSms(any(), any());
      verify(linkRepo).save(any());
    }

    @Test
    @DisplayName("Succès – APPROVE bailleur existant déjà lié : pas de nouveau lien créé")
    void processDecision_approve_existingBailleurAlreadyLinked_success() throws CustomException {
      User existingBailleur =
          SharedTestFixtures.buildUserWithoutAgency("kc-existing", UUID.randomUUID());

      when(applicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApp));
      when(agencyRepo.findById(SharedTestFixtures.AGENCY_ID)).thenReturn(Optional.of(agency));
      when(userRepo.findByKeycloakId(REVIEWER_KC_ID)).thenReturn(Optional.of(reviewer));
      when(userRepo.findByPhone(pendingApp.getPhone())).thenReturn(Optional.of(existingBailleur));
      when(linkRepo.existsByBailleurUserIdAndAgencyId(
              existingBailleur.getId(), SharedTestFixtures.AGENCY_ID))
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
      verify(keycloakAdminService, never())
          .createBailleurAccount(any(), any(), any(), any(), any());
    }
  }

  // ── getById ────────────────────────────────────────────────────────────────

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

  // ── listByAgency ───────────────────────────────────────────────────────────

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

  // ── listAll ────────────────────────────────────────────────────────────────

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
}
