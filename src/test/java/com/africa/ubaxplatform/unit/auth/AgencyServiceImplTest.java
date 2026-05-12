package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.dto.AgencyResponse;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.service.impl.AgencyServiceImpl;
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
@DisplayName("AgencyServiceImpl")
class AgencyServiceImplTest {

  @Mock AgencyRepository agencyRepository;

  @InjectMocks AgencyServiceImpl service;

  private Agency buildActiveAgency(String name, String city) {
    Agency a =
        Agency.builder()
            .name(name)
            .city(city)
            .country("CI")
            .phone("+2250700000010")
            .email("contact@" + name.toLowerCase().replace(" ", "") + ".ci")
            .description("Agence spécialisée à " + city)
            .logoUrl("https://minio/agencies-logos/logo.webp")
            .address("Plateau, " + city)
            .website("https://www." + name.toLowerCase().replace(" ", "") + ".ci")
            .build();
    SharedTestFixtures.injectId(a, UUID.randomUUID());
    return a;
  }

  // listForClient

  @Nested
  @DisplayName("listForClient()")
  class ListForClient {

    @Test
    @DisplayName("Sans filtre ville – appelle findByActiveTrueAndDeletedAtIsNull")
    void listForClient_withoutCity_returnsAllActiveAgencies() {
      Agency a = buildActiveAgency("Agence Abidjan", "Abidjan");
      when(agencyRepository.findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(a)));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().name()).isEqualTo("Agence Abidjan");
      verify(agencyRepository).findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged());
      verify(agencyRepository, never())
          .findByActiveTrueAndDeletedAtIsNullAndCityIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("Avec filtre ville – appelle findByActiveTrueAndDeletedAtIsNullAndCityIgnoreCase")
    void listForClient_withCity_returnsFilteredAgencies() {
      Agency a = buildActiveAgency("Immo Dakar", "Dakar");
      when(agencyRepository.findByActiveTrueAndDeletedAtIsNullAndCityIgnoreCase(
              "Dakar", Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(a)));

      var result = service.listForClient("Dakar", Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().city()).isEqualTo("Dakar");
      verify(agencyRepository)
          .findByActiveTrueAndDeletedAtIsNullAndCityIgnoreCase("Dakar", Pageable.unpaged());
      verify(agencyRepository, never()).findByActiveTrueAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("Ville vide (blank) – traité comme absence de filtre")
    void listForClient_withBlankCity_treatedAsNoFilter() {
      when(agencyRepository.findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      service.listForClient("   ", Pageable.unpaged());

      verify(agencyRepository).findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged());
      verify(agencyRepository, never())
          .findByActiveTrueAndDeletedAtIsNullAndCityIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("Aucune agence active – retourne page vide sans exception")
    void listForClient_emptyResult_returnsEmptyPage() {
      when(agencyRepository.findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of()));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Mapping correct – tous les champs AgencyResponse remplis")
    void listForClient_mapsAllAgencyFields() {
      Agency a =
          Agency.builder()
              .name("Immo Prestige")
              .description("Spécialiste immobilier haut de gamme")
              .logoUrl("https://minio/agencies-logos/prestige.webp")
              .address("Zone 4, Abidjan")
              .city("Abidjan")
              .country("CI")
              .phone("+2250708080808")
              .email("contact@immoprestige.ci")
              .website("https://www.immoprestige.ci")
              .build();
      SharedTestFixtures.injectId(a, SharedTestFixtures.AGENCY_ID);

      when(agencyRepository.findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(a)));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(1);
      AgencyResponse r = result.getContent().getFirst();
      assertThat(r.id()).isEqualTo(SharedTestFixtures.AGENCY_ID);
      assertThat(r.name()).isEqualTo("Immo Prestige");
      assertThat(r.description()).isEqualTo("Spécialiste immobilier haut de gamme");
      assertThat(r.logoUrl()).isEqualTo("https://minio/agencies-logos/prestige.webp");
      assertThat(r.address()).isEqualTo("Zone 4, Abidjan");
      assertThat(r.city()).isEqualTo("Abidjan");
      assertThat(r.country()).isEqualTo("CI");
      assertThat(r.phone()).isEqualTo("+2250708080808");
      assertThat(r.email()).isEqualTo("contact@immoprestige.ci");
      assertThat(r.website()).isEqualTo("https://www.immoprestige.ci");
      assertThat(r.verified()).isFalse();
    }

    @Test
    @DisplayName("Plusieurs agences – toutes retournées dans la page")
    void listForClient_multipleAgencies_allReturned() {
      Agency a1 = buildActiveAgency("Agence Lomé", "Lomé");
      Agency a2 = buildActiveAgency("Agence Cotonou", "Cotonou");
      Agency a3 = buildActiveAgency("Agence Dakar", "Dakar");

      when(agencyRepository.findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged()))
          .thenReturn(new PageImpl<>(List.of(a1, a2, a3)));

      var result = service.listForClient(null, Pageable.unpaged());

      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getContent())
          .extracting(AgencyResponse::name)
          .containsExactlyInAnyOrder("Agence Lomé", "Agence Cotonou", "Agence Dakar");
    }
  }
}
