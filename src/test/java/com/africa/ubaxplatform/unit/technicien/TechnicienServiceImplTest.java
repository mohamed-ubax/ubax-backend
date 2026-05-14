package com.africa.ubaxplatform.unit.technicien;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.technicien.dto.CreateTechnicienRequest;
import com.africa.ubaxplatform.technicien.dto.TechnicienResponse;
import com.africa.ubaxplatform.technicien.dto.UpdateTechnicienRequest;
import com.africa.ubaxplatform.technicien.entity.Technicien;
import com.africa.ubaxplatform.technicien.repository.TechnicienRepository;
import com.africa.ubaxplatform.technicien.service.impl.TechnicienServiceImpl;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
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
@DisplayName("TechnicienServiceImpl – tests unitaires")
class TechnicienServiceImplTest {

  @Mock private TechnicienRepository technicienRepo;
  @Mock private UserRepository userRepo;

  @InjectMocks private TechnicienServiceImpl service;

  private Agency agency;
  private Hotel hotel;
  private User agencyUser;
  private User hotelUser;
  private User noStructureUser;
  private Technicien agencyTech;
  private Technicien hotelTech;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    hotel = SharedTestFixtures.buildHotel();
    agencyUser = SharedTestFixtures.buildPartnerUser();
    hotelUser =
        SharedTestFixtures.buildHotelUser(
            SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID, hotel);
    noStructureUser =
        SharedTestFixtures.buildUserWithoutAgency(
            SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
    agencyTech = SharedTestFixtures.buildTechnicien(agency, null);
    hotelTech = SharedTestFixtures.buildTechnicien(null, hotel);
  }

  private CreateTechnicienRequest buildCreateRequest() {
    CreateTechnicienRequest req = new CreateTechnicienRequest();
    req.setFirstName("Mamadou");
    req.setLastName("Diallo");
    req.setPhone("+2250712345678");
    req.setProfession("PLOMBIER");
    req.setAddress("Cocody, Abidjan");
    return req;
  }

  // ── create() ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – technicien créé pour une agence")
    void create_withAgency_success() throws Exception {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.save(any(Technicien.class)))
          .thenAnswer(
              inv -> {
                Technicien t = inv.getArgument(0);
                SharedTestFixtures.injectId(t, SharedTestFixtures.TECHNICIEN_ID);
                return t;
              });

