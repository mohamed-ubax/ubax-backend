package com.africa.ubaxplatform.unit.visitappointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import com.africa.ubaxplatform.visitappointment.codeList.VisitRequestStatus;
import com.africa.ubaxplatform.visitappointment.dto.ConfigureVisitAvailabilityDto;
import com.africa.ubaxplatform.visitappointment.dto.ConfirmVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.CreateVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.RejectVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.UpdateBlackoutDatesDto;
import com.africa.ubaxplatform.visitappointment.dto.VisitRequestResponse;
import com.africa.ubaxplatform.visitappointment.entity.AgencyVisitAvailability;
import com.africa.ubaxplatform.visitappointment.entity.PropertyVisitRequest;
import com.africa.ubaxplatform.visitappointment.repository.AgencyVisitAvailabilityRepository;
import com.africa.ubaxplatform.visitappointment.repository.PropertyVisitRequestRepository;
import com.africa.ubaxplatform.visitappointment.service.impl.PropertyVisitServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
@DisplayName("PropertyVisitServiceImpl")
class PropertyVisitServiceImplTest {

  @Mock PropertyVisitRequestRepository visitRequestRepository;
  @Mock AgencyVisitAvailabilityRepository availabilityRepository;
  @Mock UserRepository userRepository;
  @Mock PropertyRepository propertyRepository;

  @InjectMocks PropertyVisitServiceImpl service;

  // ── Constants ────────────────────────────────────────────────────

  private static final String CLIENT_KC_ID = "client-kc-" + UUID.randomUUID();
  private static final String PARTNER_KC_ID = SharedTestFixtures.KEYCLOAK_ID;
  private static final UUID CLIENT_ID = UUID.randomUUID();
  private static final UUID PARTNER_ID = SharedTestFixtures.USER_ID;
  private static final UUID AGENCY_ID = SharedTestFixtures.AGENCY_ID;
  private static final UUID PROPERTY_ID = SharedTestFixtures.PROPERTY_ID;
  private static final UUID VISIT_REQUEST_ID = UUID.randomUUID();
  private static final UUID AVAILABILITY_ID = UUID.randomUUID();

  // ── Fixtures ─────────────────────────────────────────────────────

  private Agency agency;
  private User clientUser;
  private User partnerUser;
  private Property property;
  private AgencyVisitAvailability availability;
  private PropertyVisitRequest pendingRequest;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();

    clientUser = SharedTestFixtures.buildUserWithoutAgency(CLIENT_KC_ID, CLIENT_ID);

    partnerUser = SharedTestFixtures.buildPartnerUser(PARTNER_KC_ID, PARTNER_ID, agency);

    property = Property.builder().title("Bel Appartement T3 Cocody").agency(agency).build();
    SharedTestFixtures.injectId(property, PROPERTY_ID);

    availability =
        AgencyVisitAvailability.builder()
            .property(property)
            .agency(agency)
            .timeSlotsConfig("{}")
            .blackoutDates(new LocalDate[0])
            .maxVisitsPerSlot(3)
            .isActive(true)
            .build();
    SharedTestFixtures.injectId(availability, AVAILABILITY_ID);

