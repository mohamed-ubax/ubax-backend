package com.africa.ubaxplatform.unit.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import com.africa.ubaxplatform.reservation.dto.CancelReservationRequest;
import com.africa.ubaxplatform.reservation.dto.CreateReservationRequest;
import com.africa.ubaxplatform.reservation.dto.ReservationResponse;
import com.africa.ubaxplatform.reservation.entity.Reservation;
import com.africa.ubaxplatform.reservation.repository.ReservationRepository;
import com.africa.ubaxplatform.reservation.service.impl.ReservationServiceImpl;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("ReservationServiceImpl – tests unitaires")
class ReservationServiceImplTest {

  @Mock private ReservationRepository reservationRepo;
  @Mock private UserRepository userRepo;
  @Mock private PropertyRepository propertyRepo;

  @InjectMocks private ReservationServiceImpl service;

  private static final UUID RESERVATION_ID = UUID.randomUUID();
  private static final UUID HOTEL_ID = UUID.randomUUID();

  private Agency agency;
  private User client;
  private Hotel hotel;
  private User hotelStaff;
  private Property hotelProperty;
  private Reservation pendingReservation;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    client =
        SharedTestFixtures.buildUserWithoutAgency(
            SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);

    hotel = Hotel.builder().name("Grand Hôtel Abidjan").build();
    SharedTestFixtures.injectId(hotel, HOTEL_ID);

    hotelStaff = SharedTestFixtures.buildPartnerUser("hotel-kc", UUID.randomUUID(), null);
    hotelStaff.setHotel(hotel);

    hotelProperty =
        Property.builder()
            .owner(hotelStaff)
            .hotel(hotel)
            .title("Chambre Deluxe")
            .propertyType("HOTEL_ROOM")
            .transactionType("SHORT_STAY")
            .price(BigDecimal.valueOf(50_000))
            .status(PropertyStatus.PUBLISHED)
            .city("Abidjan")
            .build();
    SharedTestFixtures.injectId(hotelProperty, SharedTestFixtures.PROPERTY_ID);

