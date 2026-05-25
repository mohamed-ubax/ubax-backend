package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.dto.HotelResponse;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.service.impl.HotelServiceImpl;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.util.List;
import java.util.UUID;
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
@DisplayName("HotelServiceImpl")
class HotelServiceImplTest {

  @Mock HotelRepository hotelRepository;

  @InjectMocks HotelServiceImpl service;

  private Hotel buildActiveHotel(String name, String city) {
    Hotel h =
        Hotel.builder()
            .name(name)
            .description("Hôtel situé à " + city)
            .logoUrl("https://minio/hotels/logo.webp")
            .address("Centre Ville, " + city)
            .city(city)
            .country("CI")
            .phone("+2250700000010")
            .email("contact@" + name.toLowerCase().replace(" ", "") + ".ci")
            .website("https://www." + name.toLowerCase().replace(" ", "") + ".ci")
            .stars(4)
            .totalRooms(120)
            .build();
    SharedTestFixtures.injectId(h, UUID.randomUUID());
    return h;
  }

  // ── listForClient ────────────────────────────────────────────────

  @Nested
  @DisplayName("listForClient()")
  class ListForClient {

    @Test
    @DisplayName("Sans filtre ville – appelle findByActiveAndDeletedAtIsNull")
    void listForClient_withoutCity_returnsAllActiveHotels() {
      Hotel h = buildActiveHotel("Hotel Abidjan", "Abidjan");
      when(hotelRepository.findByActiveAndDeletedAtIsNull(true, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(h)));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().name()).isEqualTo("Hotel Abidjan");
      verify(hotelRepository).findByActiveAndDeletedAtIsNull(true, Pageable.unpaged());
      verify(hotelRepository, never())
          .findByActiveAndDeletedAtIsNullAndCityIgnoreCase(
              any(Boolean.class), any(String.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Avec filtre ville – appelle findByActiveAndDeletedAtIsNullAndCityIgnoreCase")
    void listForClient_withCity_returnsFilteredHotels() {
      Hotel h = buildActiveHotel("Teranga Hotel", "Dakar");
      when(hotelRepository.findByActiveAndDeletedAtIsNullAndCityIgnoreCase(
              true, "Dakar", Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(h)));

      var result = service.listForClient("Dakar", Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().city()).isEqualTo("Dakar");
      verify(hotelRepository)
          .findByActiveAndDeletedAtIsNullAndCityIgnoreCase(true, "Dakar", Pageable.unpaged());
      verify(hotelRepository, never())
          .findByActiveAndDeletedAtIsNull(any(Boolean.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Ville vide (blank) – traité comme absence de filtre")
    void listForClient_withBlankCity_treatedAsNoFilter() {
      when(hotelRepository.findByActiveAndDeletedAtIsNull(true, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      service.listForClient("   ", Pageable.unpaged());

      verify(hotelRepository).findByActiveAndDeletedAtIsNull(true, Pageable.unpaged());
      verify(hotelRepository, never())
          .findByActiveAndDeletedAtIsNullAndCityIgnoreCase(
              any(Boolean.class), any(String.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Aucun hôtel actif – retourne page vide sans exception")
    void listForClient_emptyResult_returnsEmptyPage() {
      when(hotelRepository.findByActiveAndDeletedAtIsNull(true, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Mapping correct – tous les champs HotelResponse remplis")
    void listForClient_mapsAllHotelFields() {
      Hotel h =
          Hotel.builder()
              .name("Sofitel Abidjan")
              .description("Hôtel 5 étoiles au cœur d'Abidjan")
              .logoUrl("https://minio/hotels/sofitel.webp")
              .address("Avenue Botreau-Roussel, Plateau")
              .city("Abidjan")
              .country("CI")
              .phone("+2252022000000")
              .email("contact@sofitel-abidjan.ci")
              .website("https://www.sofitel-abidjan.com")
              .stars(5)
              .totalRooms(250)
              .build();
      SharedTestFixtures.injectId(h, SharedTestFixtures.HOTEL_ID);

      when(hotelRepository.findByActiveAndDeletedAtIsNull(true, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(h)));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      HotelResponse r = result.getContent().getFirst();
      assertThat(r.id()).isEqualTo(SharedTestFixtures.HOTEL_ID);
      assertThat(r.name()).isEqualTo("Sofitel Abidjan");
      assertThat(r.description()).isEqualTo("Hôtel 5 étoiles au cœur d'Abidjan");
      assertThat(r.logoUrl()).isEqualTo("https://minio/hotels/sofitel.webp");
      assertThat(r.address()).isEqualTo("Avenue Botreau-Roussel, Plateau");
      assertThat(r.city()).isEqualTo("Abidjan");
      assertThat(r.country()).isEqualTo("CI");
      assertThat(r.phone()).isEqualTo("+2252022000000");
      assertThat(r.email()).isEqualTo("contact@sofitel-abidjan.ci");
      assertThat(r.website()).isEqualTo("https://www.sofitel-abidjan.com");
      assertThat(r.stars()).isEqualTo(5);
      assertThat(r.totalRooms()).isEqualTo(250);
      assertThat(r.verified()).isFalse();
    }

    @Test
    @DisplayName("Plusieurs hôtels – tous retournés dans la page")
    void listForClient_multipleHotels_allReturned() {
      Hotel h1 = buildActiveHotel("Hotel Lomé", "Lomé");
      Hotel h2 = buildActiveHotel("Hotel Cotonou", "Cotonou");
      Hotel h3 = buildActiveHotel("Hotel Dakar", "Dakar");

      when(hotelRepository.findByActiveAndDeletedAtIsNull(true, Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(h1, h2, h3)));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getContent())
          .extracting(HotelResponse::name)
          .containsExactlyInAnyOrder("Hotel Lomé", "Hotel Cotonou", "Hotel Dakar");
    }
  }
}
