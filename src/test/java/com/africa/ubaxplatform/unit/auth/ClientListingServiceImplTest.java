package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.impl.ClientListingServiceImpl;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.reservation.repository.ReservationRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientListingServiceImpl – tests unitaires")
class ClientListingServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private AgencyRepository agencyRepository;
  @Mock private HotelRepository hotelRepository;

  @InjectMocks private ClientListingServiceImpl service;

  private static final UUID HOTEL_ID = UUID.randomUUID();
  private static final UUID AGENCY_ID = UUID.randomUUID();
  private static final UUID CLIENT_ID = UUID.randomUUID();
  private static final String PARTNER_KC_ID = "partner-kc-" + UUID.randomUUID();

  private Agency agency;
  private Hotel hotel;
  private User clientUser;
  private User partnerWithAgency;
  private User partnerWithHotel;

  @BeforeEach
  void setUp() {
    agency = Agency.builder().name("Agence Test CI").city("Abidjan").build();
    SharedTestFixtures.injectId(agency, AGENCY_ID);

    hotel = Hotel.builder().name("Grand Hôtel Abidjan").build();
    SharedTestFixtures.injectId(hotel, HOTEL_ID);

    clientUser =
        User.builder()
            .keycloakId("client-kc-001")
            .firstName("Aya")
            .lastName("Kouassi")
            .email("aya@test.ci")
            .phone("+2250708080808")
            .city("Abidjan")
            .roles(new HashSet<>(Set.of(UserRole.CLIENT)))
            .build();
    SharedTestFixtures.injectId(clientUser, CLIENT_ID);

    partnerWithAgency =
        SharedTestFixtures.buildPartnerUser(PARTNER_KC_ID, UUID.randomUUID(), agency);

    partnerWithHotel =
        User.builder()
            .keycloakId(PARTNER_KC_ID)
            .firstName("Moussa")
            .lastName("Diallo")
            .email("moussa@hotel.ci")
            .phone("+2250700000099")
            .roles(new HashSet<>(Set.of(UserRole.PARTNER)))
            .hotel(hotel)
            .build();
    SharedTestFixtures.injectId(partnerWithHotel, UUID.randomUUID());
  }

  // ── listAllClients ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("listAllClients()")
  class ListAllClients {

    @Test
    @DisplayName("Succès – retourne la page de clients actifs")
    void listAllClients_withClients_returnsPage() {
      when(userRepository.findByRoleAndDeletedAtIsNull(UserRole.CLIENT, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser)));

      Page<ClientUserResponse> result = service.listAllClients(Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().email()).isEqualTo("aya@test.ci");
    }

    @Test
    @DisplayName("Succès – aucun client → page vide sans exception")
    void listAllClients_noClients_returnsEmptyPage() {
      when(userRepository.findByRoleAndDeletedAtIsNull(UserRole.CLIENT, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      Page<ClientUserResponse> result = service.listAllClients(Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Succès – plusieurs clients retournés")
    void listAllClients_multipleClients_allMapped() {
      User client2 = buildExtraClient("Koffi", "Asante", "koffi@test.ci");
      User client3 = buildExtraClient("Fatou", "Diallo", "fatou@test.ci");

      when(userRepository.findByRoleAndDeletedAtIsNull(UserRole.CLIENT, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser, client2, client3)));

      Page<ClientUserResponse> result = service.listAllClients(Pageable.unpaged());

      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getContent())
          .extracting(ClientUserResponse::email)
          .containsExactlyInAnyOrder("aya@test.ci", "koffi@test.ci", "fatou@test.ci");
    }

    @Test
    @DisplayName("Mapping – tous les champs ClientUserResponse remplis correctement")
    void listAllClients_mapsAllFields() {
      when(userRepository.findByRoleAndDeletedAtIsNull(UserRole.CLIENT, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser)));

      ClientUserResponse r = service.listAllClients(Pageable.unpaged()).getContent().getFirst();

      assertThat(r.id()).isEqualTo(CLIENT_ID);
      assertThat(r.firstName()).isEqualTo("Aya");
      assertThat(r.lastName()).isEqualTo("Kouassi");
      assertThat(r.email()).isEqualTo("aya@test.ci");
      assertThat(r.phone()).isEqualTo("+2250708080808");
      assertThat(r.city()).isEqualTo("Abidjan");
    }
  }

  // ── listClientsByHotel ─────────────────────────────────────────────────────

  @Nested
  @DisplayName("listClientsByHotel()")
  class ListClientsByHotel {

    @Test
    @DisplayName("Succès – hôtel trouvé, clients via réservations retournés")
    void listClientsByHotel_hotelExists_returnsClients() throws CustomException {
      when(hotelRepository.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));
      when(reservationRepository.findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser)));

      Page<ClientUserResponse> result = service.listClientsByHotel(HOTEL_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().email()).isEqualTo("aya@test.ci");
      verify(hotelRepository).findByIdAndDeletedAtIsNull(HOTEL_ID);
      verify(reservationRepository).findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged());
    }

    @Test
    @DisplayName("Succès – hôtel trouvé, aucune réservation → page vide")
    void listClientsByHotel_noReservations_returnsEmptyPage() throws CustomException {
      when(hotelRepository.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));
      when(reservationRepository.findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      Page<ClientUserResponse> result = service.listClientsByHotel(HOTEL_ID, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Succès – plusieurs clients distincts retournés")
    void listClientsByHotel_multipleClients_allReturned() throws CustomException {
      User client2 = buildExtraClient("Koffi", "Asante", "koffi@test.ci");

      when(hotelRepository.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));
      when(reservationRepository.findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser, client2)));

      Page<ClientUserResponse> result = service.listClientsByHotel(HOTEL_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent())
          .extracting(ClientUserResponse::email)
          .containsExactlyInAnyOrder("aya@test.ci", "koffi@test.ci");
    }

    @Test
    @DisplayName("Échec – hôtel introuvable → CustomException PARTNER_NOT_FOUND")
    void listClientsByHotel_hotelNotFound_throwsCustomException() {
      when(hotelRepository.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.listClientsByHotel(HOTEL_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_NOT_FOUND);
    }
  }

  // ── listClientsByAgency ────────────────────────────────────────────────────

  @Nested
  @DisplayName("listClientsByAgency()")
  class ListClientsByAgency {

    @Test
    @DisplayName("Succès – agence trouvée, clients via contrats retournés")
    void listClientsByAgency_agencyExists_returnsClients() throws CustomException {
      when(agencyRepository.findByIdAndDeletedAtIsNull(AGENCY_ID)).thenReturn(Optional.of(agency));
      when(contractRepository.findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser)));

      Page<ClientUserResponse> result = service.listClientsByAgency(AGENCY_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().email()).isEqualTo("aya@test.ci");
      verify(agencyRepository).findByIdAndDeletedAtIsNull(AGENCY_ID);
      verify(contractRepository).findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged());
    }

    @Test
    @DisplayName("Succès – agence trouvée, aucun contrat → page vide")
    void listClientsByAgency_noContracts_returnsEmptyPage() throws CustomException {
      when(agencyRepository.findByIdAndDeletedAtIsNull(AGENCY_ID)).thenReturn(Optional.of(agency));
      when(contractRepository.findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      Page<ClientUserResponse> result = service.listClientsByAgency(AGENCY_ID, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Succès – plusieurs clients distincts retournés")
    void listClientsByAgency_multipleClients_allReturned() throws CustomException {
      User client2 = buildExtraClient("Fatou", "Diallo", "fatou@test.ci");
      User client3 = buildExtraClient("Ibrahima", "Sow", "ibrahima@test.ci");

      when(agencyRepository.findByIdAndDeletedAtIsNull(AGENCY_ID)).thenReturn(Optional.of(agency));
      when(contractRepository.findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser, client2, client3)));

      Page<ClientUserResponse> result = service.listClientsByAgency(AGENCY_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getContent())
          .extracting(ClientUserResponse::email)
          .containsExactlyInAnyOrder("aya@test.ci", "fatou@test.ci", "ibrahima@test.ci");
    }

    @Test
    @DisplayName("Échec – agence introuvable → CustomException PARTNER_NOT_FOUND")
    void listClientsByAgency_agencyNotFound_throwsCustomException() {
      when(agencyRepository.findByIdAndDeletedAtIsNull(AGENCY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.listClientsByAgency(AGENCY_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_NOT_FOUND);
    }
  }

  // ── listClientsByCallerAgency ──────────────────────────────────────────────

  @Nested
  @DisplayName("listClientsByCallerAgency()")
  class ListClientsByCallerAgency {

    @Test
    @DisplayName("Succès – caller lié à une agence, clients retournés")
    void listClientsByCallerAgency_callerHasAgency_returnsClients() throws CustomException {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(partnerWithAgency));
      when(contractRepository.findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser)));

      Page<ClientUserResponse> result =
          service.listClientsByCallerAgency(PARTNER_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().email()).isEqualTo("aya@test.ci");
      verify(contractRepository).findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged());
    }

    @Test
    @DisplayName("Succès – caller avec agence, aucun contrat → page vide")
    void listClientsByCallerAgency_noClients_returnsEmptyPage() throws CustomException {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(partnerWithAgency));
      when(contractRepository.findDistinctClientsByAgencyId(AGENCY_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      Page<ClientUserResponse> result =
          service.listClientsByCallerAgency(PARTNER_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Échec – caller introuvable → CustomException USER_NOT_FOUND")
    void listClientsByCallerAgency_callerNotFound_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.listClientsByCallerAgency(PARTNER_KC_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – caller sans agence → CustomException (BadRequest)")
    void listClientsByCallerAgency_callerHasNoAgency_throwsCustomException() {
      User userWithoutAgency =
          SharedTestFixtures.buildUserWithoutAgency(PARTNER_KC_ID, UUID.randomUUID());
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(userWithoutAgency));

      assertThatThrownBy(() -> service.listClientsByCallerAgency(PARTNER_KC_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining("agence");
    }
  }

  // ── listClientsByCallerHotel ───────────────────────────────────────────────

  @Nested
  @DisplayName("listClientsByCallerHotel()")
  class ListClientsByCallerHotel {

    @Test
    @DisplayName("Succès – caller lié à un hôtel, clients retournés")
    void listClientsByCallerHotel_callerHasHotel_returnsClients() throws CustomException {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(partnerWithHotel));
      when(reservationRepository.findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(clientUser)));

      Page<ClientUserResponse> result =
          service.listClientsByCallerHotel(PARTNER_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().email()).isEqualTo("aya@test.ci");
      verify(reservationRepository).findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged());
    }

    @Test
    @DisplayName("Succès – caller avec hôtel, aucune réservation → page vide")
    void listClientsByCallerHotel_noClients_returnsEmptyPage() throws CustomException {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(partnerWithHotel));
      when(reservationRepository.findDistinctClientsByHotelId(HOTEL_ID, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      Page<ClientUserResponse> result =
          service.listClientsByCallerHotel(PARTNER_KC_ID, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Échec – caller introuvable → CustomException USER_NOT_FOUND")
    void listClientsByCallerHotel_callerNotFound_throwsCustomException() {
      when(userRepository.findByKeycloakId(PARTNER_KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.listClientsByCallerHotel(PARTNER_KC_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – caller sans hôtel → CustomException (BadRequest)")
    void listClientsByCallerHotel_callerHasNoHotel_throwsCustomException() {
      User userWithoutHotel =
          SharedTestFixtures.buildPartnerUser(PARTNER_KC_ID, UUID.randomUUID(), agency);
      when(userRepository.findByKeycloakId(PARTNER_KC_ID))
          .thenReturn(Optional.of(userWithoutHotel));

      assertThatThrownBy(() -> service.listClientsByCallerHotel(PARTNER_KC_ID, Pageable.unpaged()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining("hôtel");
    }
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private User buildExtraClient(String firstName, String lastName, String email) {
    User u =
        User.builder()
            .keycloakId("kc-" + UUID.randomUUID())
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .phone("+2250700000000")
            .roles(new HashSet<>(Set.of(UserRole.CLIENT)))
            .build();
    SharedTestFixtures.injectId(u, UUID.randomUUID());
    return u;
  }
}