      TechnicienResponse resp =
          service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest());

      assertThat(resp).isNotNull();
      assertThat(resp.firstName()).isEqualTo("Mamadou");
      assertThat(resp.agencyId()).isEqualTo(SharedTestFixtures.AGENCY_ID);
      assertThat(resp.hotelId()).isNull();
      assertThat(resp.available()).isTrue();
    }

    @Test
    @DisplayName("Succès – technicien créé pour un hôtel")
    void create_withHotel_success() throws Exception {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(hotelUser));
      when(technicienRepo.save(any(Technicien.class)))
          .thenAnswer(
              inv -> {
                Technicien t = inv.getArgument(0);
                SharedTestFixtures.injectId(t, SharedTestFixtures.TECHNICIEN_ID);
                return t;
              });

      TechnicienResponse resp =
          service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest());

      assertThat(resp.hotelId()).isEqualTo(SharedTestFixtures.HOTEL_ID);
      assertThat(resp.agencyId()).isNull();
    }

    @Test
    @DisplayName("Echec – caller sans agence ni hôtel → CustomException(BadRequest)")
    void create_noStructure_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noStructureUser));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining("agence ou un hôtel");
    }

    @Test
    @DisplayName("Echec – caller introuvable → CustomException(UnAuthorized)")
    void create_callerNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasCauseInstanceOf(UnAuthorizedException.class);
    }
  }

  // ── list() ───────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("list()")
  class ListTechniciens {

    @Test
    @DisplayName("Succès – tous les techniciens de l'agence (sans filtre)")
    void list_agencyNoFilter_success() throws Exception {
      Page<Technicien> page = new PageImpl<>(List.of(agencyTech));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByAgencyIdAndDeletedAtIsNull(
              SharedTestFixtures.AGENCY_ID, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – filtre available=true pour agence")
    void list_agencyWithAvailableFilter_success() throws Exception {
      Page<Technicien> page = new PageImpl<>(List.of(agencyTech));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByAgencyIdAndAvailableAndDeletedAtIsNull(
              SharedTestFixtures.AGENCY_ID, true, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, true, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – tous les techniciens de l'hôtel (sans filtre)")
    void list_hotelNoFilter_success() throws Exception {
      Page<Technicien> page = new PageImpl<>(List.of(hotelTech));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(hotelUser));
      when(technicienRepo.findByHotelIdAndDeletedAtIsNull(
              SharedTestFixtures.HOTEL_ID, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – filtre available=false pour hôtel")
    void list_hotelWithAvailableFilter_success() throws Exception {
      Page<Technicien> page = new PageImpl<>(List.of());
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(hotelUser));
      when(technicienRepo.findByHotelIdAndAvailableAndDeletedAtIsNull(
              SharedTestFixtures.HOTEL_ID, false, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, false, PageRequest.of(0, 10));

      assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Echec – caller sans structure → CustomException")
    void list_noStructure_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noStructureUser));

      assertThatThrownBy(
              () -> service.list(SharedTestFixtures.KEYCLOAK_ID, null, PageRequest.of(0, 10)))
          .isInstanceOf(CustomException.class);
    }
  }

  // ── getById() ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – technicien de la même agence retourné")
    void getById_success() throws Exception {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(agencyTech));

      TechnicienResponse resp =
          service.getById(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID);

      assertThat(resp.id()).isEqualTo(SharedTestFixtures.TECHNICIEN_ID);
      assertThat(resp.profession()).isEqualTo("PLOMBIER");
    }

    @Test
    @DisplayName("Echec – technicien introuvable → CustomException(NotFoundException)")
    void getById_notFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.getById(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID))
          .isInstanceOf(CustomException.class)
          .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Echec – technicien appartient à une autre agence → CustomException(UnAuthorized)")
    void getById_differentAgency_throwsCustomException() {
      Agency otherAgency = Agency.builder().name("Autre Agence").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());
      Technicien otherTech = SharedTestFixtures.buildTechnicien(otherAgency, null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(otherTech));

      assertThatThrownBy(
              () ->
                  service.getById(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID))
          .isInstanceOf(CustomException.class)
          .hasCauseInstanceOf(UnAuthorizedException.class);
    }
  }

  // ── update() ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("update()")
  class Update {

    @Test
    @DisplayName("Succès – mise à jour partielle des champs")
    void update_partialFields_success() throws Exception {
      UpdateTechnicienRequest req = new UpdateTechnicienRequest();
      req.setPhone("+2250799999999");
      req.setProfession("ELECTRICIEN");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(agencyTech));
      when(technicienRepo.save(any(Technicien.class))).thenReturn(agencyTech);

      service.update(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID, req);

      assertThat(agencyTech.getPhone()).isEqualTo("+2250799999999");
      assertThat(agencyTech.getProfession()).isEqualTo("ELECTRICIEN");
      verify(technicienRepo).save(agencyTech);
    }

    @Test
    @DisplayName("Echec – technicien introuvable → CustomException(NotFoundException)")
    void update_notFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.update(
                      SharedTestFixtures.KEYCLOAK_ID,
                      SharedTestFixtures.TECHNICIEN_ID,
                      new UpdateTechnicienRequest()))
          .isInstanceOf(CustomException.class)
          .hasCauseInstanceOf(NotFoundException.class);
    }
  }

  // ── toggleAvailability() ─────────────────────────────────────────────────

  @Nested
  @DisplayName("toggleAvailability()")
  class ToggleAvailability {

    @Test
    @DisplayName("Succès – disponible=true → false")
    void toggle_trueToFalse_success() throws Exception {
      agencyTech.setAvailable(true);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(agencyTech));
      when(technicienRepo.save(any(Technicien.class))).thenReturn(agencyTech);

      service.toggleAvailability(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID);

      assertThat(agencyTech.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Succès – disponible=false → true")
    void toggle_falseToTrue_success() throws Exception {
      agencyTech.setAvailable(false);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(agencyTech));
      when(technicienRepo.save(any(Technicien.class))).thenReturn(agencyTech);

      service.toggleAvailability(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID);

      assertThat(agencyTech.isAvailable()).isTrue();
    }
  }

  // ── delete() ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("delete()")
  class Delete {

    @Test
    @DisplayName("Succès – soft delete : deletedAt renseigné")
    void delete_success_setsDeletedAt() throws Exception {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(agencyTech));
      when(technicienRepo.save(any(Technicien.class))).thenReturn(agencyTech);

      service.delete(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID);

      assertThat(agencyTech.getDeletedAt()).isNotNull();
      verify(technicienRepo).save(agencyTech);
    }

    @Test
    @DisplayName("Echec – technicien introuvable → CustomException(NotFoundException)")
    void delete_notFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(agencyUser));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.delete(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.TECHNICIEN_ID))
          .isInstanceOf(CustomException.class)
          .hasCauseInstanceOf(NotFoundException.class);
    }
  }
}