    pendingReservation =
        Reservation.builder()
            .property(hotelProperty)
            .client(client)
            .checkInDate(LocalDate.now().plusDays(5))
            .checkOutDate(LocalDate.now().plusDays(8))
            .numberOfNights(3)
            .guestCount(2)
            .pricePerNight(BigDecimal.valueOf(50_000))
            .totalAmount(BigDecimal.valueOf(150_000))
            .status(ReservationStatus.PENDING)
            .build();
    SharedTestFixtures.injectId(pendingReservation, RESERVATION_ID);
  }

  private CreateReservationRequest buildCreateRequest() {
    return new CreateReservationRequest(
        SharedTestFixtures.PROPERTY_ID,
        LocalDate.now().plusDays(5),
        LocalDate.now().plusDays(8),
        2,
        "Chambre calme si possible");
  }

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – réservation créée pour un bien PUBLISHED")
    void create_publishedProperty_success() throws CustomException {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(hotelProperty));
      when(reservationRepo.countOverlappingConfirmed(any(), any(), any())).thenReturn(0L);
      when(reservationRepo.save(any(Reservation.class)))
          .thenAnswer(
              inv -> {
                Reservation r = inv.getArgument(0);
                SharedTestFixtures.injectId(r, RESERVATION_ID);
                return r;
              });

      ReservationResponse resp =
          service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest());

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(ReservationStatus.PENDING);
      assertThat(resp.numberOfNights()).isEqualTo(3);
    }

    @Test
    @DisplayName(
        "Echec – date de départ avant date d'arrivée → RESERVATION_CREATE_FAILURE_BAD_REQUEST")
    void create_checkoutBeforeCheckin_throwsBadRequest() {
      CreateReservationRequest req =
          new CreateReservationRequest(
              SharedTestFixtures.PROPERTY_ID,
              LocalDate.now().plusDays(8),
              LocalDate.now().plusDays(5),
              2,
              null);

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.RESERVATION_CREATE_FAILURE_BAD_REQUEST);
    }

    @Test
    @DisplayName("Echec – même date arrivée et départ → RESERVATION_CREATE_FAILURE_BAD_REQUEST")
    void create_sameDates_throwsBadRequest() {
      LocalDate same = LocalDate.now().plusDays(5);
      CreateReservationRequest req =
          new CreateReservationRequest(SharedTestFixtures.PROPERTY_ID, same, same, 2, null);

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.RESERVATION_CREATE_FAILURE_BAD_REQUEST);
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void create_userNotFound_throwsCustomException() {
      CreateReservationRequest req = buildCreateRequest();
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bien introuvable → PROPERTY_GET_FAILURE_NOT_FOUND")
    void create_propertyNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bien non PUBLISHED → RESERVATION_CREATE_FAILURE_BAD_REQUEST")
    void create_propertyNotPublished_throwsBadRequest() {
      Property draftProp =
          Property.builder()
              .owner(hotelStaff)
              .title("Chambre brouillon")
              .propertyType("HOTEL_ROOM")
              .transactionType("RENT")
              .price(BigDecimal.valueOf(50_000))
              .status(PropertyStatus.DRAFT)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(draftProp, SharedTestFixtures.PROPERTY_ID);

      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(draftProp));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.RESERVATION_CREATE_FAILURE_BAD_REQUEST);
    }

    @Test
    @DisplayName(
        "Echec – chevauchement avec réservation confirmée → RESERVATION_CREATE_FAILURE_UNAVAILABLE")
    void create_overlappingDates_throwsBadRequest() {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(hotelProperty));
      when(reservationRepo.countOverlappingConfirmed(any(), any(), any())).thenReturn(1L);

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.RESERVATION_CREATE_FAILURE_UNAVAILABLE);
    }
  }

  @Nested
  @DisplayName("getMyReservations()")
  class GetMyReservations {

    @Test
    @DisplayName("Succès – liste paginée des réservations du client")
    void getMyReservations_success() throws CustomException {
      Page<Reservation> page = new PageImpl<>(java.util.List.of(pendingReservation));
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(reservationRepo.findByClientIdAndDeletedAtIsNull(
              SharedTestFixtures.USER_ID, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.getMyReservations(SharedTestFixtures.KEYCLOAK_ID, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void getMyReservations_userNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.getMyReservations(SharedTestFixtures.KEYCLOAK_ID, PageRequest.of(0, 10)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – accès accordé au client propriétaire")
    void getById_client_success() throws CustomException {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      ReservationResponse resp = service.getById(SharedTestFixtures.KEYCLOAK_ID, RESERVATION_ID);

      assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Succès – accès accordé à l'hôtelier propriétaire du bien")
    void getById_hotelOwner_success() throws CustomException {
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      ReservationResponse resp = service.getById("hotel-kc", RESERVATION_ID);

      assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Echec – accès refusé (ni client ni hôtelier) → USER_FORBIDDEN")
    void getById_unauthorized_throwsForbidden() {
      User stranger = SharedTestFixtures.buildUserWithoutAgency("stranger-kc", UUID.randomUUID());
      when(userRepo.findByKeycloakIdWithHotel("stranger-kc")).thenReturn(Optional.of(stranger));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.getById("stranger-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_FORBIDDEN);
    }

    @Test
    @DisplayName("Echec – réservation introuvable → RESERVATION_GET_FAILURE_NOT_FOUND")
    void getById_notFound_throwsCustomException() {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(SharedTestFixtures.KEYCLOAK_ID, RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.RESERVATION_GET_FAILURE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("cancelByClient()")
  class CancelByClient {

    @Test
    @DisplayName("Succès – client annule une réservation PENDING")
    void cancelByClient_pending_success() throws CustomException {
      CancelReservationRequest req = new CancelReservationRequest("Changement de plans");
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(pendingReservation);

      ReservationResponse resp =
          service.cancelByClient(SharedTestFixtures.KEYCLOAK_ID, RESERVATION_ID, req);

      assertThat(resp.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("Echec – réservation CONFIRMED ne peut pas être annulée par le client")
    void cancelByClient_confirmed_throwsBadRequest() {
      Reservation confirmed =
          Reservation.builder()
              .property(hotelProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.CONFIRMED)
              .build();
      SharedTestFixtures.injectId(confirmed, RESERVATION_ID);

      CancelReservationRequest req = new CancelReservationRequest("Trop tard");
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.of(confirmed));

      assertThatThrownBy(
              () -> service.cancelByClient(SharedTestFixtures.KEYCLOAK_ID, RESERVATION_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("Echec – réservation appartenant à un autre client → USER_FORBIDDEN")
    void cancelByClient_notOwner_throwsForbidden() {
      User otherClient = SharedTestFixtures.buildUserWithoutAgency("other-kc", UUID.randomUUID());
      CancelReservationRequest req = new CancelReservationRequest("Test");
      when(userRepo.findByKeycloakIdWithHotel("other-kc")).thenReturn(Optional.of(otherClient));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.cancelByClient("other-kc", RESERVATION_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  @Nested
  @DisplayName("getHotelReservations()")
  class GetHotelReservations {

    @Test
    @DisplayName("Succès – liste paginée sans filtre de statut")
    void getHotelReservations_noFilter_success() throws CustomException {
      Page<Reservation> page = new PageImpl<>(java.util.List.of(pendingReservation));
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByHotelId(HOTEL_ID, PageRequest.of(0, 10))).thenReturn(page);

      var result = service.getHotelReservations("hotel-kc", null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – liste filtrée par statut PENDING")
    void getHotelReservations_withStatus_success() throws CustomException {
      Page<Reservation> page = new PageImpl<>(java.util.List.of(pendingReservation));
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByHotelIdAndStatus(
              HOTEL_ID, ReservationStatus.PENDING, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result =
          service.getHotelReservations(
              "hotel-kc", ReservationStatus.PENDING, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Echec – utilisateur non hôtelier → USER_UNAUTHORIZED")
    void getHotelReservations_notHotelPartner_throwsUnauthorized() {
      when(userRepo.findByKeycloakIdWithHotel(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(client));

      assertThatThrownBy(
              () ->
                  service.getHotelReservations(
                      SharedTestFixtures.KEYCLOAK_ID, null, PageRequest.of(0, 10)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("confirm()")
  class Confirm {

    @Test
    @DisplayName("Succès – PENDING → CONFIRMED")
    void confirm_pending_success() throws CustomException {
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(pendingReservation);

      ReservationResponse resp = service.confirm("hotel-kc", RESERVATION_ID);

      assertThat(resp.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName(
        "Succès – bien sans hotel_id direct (pré-V044) : fallback sur property.owner.hotel")
    void confirm_propertyHotelNullFallbackToOwnerHotel_success() throws CustomException {
      // Bien sans hotel_id renseigné (ancien schéma pré-V044) mais owner rattaché à l'hôtel
      Property legacyProperty =
          Property.builder()
              .owner(hotelStaff) // owner.hotel = hotel → fallback disponible
              .hotel(null) // ← hotel_id non renseigné
              .title("Chambre legacy")
              .propertyType("HOTEL_ROOM")
              .transactionType("SHORT_STAY")
              .price(BigDecimal.valueOf(50_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(legacyProperty, SharedTestFixtures.PROPERTY_ID);

      Reservation legacyReservation =
          Reservation.builder()
              .property(legacyProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.PENDING)
              .build();
      SharedTestFixtures.injectId(legacyReservation, RESERVATION_ID);

      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(legacyReservation));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(legacyReservation);

      ReservationResponse resp = service.confirm("hotel-kc", RESERVATION_ID);

      assertThat(resp.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Echec – bien sans hotel_id ET owner sans hôtel → USER_FORBIDDEN")
    void confirm_propertyHotelNullOwnerHotelNull_throwsForbidden() {
      User ownerWithoutHotel =
          SharedTestFixtures.buildUserWithoutAgency("owner-kc", UUID.randomUUID());
      // ownerWithoutHotel.hotel = null

      Property orphanProperty =
          Property.builder()
              .owner(ownerWithoutHotel)
              .hotel(null) // aucun hotel rattaché
              .title("Bien orphelin")
              .propertyType("HOTEL_ROOM")
              .transactionType("SHORT_STAY")
              .price(BigDecimal.valueOf(50_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(orphanProperty, SharedTestFixtures.PROPERTY_ID);

      Reservation orphanReservation =
          Reservation.builder()
              .property(orphanProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.PENDING)
              .build();
      SharedTestFixtures.injectId(orphanReservation, RESERVATION_ID);

      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(orphanReservation));

      assertThatThrownBy(() -> service.confirm("hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_FORBIDDEN);
    }

    @Test
    @DisplayName(
        "Succès – hotel_id direct ≠ caller mais owner.hotel = caller (logique OR) → CONFIRMED")
    void confirm_propertyHotelMismatchOwnerHotelMatches_success() throws CustomException {
      // Scénario du bug : property.hotel_id pointe sur un hôtel différent (ex. migration V062
      // incohérente) mais property.owner.hotel correspond bien au gérant qui confirme.
      Hotel otherHotel = Hotel.builder().name("Autre hôtel").build();
      SharedTestFixtures.injectId(otherHotel, UUID.randomUUID()); // ID différent de HOTEL_ID

      Property mismatchProperty =
          Property.builder()
              .owner(hotelStaff) // owner.hotel = hotel (HOTEL_ID) → chemin owner valide
              .hotel(otherHotel) // hotel_id direct ≠ HOTEL_ID → chemin direct invalide
              .title("Chambre incohérente")
              .propertyType("HOTEL_ROOM")
              .transactionType("SHORT_STAY")
              .price(BigDecimal.valueOf(50_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(mismatchProperty, SharedTestFixtures.PROPERTY_ID);

      Reservation mismatchReservation =
          Reservation.builder()
              .property(mismatchProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.PENDING)
              .build();
      SharedTestFixtures.injectId(mismatchReservation, RESERVATION_ID);

      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(mismatchReservation));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(mismatchReservation);

      ReservationResponse resp = service.confirm("hotel-kc", RESERVATION_ID);

      assertThat(resp.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Echec – caller sans hôtel → USER_UNAUTHORIZED")
    void confirm_callerWithoutHotel_throwsUnauthorized() {
      User noHotelUser =
          SharedTestFixtures.buildUserWithoutAgency("no-hotel-kc", UUID.randomUUID());
      when(userRepo.findByKeycloakIdWithHotel("no-hotel-kc")).thenReturn(Optional.of(noHotelUser));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.confirm("no-hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Echec – réservation non PENDING → transition invalide")
    void confirm_notPending_throwsBadRequest() {
      Reservation confirmed =
          Reservation.builder()
              .property(hotelProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.CONFIRMED)
              .build();
      SharedTestFixtures.injectId(confirmed, RESERVATION_ID);

      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.of(confirmed));

      assertThatThrownBy(() -> service.confirm("hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("cancelByHotel()")
  class CancelByHotel {

    @Test
    @DisplayName("Succès – annulation d'une réservation PENDING par l'hôtel")
    void cancelByHotel_pending_success() throws CustomException {
      CancelReservationRequest req = new CancelReservationRequest("Overbooking");
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(pendingReservation);

      ReservationResponse resp = service.cancelByHotel("hotel-kc", RESERVATION_ID, req);

      assertThat(resp.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("Succès – annulation d'une réservation CONFIRMED par l'hôtel")
    void cancelByHotel_confirmed_success() throws CustomException {
      Reservation confirmed =
          Reservation.builder()
              .property(hotelProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.CONFIRMED)
              .build();
      SharedTestFixtures.injectId(confirmed, RESERVATION_ID);

      CancelReservationRequest req = new CancelReservationRequest("Fermeture imprévue");
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.of(confirmed));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(confirmed);

      ReservationResponse resp = service.cancelByHotel("hotel-kc", RESERVATION_ID, req);

      assertThat(resp.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("Echec – caller sans hôtel → USER_UNAUTHORIZED")
    void cancelByHotel_callerWithoutHotel_throwsUnauthorized() {
      User noHotelUser =
          SharedTestFixtures.buildUserWithoutAgency("no-hotel-kc", UUID.randomUUID());
      CancelReservationRequest req = new CancelReservationRequest("Test");
      when(userRepo.findByKeycloakIdWithHotel("no-hotel-kc")).thenReturn(Optional.of(noHotelUser));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.cancelByHotel("no-hotel-kc", RESERVATION_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Echec – propriété appartenant à un autre hôtel → USER_FORBIDDEN")
    void cancelByHotel_wrongHotel_throwsForbidden() {
      Hotel otherHotel = Hotel.builder().name("Hôtel concurrent").build();
      SharedTestFixtures.injectId(otherHotel, UUID.randomUUID());
      User otherHotelStaff =
          SharedTestFixtures.buildPartnerUser("other-hotel-kc", UUID.randomUUID(), null);
      otherHotelStaff.setHotel(otherHotel);

      CancelReservationRequest req = new CancelReservationRequest("Test");
      when(userRepo.findByKeycloakIdWithHotel("other-hotel-kc"))
          .thenReturn(Optional.of(otherHotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.cancelByHotel("other-hotel-kc", RESERVATION_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_FORBIDDEN);
    }

    @Test
    @DisplayName("Echec – réservation COMPLETED ne peut pas être annulée")
    void cancelByHotel_completed_throwsBadRequest() {
      Reservation completed =
          Reservation.builder()
              .property(hotelProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.COMPLETED)
              .build();
      SharedTestFixtures.injectId(completed, RESERVATION_ID);

      CancelReservationRequest req = new CancelReservationRequest("Erreur");
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.of(completed));

      assertThatThrownBy(() -> service.cancelByHotel("hotel-kc", RESERVATION_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("complete()")
  class Complete {

    @Test
    @DisplayName("Succès – CONFIRMED → COMPLETED")
    void complete_confirmed_success() throws CustomException {
      Reservation confirmed =
          Reservation.builder()
              .property(hotelProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.CONFIRMED)
              .build();
      SharedTestFixtures.injectId(confirmed, RESERVATION_ID);

      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.of(confirmed));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(confirmed);

      ReservationResponse resp = service.complete("hotel-kc", RESERVATION_ID);

      assertThat(resp.status()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @DisplayName("Echec – caller sans hôtel → USER_UNAUTHORIZED")
    void complete_callerWithoutHotel_throwsUnauthorized() {
      User noHotelUser =
          SharedTestFixtures.buildUserWithoutAgency("no-hotel-kc", UUID.randomUUID());
      when(userRepo.findByKeycloakIdWithHotel("no-hotel-kc")).thenReturn(Optional.of(noHotelUser));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.complete("no-hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Echec – réservation PENDING → transition invalide")
    void complete_pending_throwsBadRequest() {
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.complete("hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("noShow()")
  class NoShow {

    @Test
    @DisplayName("Succès – CONFIRMED → NO_SHOW")
    void noShow_confirmed_success() throws CustomException {
      Reservation confirmed =
          Reservation.builder()
              .property(hotelProperty)
              .client(client)
              .checkInDate(LocalDate.now().plusDays(5))
              .checkOutDate(LocalDate.now().plusDays(8))
              .numberOfNights(3)
              .guestCount(2)
              .pricePerNight(BigDecimal.valueOf(50_000))
              .totalAmount(BigDecimal.valueOf(150_000))
              .status(ReservationStatus.CONFIRMED)
              .build();
      SharedTestFixtures.injectId(confirmed, RESERVATION_ID);

      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID)).thenReturn(Optional.of(confirmed));
      when(reservationRepo.save(any(Reservation.class))).thenReturn(confirmed);

      ReservationResponse resp = service.noShow("hotel-kc", RESERVATION_ID);

      assertThat(resp.status()).isEqualTo(ReservationStatus.NO_SHOW);
    }

    @Test
    @DisplayName("Echec – caller sans hôtel → USER_UNAUTHORIZED")
    void noShow_callerWithoutHotel_throwsUnauthorized() {
      User noHotelUser =
          SharedTestFixtures.buildUserWithoutAgency("no-hotel-kc", UUID.randomUUID());
      when(userRepo.findByKeycloakIdWithHotel("no-hotel-kc")).thenReturn(Optional.of(noHotelUser));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.noShow("no-hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Echec – réservation PENDING → transition invalide")
    void noShow_pending_throwsBadRequest() {
      when(userRepo.findByKeycloakIdWithHotel("hotel-kc")).thenReturn(Optional.of(hotelStaff));
      when(reservationRepo.findByIdWithDetails(RESERVATION_ID))
          .thenReturn(Optional.of(pendingReservation));

      assertThatThrownBy(() -> service.noShow("hotel-kc", RESERVATION_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("listAll()")
  class ListAll {

    @Test
    @DisplayName("Succès – liste sans filtre de statut (admin)")
    void listAll_noFilter_success() {
      Page<Reservation> page = new PageImpl<>(java.util.List.of(pendingReservation));
      when(reservationRepo.findByDeletedAtIsNull(PageRequest.of(0, 10))).thenReturn(page);

      var result = service.listAll(null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – liste filtrée par statut PENDING")
    void listAll_withStatus_success() {
      Page<Reservation> page = new PageImpl<>(java.util.List.of(pendingReservation));
      when(reservationRepo.findByStatusAndDeletedAtIsNull(
              ReservationStatus.PENDING, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.listAll(ReservationStatus.PENDING, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }
  }
}