    pendingRequest =
        PropertyVisitRequest.builder()
            .property(property)
            .client(clientUser)
            .requestedDate(LocalDate.now().plusDays(3))
            .requestedTimeSlot("10:00-14:00")
            .status(VisitRequestStatus.PENDING)
            .build();
    SharedTestFixtures.injectId(pendingRequest, VISIT_REQUEST_ID);
  }

  // ── createVisitRequest ────────────────────────────────────────────

  @Nested
  @DisplayName("createVisitRequest()")
  class CreateVisitRequest {

    private CreateVisitRequestDto validRequest() {
      return CreateVisitRequestDto.builder()
          .propertyId(PROPERTY_ID.toString())
          .requestedDate(LocalDate.now().plusDays(3))
          .requestedTimeSlot("10:00-14:00")
          .clientNotes("Très intéressé, disponible en journée")
          .build();
    }

    @Test
    @DisplayName("Client non trouvé – lève NotFoundException")
    void createVisitRequest_clientNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, validRequest()))
          .isInstanceOf(NotFoundException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bien non trouvé – lève NotFoundException")
    void createVisitRequest_propertyNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, validRequest()))
          .isInstanceOf(NotFoundException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Date aujourd'hui – lève CustomException (doit être au minimum demain)")
    void createVisitRequest_dateToday_throwsCustomException() {
      CreateVisitRequestDto request =
          CreateVisitRequestDto.builder()
              .propertyId(PROPERTY_ID.toString())
              .requestedDate(LocalDate.now())
              .requestedTimeSlot("10:00-14:00")
              .build();

      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, request))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Date passée – lève CustomException")
    void createVisitRequest_datePast_throwsCustomException() {
      CreateVisitRequestDto request =
          CreateVisitRequestDto.builder()
              .propertyId(PROPERTY_ID.toString())
              .requestedDate(LocalDate.now().minusDays(1))
              .requestedTimeSlot("10:00-14:00")
              .build();

      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, request))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Demande déjà existante pour ce bien – lève CustomException (doublon)")
    void createVisitRequest_duplicateRequest_throwsCustomException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(visitRequestRepository.findByClientIdAndPropertyId(CLIENT_ID, PROPERTY_ID))
          .thenReturn(Optional.of(pendingRequest));

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, validRequest()))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aucune disponibilité configurée – lève CustomException")
    void createVisitRequest_noAvailabilityConfigured_throwsCustomException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(visitRequestRepository.findByClientIdAndPropertyId(CLIENT_ID, PROPERTY_ID))
          .thenReturn(Optional.empty());
      when(availabilityRepository.findByPropertyId(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, validRequest()))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Disponibilité inactive – lève CustomException")
    void createVisitRequest_availabilityInactive_throwsCustomException() {
      availability.setIsActive(false);

      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(visitRequestRepository.findByClientIdAndPropertyId(CLIENT_ID, PROPERTY_ID))
          .thenReturn(Optional.empty());
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));

      assertThatThrownBy(() -> service.createVisitRequest(CLIENT_KC_ID, validRequest()))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas nominal – crée la demande avec statut PENDING")
    void createVisitRequest_validRequest_createsPendingVisitRequest() throws Exception {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(visitRequestRepository.findByClientIdAndPropertyId(CLIENT_ID, PROPERTY_ID))
          .thenReturn(Optional.empty());
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));
      when(visitRequestRepository.save(any(PropertyVisitRequest.class))).thenReturn(pendingRequest);

      VisitRequestResponse result = service.createVisitRequest(CLIENT_KC_ID, validRequest());

      assertThat(result.getStatus()).isEqualTo(VisitRequestStatus.PENDING);
      assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID);
      assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
      verify(visitRequestRepository).save(any(PropertyVisitRequest.class));
    }
  }

  // ── getMyVisitRequests ─────────────────────────────────────────────

  @Nested
  @DisplayName("getMyVisitRequests()")
  class GetMyVisitRequests {

    @Test
    @DisplayName("Client non trouvé – lève NotFoundException")
    void getMyVisitRequests_clientNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getMyVisitRequests(CLIENT_KC_ID, Pageable.unpaged()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Cas nominal – retourne la page paginée des demandes du client")
    void getMyVisitRequests_validClient_returnsPageOfRequests() throws Exception {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findByClientIdOrderByCreatedAtDesc(CLIENT_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(pendingRequest)));

      var result = service.getMyVisitRequests(CLIENT_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getStatus()).isEqualTo(VisitRequestStatus.PENDING);
    }

    @Test
    @DisplayName("Aucune demande – retourne page vide")
    void getMyVisitRequests_noRequests_returnsEmptyPage() throws Exception {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findByClientIdOrderByCreatedAtDesc(CLIENT_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      var result = service.getMyVisitRequests(CLIENT_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
    }
  }

  // ── getVisitRequestDetail ──────────────────────────────────────────

  @Nested
  @DisplayName("getVisitRequestDetail()")
  class GetVisitRequestDetail {

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void getDetail_callerNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getVisitRequestDetail(CLIENT_KC_ID, VISIT_REQUEST_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Demande non trouvée – lève NotFoundException")
    void getDetail_requestNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getVisitRequestDetail(CLIENT_KC_ID, VISIT_REQUEST_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Client propriétaire de la demande – accès accordé")
    void getDetail_callerIsClient_accessGranted() throws Exception {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      VisitRequestResponse result = service.getVisitRequestDetail(CLIENT_KC_ID, VISIT_REQUEST_ID);

      assertThat(result.getId()).isEqualTo(VISIT_REQUEST_ID);
    }

    @Test
    @DisplayName("Agent de l'agence propriétaire du bien – accès accordé")
    void getDetail_callerIsAgencyMember_accessGranted() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      VisitRequestResponse result = service.getVisitRequestDetail(PARTNER_KC_ID, VISIT_REQUEST_ID);

      assertThat(result.getId()).isEqualTo(VISIT_REQUEST_ID);
    }

    @Test
    @DisplayName("Utilisateur tiers (ni client ni agence) – lève CustomException (accès refusé)")
    void getDetail_thirdParty_throwsCustomException() {
      UUID otherId = UUID.randomUUID();
      User otherUser = SharedTestFixtures.buildUserWithoutAgency("other-kc", otherId);

      when(userRepository.findByKeycloakId("other-kc")).thenReturn(Optional.of(otherUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      assertThatThrownBy(() -> service.getVisitRequestDetail("other-kc", VISIT_REQUEST_ID))
          .isInstanceOf(CustomException.class);
    }
  }

  // ── cancelVisitRequest ─────────────────────────────────────────────

  @Nested
  @DisplayName("cancelVisitRequest()")
  class CancelVisitRequest {

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void cancel_callerNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.cancelVisitRequest(CLIENT_KC_ID, VISIT_REQUEST_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Demande non trouvée – lève NotFoundException")
    void cancel_requestNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.cancelVisitRequest(CLIENT_KC_ID, VISIT_REQUEST_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Autre client essaie d'annuler – lève CustomException")
    void cancel_notOwner_throwsCustomException() {
      UUID otherId = UUID.randomUUID();
      User otherClient = SharedTestFixtures.buildUserWithoutAgency("other-kc", otherId);

      when(userRepository.findByKeycloakId("other-kc")).thenReturn(Optional.of(otherClient));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      assertThatThrownBy(() -> service.cancelVisitRequest("other-kc", VISIT_REQUEST_ID))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Demande déjà CONFIRMED – lève CustomException (seul PENDING est annulable)")
    void cancel_confirmedRequest_throwsCustomException() {
      pendingRequest.setStatus(VisitRequestStatus.CONFIRMED);

      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      assertThatThrownBy(() -> service.cancelVisitRequest(CLIENT_KC_ID, VISIT_REQUEST_ID))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas nominal – annule la demande PENDING → CANCELLED")
    void cancel_pendingRequest_setsCancelledStatus() throws Exception {
      when(userRepository.findByKeycloakId(CLIENT_KC_ID)).thenReturn(Optional.of(clientUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(visitRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

      service.cancelVisitRequest(CLIENT_KC_ID, VISIT_REQUEST_ID);

      assertThat(pendingRequest.getStatus()).isEqualTo(VisitRequestStatus.CANCELLED);
      verify(visitRequestRepository).save(pendingRequest);
    }
  }

  // ── getAvailableSlotsForProperty ───────────────────────────────────

  @Nested
  @DisplayName("getAvailableSlotsForProperty()")
  class GetAvailableSlotsForProperty {

    @Test
    @DisplayName("Bien non trouvé – lève NotFoundException")
    void getAvailableSlots_propertyNotFound_throwsNotFoundException() {
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getAvailableSlotsForProperty(PROPERTY_ID, 30))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Aucune disponibilité configurée – lève CustomException")
    void getAvailableSlots_noAvailability_throwsCustomException() {
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getAvailableSlotsForProperty(PROPERTY_ID, 30))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Disponibilité inactive – lève CustomException")
    void getAvailableSlots_availabilityInactive_throwsCustomException() {
      availability.setIsActive(false);

      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));

      assertThatThrownBy(() -> service.getAvailableSlotsForProperty(PROPERTY_ID, 30))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Aucun créneau configuré (timeSlotsConfig vide) – retourne listes vides")
    void getAvailableSlots_noSlotsConfigured_returnsEmptyResponse() throws Exception {
      // timeSlotsConfig = "{}" → aucun jour ouvert
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));

      var result = service.getAvailableSlotsForProperty(PROPERTY_ID, 5);

      assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID.toString());
      assertThat(result.getAvailableDates()).isEmpty();
      assertThat(result.getSlotsByDate()).isEmpty();
    }
  }

  // ── configureAvailability ──────────────────────────────────────────

  @Nested
  @DisplayName("configureAvailability()")
  class ConfigureAvailability {

    private ConfigureVisitAvailabilityDto validConfig() {
      return ConfigureVisitAvailabilityDto.builder()
          .propertyId(PROPERTY_ID.toString())
          .timeSlots(
              Map.of("1", List.of("10:00-14:00", "14:00-18:00"), "5", List.of("10:00-18:00")))
          .maxVisitsPerSlot(3)
          .build();
    }

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void configure_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.configureAvailability(PARTNER_KC_ID, validConfig()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Pas membre d'une agence – lève CustomException")
    void configure_noAgency_throwsCustomException() {
      User userWithoutAgency = SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, PARTNER_ID);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(userWithoutAgency));

      assertThatThrownBy(() -> service.configureAvailability(PARTNER_KC_ID, validConfig()))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Bien non trouvé – lève NotFoundException")
    void configure_propertyNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.configureAvailability(PARTNER_KC_ID, validConfig()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Bien n'appartient pas à l'agence – lève CustomException")
    void configure_propertyNotOwnedByAgency_throwsCustomException() {
      Agency otherAgency = Agency.builder().name("Autre Agence").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());

      Property otherProperty =
          Property.builder().title("Bien autre agence").agency(otherAgency).build();
      SharedTestFixtures.injectId(otherProperty, PROPERTY_ID);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(otherProperty));

      assertThatThrownBy(() -> service.configureAvailability(PARTNER_KC_ID, validConfig()))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Nouvelle configuration (pas d'existante) – crée et sauvegarde la config")
    void configure_noExistingConfig_createsNewAvailability() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.empty())
          .thenReturn(Optional.of(availability)); // second call from getAvailableSlotsForProperty
      when(availabilityRepository.save(any(AgencyVisitAvailability.class)))
          .thenReturn(availability);

      service.configureAvailability(PARTNER_KC_ID, validConfig());

      verify(availabilityRepository).save(any(AgencyVisitAvailability.class));
    }

    @Test
    @DisplayName("Configuration existante – met à jour (idempotent)")
    void configure_existingConfig_updatesExistingAvailability() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));
      when(availabilityRepository.save(any(AgencyVisitAvailability.class)))
          .thenReturn(availability);

      service.configureAvailability(PARTNER_KC_ID, validConfig());

      verify(availabilityRepository).save(availability);
    }
  }

  // ── getConfiguredAvailability ──────────────────────────────────────

  @Nested
  @DisplayName("getConfiguredAvailability()")
  class GetConfiguredAvailability {

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void getConfigured_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getConfiguredAvailability(PARTNER_KC_ID, PROPERTY_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Pas membre d'une agence – lève CustomException")
    void getConfigured_noAgency_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(
              Optional.of(SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, PARTNER_ID)));

      assertThatThrownBy(() -> service.getConfiguredAvailability(PARTNER_KC_ID, PROPERTY_ID))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Bien n'appartient pas à l'agence – lève CustomException")
    void getConfigured_propertyNotOwnedByAgency_throwsCustomException() {
      Agency otherAgency = Agency.builder().name("Autre").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());
      Property otherProperty = Property.builder().agency(otherAgency).build();
      SharedTestFixtures.injectId(otherProperty, PROPERTY_ID);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(otherProperty));

      assertThatThrownBy(() -> service.getConfiguredAvailability(PARTNER_KC_ID, PROPERTY_ID))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Cas nominal – délègue à getAvailableSlotsForProperty")
    void getConfigured_validAccess_returnsAvailabilityResponse() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));

      var result = service.getConfiguredAvailability(PARTNER_KC_ID, PROPERTY_ID);

      assertThat(result.getPropertyId()).isEqualTo(PROPERTY_ID.toString());
    }
  }

  // ── updateBlackoutDates ────────────────────────────────────────────

  @Nested
  @DisplayName("updateBlackoutDates()")
  class UpdateBlackoutDates {

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void updateBlackout_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.updateBlackoutDates(
                      PARTNER_KC_ID,
                      PROPERTY_ID,
                      new UpdateBlackoutDatesDto(List.of(LocalDate.now().plusDays(7)))))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Pas membre d'une agence – lève CustomException")
    void updateBlackout_noAgency_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(
              Optional.of(SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, PARTNER_ID)));

      assertThatThrownBy(
              () ->
                  service.updateBlackoutDates(
                      PARTNER_KC_ID, PROPERTY_ID, new UpdateBlackoutDatesDto(List.of())))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Configuration non trouvée – lève NotFoundException")
    void updateBlackout_availabilityNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.updateBlackoutDates(
                      PARTNER_KC_ID, PROPERTY_ID, new UpdateBlackoutDatesDto(List.of())))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Configuration d'une autre agence – lève CustomException")
    void updateBlackout_differentAgency_throwsCustomException() {
      Agency otherAgency = Agency.builder().name("Autre").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());

      AgencyVisitAvailability otherAvailability =
          AgencyVisitAvailability.builder()
              .property(property)
              .agency(otherAgency)
              .timeSlotsConfig("{}")
              .blackoutDates(new LocalDate[0])
              .maxVisitsPerSlot(3)
              .isActive(true)
              .build();
      SharedTestFixtures.injectId(otherAvailability, UUID.randomUUID());

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(otherAvailability));

      assertThatThrownBy(
              () ->
                  service.updateBlackoutDates(
                      PARTNER_KC_ID, PROPERTY_ID, new UpdateBlackoutDatesDto(List.of())))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Cas nominal – met à jour les dates blackout et sauvegarde")
    void updateBlackout_validRequest_savesNewBlackoutDates() throws Exception {
      List<LocalDate> newDates = List.of(LocalDate.now().plusDays(7), LocalDate.now().plusDays(8));

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));
      when(availabilityRepository.save(availability)).thenReturn(availability);

      service.updateBlackoutDates(PARTNER_KC_ID, PROPERTY_ID, new UpdateBlackoutDatesDto(newDates));

      assertThat(availability.getBlackoutDates()).hasSize(2);
      verify(availabilityRepository).save(availability);
    }

    @Test
    @DisplayName("Liste vide – supprime toutes les dates blackout existantes")
    void updateBlackout_emptyList_clearsAllBlackoutDates() throws Exception {
      availability.setBlackoutDates(new LocalDate[] {LocalDate.now().plusDays(10)});

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));
      when(availabilityRepository.save(availability)).thenReturn(availability);

      service.updateBlackoutDates(
          PARTNER_KC_ID, PROPERTY_ID, new UpdateBlackoutDatesDto(List.of()));

      assertThat(availability.getBlackoutDates()).isEmpty();
      verify(availabilityRepository).save(availability);
    }
  }

  // ── getVisitRequestsForAgency ──────────────────────────────────────

  @Nested
  @DisplayName("getVisitRequestsForAgency()")
  class GetVisitRequestsForAgency {

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void getForAgency_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getVisitRequestsForAgency(PARTNER_KC_ID, Pageable.unpaged()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Pas membre d'une agence – lève CustomException")
    void getForAgency_noAgency_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(
              Optional.of(SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, PARTNER_ID)));

      assertThatThrownBy(() -> service.getVisitRequestsForAgency(PARTNER_KC_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Cas nominal – retourne les demandes PENDING de l'agence")
    void getForAgency_validPartner_returnsPendingRequests() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findPendingByAgencyId(AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(pendingRequest)));

      var result = service.getVisitRequestsForAgency(PARTNER_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getStatus()).isEqualTo(VisitRequestStatus.PENDING);
    }
  }

  // ── confirmVisitRequest ────────────────────────────────────────────

  @Nested
  @DisplayName("confirmVisitRequest()")
  class ConfirmVisitRequest {

    private ConfirmVisitRequestDto validConfirm() {
      return ConfirmVisitRequestDto.builder()
          .confirmedDate(LocalDate.now().plusDays(3))
          .confirmedTimeSlot("10:00-14:00")
          .build();
    }

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void confirm_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Pas membre d'une agence – lève CustomException")
    void confirm_noAgency_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(
              Optional.of(SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, PARTNER_ID)));

      assertThatThrownBy(
              () -> service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm()))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Demande non trouvée – lève NotFoundException")
    void confirm_requestNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Demande d'une autre agence – lève CustomException")
    void confirm_requestNotOwnedByAgency_throwsCustomException() {
      Agency otherAgency = Agency.builder().name("Autre").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());
      Property otherProperty = Property.builder().agency(otherAgency).build();
      SharedTestFixtures.injectId(otherProperty, UUID.randomUUID());

      PropertyVisitRequest otherRequest =
          PropertyVisitRequest.builder()
              .property(otherProperty)
              .client(clientUser)
              .status(VisitRequestStatus.PENDING)
              .build();
      SharedTestFixtures.injectId(otherRequest, VISIT_REQUEST_ID);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID)).thenReturn(Optional.of(otherRequest));

      assertThatThrownBy(
              () -> service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm()))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Demande déjà CONFIRMED – lève CustomException (statut invalide)")
    void confirm_alreadyConfirmed_throwsCustomException() {
      pendingRequest.setStatus(VisitRequestStatus.CONFIRMED);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      assertThatThrownBy(
              () -> service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm()))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Créneau complet (bookings >= maxVisitsPerSlot) – lève CustomException")
    void confirm_slotFull_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(visitRequestRepository.countConfirmedForDateAndSlot(
              PROPERTY_ID, validConfirm().getConfirmedDate(), "10:00-14:00"))
          .thenReturn(3L); // 3 >= maxVisitsPerSlot (3)
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));

      assertThatThrownBy(
              () -> service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm()))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas nominal – confirme et retourne la demande avec statut CONFIRMED")
    void confirm_validRequest_setsConfirmedStatus() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(visitRequestRepository.countConfirmedForDateAndSlot(
              PROPERTY_ID, validConfirm().getConfirmedDate(), "10:00-14:00"))
          .thenReturn(1L); // 1 < maxVisitsPerSlot (3)
      when(availabilityRepository.findByPropertyId(PROPERTY_ID))
          .thenReturn(Optional.of(availability));
      when(visitRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

      VisitRequestResponse result =
          service.confirmVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validConfirm());

      assertThat(pendingRequest.getStatus()).isEqualTo(VisitRequestStatus.CONFIRMED);
      assertThat(result).isNotNull();
      verify(visitRequestRepository).save(pendingRequest);
    }
  }

  // ── rejectVisitRequest ─────────────────────────────────────────────

  @Nested
  @DisplayName("rejectVisitRequest()")
  class RejectVisitRequest {

    private RejectVisitRequestDto validReject() {
      return RejectVisitRequestDto.builder().reason("Bien vendu").build();
    }

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void reject_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.rejectVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validReject()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Demande non PENDING – lève CustomException")
    void reject_requestNotPending_throwsCustomException() {
      pendingRequest.setStatus(VisitRequestStatus.CONFIRMED);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));

      assertThatThrownBy(
              () -> service.rejectVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validReject()))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas nominal – rejette la demande PENDING → REJECTED avec motif")
    void reject_validRequest_setsRejectedStatusAndReason() throws Exception {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(visitRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

      VisitRequestResponse result =
          service.rejectVisitRequest(PARTNER_KC_ID, VISIT_REQUEST_ID, validReject());

      assertThat(pendingRequest.getStatus()).isEqualTo(VisitRequestStatus.REJECTED);
      assertThat(pendingRequest.getRejectionReason()).isEqualTo("Bien vendu");
      assertThat(result).isNotNull();
      verify(visitRequestRepository).save(pendingRequest);
    }
  }

  // ── assignAgent ────────────────────────────────────────────────────

  @Nested
  @DisplayName("assignAgent()")
  class AssignAgent {

    private static final UUID AGENT_ID = UUID.randomUUID();

    private User buildAgent(Agency agencyForAgent) {
      String agentKcId = "agent-kc-" + AGENT_ID;
      User agent = SharedTestFixtures.buildPartnerUser(agentKcId, AGENT_ID, agencyForAgent);
      return agent;
    }

    @Test
    @DisplayName("Utilisateur non trouvé – lève NotFoundException")
    void assign_userNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignAgent(PARTNER_KC_ID, VISIT_REQUEST_ID, AGENT_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Pas membre d'une agence – lève CustomException")
    void assign_noAgency_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(
              Optional.of(SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, PARTNER_ID)));

      assertThatThrownBy(() -> service.assignAgent(PARTNER_KC_ID, VISIT_REQUEST_ID, AGENT_ID))
          .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("Demande non trouvée – lève NotFoundException")
    void assign_requestNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignAgent(PARTNER_KC_ID, VISIT_REQUEST_ID, AGENT_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Agent non trouvé – lève NotFoundException")
    void assign_agentNotFound_throwsNotFoundException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(userRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignAgent(PARTNER_KC_ID, VISIT_REQUEST_ID, AGENT_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Agent d'une autre agence – lève CustomException")
    void assign_agentFromDifferentAgency_throwsCustomException() {
      Agency otherAgency = Agency.builder().name("Autre Agence").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());

      User agentFromOtherAgency = buildAgent(otherAgency);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agentFromOtherAgency));

      assertThatThrownBy(() -> service.assignAgent(PARTNER_KC_ID, VISIT_REQUEST_ID, AGENT_ID))
          .isInstanceOf(CustomException.class);

      verify(visitRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas nominal – assigne l'agent et sauvegarde")
    void assign_validAgent_assignsAndSaves() throws Exception {
      User agent = buildAgent(agency);

      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.of(partnerUser));
      when(visitRequestRepository.findById(VISIT_REQUEST_ID))
          .thenReturn(Optional.of(pendingRequest));
      when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));
      when(visitRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

      VisitRequestResponse result = service.assignAgent(PARTNER_KC_ID, VISIT_REQUEST_ID, AGENT_ID);

      assertThat(pendingRequest.getAgent()).isEqualTo(agent);
      assertThat(result).isNotNull();
      verify(visitRequestRepository).save(pendingRequest);
    }
  }
}
