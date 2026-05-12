package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.dto.AdminAgencyResponse;
import com.africa.ubaxplatform.auth.dto.AdminHotelResponse;
import com.africa.ubaxplatform.auth.dto.UpdateSubscriptionRequest;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.impl.AdminPartnerServiceImpl;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPartnerServiceImpl")
class AdminPartnerServiceImplTest {

  @Mock AgencyRepository agencyRepo;
  @Mock HotelRepository hotelRepo;
  @Mock UserRepository userRepo;

  @InjectMocks AdminPartnerServiceImpl service;

  private static final UUID HOTEL_ID = UUID.randomUUID();

  Agency agency;
  Hotel hotel;

  @BeforeEach
  void setUp() {
    agency = Agency.builder().name("Agence Test CI").city("Abidjan").build();
    agency.setActive(true);
    SharedTestFixtures.injectId(agency, SharedTestFixtures.AGENCY_ID);

    hotel = Hotel.builder().name("Hôtel Ivoire").city("Abidjan").stars(4).totalRooms(100).build();
    hotel.setActive(true);
    SharedTestFixtures.injectId(hotel, HOTEL_ID);
  }

  // Agences

  @Nested
  @DisplayName("listAgencies()")
  class ListAgencies {

    @Test
    @DisplayName("Retourne une page d'agences non supprimées")
    void listAgencies_returnsPage() {
      when(agencyRepo.findByDeletedAtIsNull(any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(agency)));

      Page<AdminAgencyResponse> result = service.listAgencies(Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().name()).isEqualTo("Agence Test CI");
    }
  }

  @Nested
  @DisplayName("suspendAgency()")
  class SuspendAgency {

    @Test
    @DisplayName("Succès – agence active suspendue")
    void suspendAgency_success() throws CustomException {
      when(agencyRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.AGENCY_ID))
          .thenReturn(Optional.of(agency));
      when(agencyRepo.save(agency)).thenReturn(agency);

      AdminAgencyResponse result = service.suspendAgency(SharedTestFixtures.AGENCY_ID);

      assertThat(result.active()).isFalse();
      verify(agencyRepo).save(agency);
    }

    @Test
    @DisplayName("Echec – agence déjà suspendue → PARTNER_ALREADY_SUSPENDED")
    void suspendAgency_alreadySuspended_throws() {
      agency.setActive(false);
      when(agencyRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.AGENCY_ID))
          .thenReturn(Optional.of(agency));

      assertThatThrownBy(() -> service.suspendAgency(SharedTestFixtures.AGENCY_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_ALREADY_SUSPENDED);
    }

    @Test
    @DisplayName("Echec – agence introuvable → CustomException")
    void suspendAgency_notFound_throws() {
      when(agencyRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.AGENCY_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.suspendAgency(SharedTestFixtures.AGENCY_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("activateAgency()")
  class ActivateAgency {

    @Test
    @DisplayName("Succès – agence suspendue réactivée")
    void activateAgency_success() throws CustomException {
      agency.setActive(false);
      when(agencyRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.AGENCY_ID))
          .thenReturn(Optional.of(agency));
      when(agencyRepo.save(agency)).thenReturn(agency);

      AdminAgencyResponse result = service.activateAgency(SharedTestFixtures.AGENCY_ID);

      assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("Echec – agence déjà active → PARTNER_ALREADY_ACTIVE")
    void activateAgency_alreadyActive_throws() {
      when(agencyRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.AGENCY_ID))
          .thenReturn(Optional.of(agency));

      assertThatThrownBy(() -> service.activateAgency(SharedTestFixtures.AGENCY_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_ALREADY_ACTIVE);
    }
  }

  @Nested
  @DisplayName("updateAgencySubscription()")
  class UpdateAgencySubscription {

    @Test
    @DisplayName("Succès – plan et date d'expiration mis à jour")
    void updateSubscription_success() throws CustomException {
      UpdateSubscriptionRequest req =
          new UpdateSubscriptionRequest("PREMIUM", LocalDateTime.now().plusYears(1));
      when(agencyRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.AGENCY_ID))
          .thenReturn(Optional.of(agency));
      when(agencyRepo.save(agency)).thenReturn(agency);

      AdminAgencyResponse result =
          service.updateAgencySubscription(SharedTestFixtures.AGENCY_ID, req);

      assertThat(result).isNotNull();
      assertThat(agency.getSubscriptionPlan()).isEqualTo("PREMIUM");
    }
  }

  // Hôtels

  @Nested
  @DisplayName("listHotels()")
  class ListHotels {

    @Test
    @DisplayName("Retourne une page d'hôtels non supprimés")
    void listHotels_returnsPage() {
      when(hotelRepo.findByDeletedAtIsNull(any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(hotel)));

      Page<AdminHotelResponse> result = service.listHotels(Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().name()).isEqualTo("Hôtel Ivoire");
    }
  }

  @Nested
  @DisplayName("suspendHotel()")
  class SuspendHotel {

    @Test
    @DisplayName("Succès – hôtel actif suspendu")
    void suspendHotel_success() throws CustomException {
      when(hotelRepo.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));
      when(hotelRepo.save(hotel)).thenReturn(hotel);

      AdminHotelResponse result = service.suspendHotel(HOTEL_ID);

      assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("Echec – hôtel déjà suspendu → PARTNER_ALREADY_SUSPENDED")
    void suspendHotel_alreadySuspended_throws() {
      hotel.setActive(false);
      when(hotelRepo.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));

      assertThatThrownBy(() -> service.suspendHotel(HOTEL_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_ALREADY_SUSPENDED);
    }

    @Test
    @DisplayName("Echec – hôtel introuvable → PARTNER_NOT_FOUND")
    void suspendHotel_notFound_throws() {
      when(hotelRepo.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.suspendHotel(HOTEL_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("activateHotel()")
  class ActivateHotel {

    @Test
    @DisplayName("Succès – hôtel suspendu réactivé")
    void activateHotel_success() throws CustomException {
      hotel.setActive(false);
      when(hotelRepo.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));
      when(hotelRepo.save(hotel)).thenReturn(hotel);

      AdminHotelResponse result = service.activateHotel(HOTEL_ID);

      assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("Echec – hôtel déjà actif → PARTNER_ALREADY_ACTIVE")
    void activateHotel_alreadyActive_throws() {
      when(hotelRepo.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));

      assertThatThrownBy(() -> service.activateHotel(HOTEL_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PARTNER_ALREADY_ACTIVE);
    }
  }

  @Nested
  @DisplayName("updateHotelSubscription()")
  class UpdateHotelSubscription {

    @Test
    @DisplayName("Succès – plan et expiration mis à jour")
    void updateSubscription_success() throws CustomException {
      UpdateSubscriptionRequest req =
          new UpdateSubscriptionRequest("PREMIUM", LocalDateTime.now().plusYears(1));
      when(hotelRepo.findByIdAndDeletedAtIsNull(HOTEL_ID)).thenReturn(Optional.of(hotel));
      when(hotelRepo.save(hotel)).thenReturn(hotel);

      AdminHotelResponse result = service.updateHotelSubscription(HOTEL_ID, req);

      assertThat(result).isNotNull();
      assertThat(hotel.getSubscriptionPlan()).isEqualTo("PREMIUM");
    }
  }

  // Clients

  @Nested
  @DisplayName("listClients()")
  class ListClients {

    @Test
    @DisplayName("Retourne une page de clients")
    void listClients_returnsPage() {
      User client = SharedTestFixtures.buildUserWithoutAgency("kc-client", UUID.randomUUID());
      when(userRepo.findByRoleAndDeletedAtIsNull(any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(client)));

      var result = service.listClients(Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
    }
  }
}
