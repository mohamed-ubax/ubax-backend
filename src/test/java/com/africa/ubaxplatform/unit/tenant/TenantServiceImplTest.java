package com.africa.ubaxplatform.unit.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.dto.TenantCreateRequest;
import com.africa.ubaxplatform.tenant.dto.TenantResponse;
import com.africa.ubaxplatform.tenant.dto.TenantUpdateRequest;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import com.africa.ubaxplatform.tenant.repository.TenantRepository;
import com.africa.ubaxplatform.tenant.service.impl.TenantServiceImpl;
import com.africa.ubaxplatform.testHelper.TenantTestFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl – tests unitaires")
class TenantServiceImplTest {

  @Mock private TenantRepository tenantRepo;
  @Mock private UserRepository userRepo;

  @InjectMocks private TenantServiceImpl service;

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – dossier créé avec statut INCOMPLETE")
    void create_success_returnsTenantWithIncompleteStatus() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      TenantCreateRequest req = TenantTestFixtures.buildCreateRequest();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.existsByUserId(TenantTestFixtures.USER_ID)).thenReturn(false);
      when(tenantRepo.save(any(Tenant.class)))
          .thenAnswer(
              inv -> {
                Tenant t = inv.getArgument(0);
                TenantTestFixtures.injectId(t, TenantTestFixtures.TENANT_ID);
                return t;
              });

      TenantResponse response = service.create(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.status()).isEqualTo(TenantStatus.INCOMPLETE);
      assertThat(response.employmentStatus()).isEqualTo("EMPLOYEE");
      assertThat(response.monthlyIncome()).isEqualByComparingTo(BigDecimal.valueOf(500_000));

      ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
      verify(tenantRepo).save(captor.capture());
      assertThat(captor.getValue().getUser()).isEqualTo(user);
      assertThat(captor.getValue().isHasGuarantor()).isFalse();
    }

    @Test
    @DisplayName("Succès – dossier avec garant, champs garant persistés")
    void create_withGuarantor_persistsGuarantorFields() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      TenantCreateRequest req = TenantTestFixtures.buildCreateRequestWithGuarantor();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.existsByUserId(TenantTestFixtures.USER_ID)).thenReturn(false);
      when(tenantRepo.save(any(Tenant.class)))
          .thenAnswer(
              inv -> {
                Tenant t = inv.getArgument(0);
                TenantTestFixtures.injectId(t, TenantTestFixtures.TENANT_ID);
                return t;
              });

      TenantResponse response = service.create(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.hasGuarantor()).isTrue();
      assertThat(response.guarantorName()).isEqualTo("Papa Koné");
      assertThat(response.guarantorPhone()).isEqualTo("+2250722334455");
    }

    @Test
    @DisplayName("Succès – dossier créé avec documents → statut PENDING_REVIEW immédiat")
    void create_withDocuments_transitionsToPendingReview() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      TenantCreateRequest req = TenantTestFixtures.buildCreateRequestWithDocuments();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.existsByUserId(TenantTestFixtures.USER_ID)).thenReturn(false);
      when(tenantRepo.save(any(Tenant.class)))
          .thenAnswer(
              inv -> {
                Tenant t = inv.getArgument(0);
                TenantTestFixtures.injectId(t, TenantTestFixtures.TENANT_ID);
                return t;
              });

      TenantResponse response = service.create(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.status()).isEqualTo(TenantStatus.PENDING_REVIEW);
      assertThat(response.idDocumentUrl()).isEqualTo("https://minio/tenant-documents/id-card.pdf");
      assertThat(response.idDocumentNumber()).isEqualTo("CI-2024-001");
    }

    @Test
    @DisplayName("Échec – utilisateur introuvable → CustomException USER_NOT_FOUND")
    void create_userNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.create(
                      TenantTestFixtures.KEYCLOAK_ID, TenantTestFixtures.buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);

      verify(tenantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – dossier déjà existant pour cet utilisateur → CustomException")
    void create_tenantAlreadyExists_throwsCustomException() {
      User user = TenantTestFixtures.buildUser();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.existsByUserId(TenantTestFixtures.USER_ID)).thenReturn(true);

      assertThatThrownBy(
              () ->
                  service.create(
                      TenantTestFixtures.KEYCLOAK_ID, TenantTestFixtures.buildCreateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_CREATE_FAILURE);

      verify(tenantRepo, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getMyProfile()")
  class GetMyProfile {

    @Test
    @DisplayName("Succès – retourne le profil du locataire connecté")
    void getMyProfile_success_returnsTenantResponse() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.of(tenant));

      TenantResponse response = service.getMyProfile(TenantTestFixtures.KEYCLOAK_ID);

      assertThat(response.status()).isEqualTo(TenantStatus.INCOMPLETE);
      assertThat(response.email()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Échec – aucun dossier trouvé → CustomException TENANT_GET_FAILURE_NOT_FOUND")
    void getMyProfile_notFound_throwsCustomException() {
      User user = TenantTestFixtures.buildUser();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getMyProfile(TenantTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – dossier soft-deleted invisible → CustomException")
    void getMyProfile_softDeleted_throwsCustomException() {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);
      tenant.setDeletedAt(java.time.LocalDateTime.now().minusDays(1));

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.of(tenant));

      assertThatThrownBy(() -> service.getMyProfile(TenantTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateMyProfile()")
  class UpdateMyProfile {

    @Test
    @DisplayName("Succès – mise à jour partielle des champs professionnels")
    void updateMyProfile_partialUpdate_updatesFields() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);
      TenantUpdateRequest req = TenantTestFixtures.buildPartialUpdateRequest();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response = service.updateMyProfile(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.employmentStatus()).isEqualTo("SELF_EMPLOYED");
      assertThat(response.employerName()).isEqualTo("Mon Entreprise");
      assertThat(response.monthlyIncome()).isEqualByComparingTo(BigDecimal.valueOf(750_000));
      assertThat(response.status()).isEqualTo(TenantStatus.INCOMPLETE);
    }

    @Test
    @DisplayName("Succès – dossier complété → statut passe automatiquement à PENDING_REVIEW")
    void updateMyProfile_completeDossier_transitionsToPendingReview() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);
      TenantUpdateRequest req = TenantTestFixtures.buildCompleteUpdateRequest();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response = service.updateMyProfile(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.status()).isEqualTo(TenantStatus.PENDING_REVIEW);
      assertThat(response.idDocumentUrl()).isEqualTo("https://minio/tenant-documents/id-card.pdf");
    }

    @Test
    @DisplayName("Succès – dossier déjà PENDING_REVIEW, statut inchangé après mise à jour")
    void updateMyProfile_alreadyPendingReview_statusUnchanged() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, user);
      TenantUpdateRequest req = TenantTestFixtures.buildPartialUpdateRequest();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response = service.updateMyProfile(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.status()).isEqualTo(TenantStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Succès – dossier REJECTED complété → repasse automatiquement à PENDING_REVIEW")
    void updateMyProfile_rejectedDossierCompleted_transitionsToPendingReview()
        throws CustomException {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(TenantTestFixtures.TENANT_ID, TenantStatus.REJECTED, user);
      TenantUpdateRequest req = TenantTestFixtures.buildCompleteUpdateRequest();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response = service.updateMyProfile(TenantTestFixtures.KEYCLOAK_ID, req);

      assertThat(response.status()).isEqualTo(TenantStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Échec – dossier introuvable → CustomException TENANT_UPDATE_FAILURE_NOT_FOUND")
    void updateMyProfile_notFound_throwsCustomException() {
      User user = TenantTestFixtures.buildUser();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.of(user));
      when(tenantRepo.findByUserId(TenantTestFixtures.USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.updateMyProfile(
                      TenantTestFixtures.KEYCLOAK_ID,
                      TenantTestFixtures.buildPartialUpdateRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_UPDATE_FAILURE_NOT_FOUND);

      verify(tenantRepo, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – retourne le dossier par ID")
    void getById_found_returnsTenantResponse() throws CustomException {
      User caller = TenantTestFixtures.buildUser();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.QUALIFIED, tenantUser);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

      TenantResponse response =
          service.getById(TenantTestFixtures.KEYCLOAK_ID, TenantTestFixtures.TENANT_ID);

      assertThat(response.status()).isEqualTo(TenantStatus.QUALIFIED);
      assertThat(response.id()).isEqualTo(TenantTestFixtures.TENANT_ID);
    }

    @Test
    @DisplayName("Échec – ID inexistant → CustomException TENANT_GET_FAILURE_NOT_FOUND")
    void getById_notFound_throwsCustomException() {
      User caller = TenantTestFixtures.buildUser();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.getById(TenantTestFixtures.KEYCLOAK_ID, TenantTestFixtures.TENANT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – dossier soft-deleted → CustomException")
    void getById_softDeleted_throwsCustomException() {
      User caller = TenantTestFixtures.buildUser();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, tenantUser);
      tenant.setDeletedAt(java.time.LocalDateTime.now().minusDays(1));

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

      assertThatThrownBy(
              () -> service.getById(TenantTestFixtures.KEYCLOAK_ID, TenantTestFixtures.TENANT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("list()")
  class ListTenants {

    @Test
    @DisplayName("Succès – avec filtre de statut")
    void list_withStatus_returnsFilteredPage() throws CustomException {
      User admin = TenantTestFixtures.buildUser();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);
      Pageable pageable = PageRequest.of(0, 10);
      Page<Tenant> page = new PageImpl<>(List.of(tenant));

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findByStatusAndDeletedAtIsNull(TenantStatus.PENDING_REVIEW, pageable))
          .thenReturn(page);

      Page<TenantResponse> result =
          service.list(TenantTestFixtures.KEYCLOAK_ID, TenantStatus.PENDING_REVIEW, null, pageable);

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().status()).isEqualTo(TenantStatus.PENDING_REVIEW);
      verify(tenantRepo).findByStatusAndDeletedAtIsNull(TenantStatus.PENDING_REVIEW, pageable);
      verify(tenantRepo, never()).findByDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("Admin – filtre par propertyId uniquement → findByPropertyId")
    void list_adminWithPropertyId_callsFindByPropertyId() throws CustomException {
      User admin = TenantTestFixtures.buildUser();
      Pageable pageable = PageRequest.of(0, 10);
      User u = TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Page<Tenant> page =
          new PageImpl<>(
              List.of(
                  TenantTestFixtures.buildCompleteTenant(
                      UUID.randomUUID(), TenantStatus.QUALIFIED, u)));

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findByPropertyId(TenantTestFixtures.PROPERTY_ID, pageable)).thenReturn(page);

      Page<TenantResponse> result =
          service.list(
              TenantTestFixtures.KEYCLOAK_ID, null, TenantTestFixtures.PROPERTY_ID, pageable);

      assertThat(result.getContent()).hasSize(1);
      verify(tenantRepo).findByPropertyId(TenantTestFixtures.PROPERTY_ID, pageable);
    }

    @Test
    @DisplayName("Admin – filtre par propertyId + status → findByPropertyIdAndStatus")
    void list_adminWithPropertyIdAndStatus_callsFindByPropertyIdAndStatus() throws CustomException {
      User admin = TenantTestFixtures.buildUser();
      Pageable pageable = PageRequest.of(0, 10);
      Page<Tenant> page = new PageImpl<>(List.of());

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findByPropertyIdAndStatus(
              TenantTestFixtures.PROPERTY_ID, TenantStatus.PENDING_REVIEW, pageable))
          .thenReturn(page);

      service.list(
          TenantTestFixtures.KEYCLOAK_ID,
          TenantStatus.PENDING_REVIEW,
          TenantTestFixtures.PROPERTY_ID,
          pageable);

      verify(tenantRepo)
          .findByPropertyIdAndStatus(
              TenantTestFixtures.PROPERTY_ID, TenantStatus.PENDING_REVIEW, pageable);
    }

    @Test
    @DisplayName("PARTNER agence – sans filtre → findByAgencyId")
    void list_partnerNoFilter_callsFindByAgencyId() throws CustomException {
      User partner = TenantTestFixtures.buildUserWithAgency();
      Pageable pageable = PageRequest.of(0, 10);
      Page<Tenant> page = new PageImpl<>(List.of());

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findByAgencyId(TenantTestFixtures.AGENCY_ID, pageable)).thenReturn(page);

      service.list(TenantTestFixtures.KEYCLOAK_ID, null, null, pageable);

      verify(tenantRepo).findByAgencyId(TenantTestFixtures.AGENCY_ID, pageable);
    }

    @Test
    @DisplayName("PARTNER agence – filtre status → findByAgencyIdAndStatus")
    void list_partnerWithStatus_callsFindByAgencyIdAndStatus() throws CustomException {
      User partner = TenantTestFixtures.buildUserWithAgency();
      Pageable pageable = PageRequest.of(0, 10);
      Page<Tenant> page = new PageImpl<>(List.of());

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findByAgencyIdAndStatus(
              TenantTestFixtures.AGENCY_ID, TenantStatus.PENDING_REVIEW, pageable))
          .thenReturn(page);

      service.list(TenantTestFixtures.KEYCLOAK_ID, TenantStatus.PENDING_REVIEW, null, pageable);

      verify(tenantRepo)
          .findByAgencyIdAndStatus(
              TenantTestFixtures.AGENCY_ID, TenantStatus.PENDING_REVIEW, pageable);
    }

    @Test
    @DisplayName("PARTNER agence – filtre propertyId → findByAgencyIdAndPropertyId")
    void list_partnerWithPropertyId_callsFindByAgencyIdAndPropertyId() throws CustomException {
      User partner = TenantTestFixtures.buildUserWithAgency();
      Pageable pageable = PageRequest.of(0, 10);
      Page<Tenant> page = new PageImpl<>(List.of());

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findByAgencyIdAndPropertyId(
              TenantTestFixtures.AGENCY_ID, TenantTestFixtures.PROPERTY_ID, pageable))
          .thenReturn(page);

      service.list(TenantTestFixtures.KEYCLOAK_ID, null, TenantTestFixtures.PROPERTY_ID, pageable);

      verify(tenantRepo)
          .findByAgencyIdAndPropertyId(
              TenantTestFixtures.AGENCY_ID, TenantTestFixtures.PROPERTY_ID, pageable);
    }

    @Test
    @DisplayName(
        "PARTNER agence – filtre propertyId + status → findByAgencyIdAndPropertyIdAndStatus")
    void list_partnerWithPropertyIdAndStatus_callsFindByAgencyIdAndPropertyIdAndStatus()
        throws CustomException {
      User partner = TenantTestFixtures.buildUserWithAgency();
      Pageable pageable = PageRequest.of(0, 10);
      Page<Tenant> page = new PageImpl<>(List.of());

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findByAgencyIdAndPropertyIdAndStatus(
              TenantTestFixtures.AGENCY_ID,
              TenantTestFixtures.PROPERTY_ID,
              TenantStatus.PENDING_REVIEW,
              pageable))
          .thenReturn(page);

      service.list(
          TenantTestFixtures.KEYCLOAK_ID,
          TenantStatus.PENDING_REVIEW,
          TenantTestFixtures.PROPERTY_ID,
          pageable);

      verify(tenantRepo)
          .findByAgencyIdAndPropertyIdAndStatus(
              TenantTestFixtures.AGENCY_ID,
              TenantTestFixtures.PROPERTY_ID,
              TenantStatus.PENDING_REVIEW,
              pageable);
    }

    @Test
    @DisplayName("PARTNER hôtel → UnAuthorizedException")
    void list_hotelPartner_throwsUnauthorized() {
      User hotelPartner = TenantTestFixtures.buildUserWithHotel();

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(hotelPartner));

      assertThatThrownBy(
              () -> service.list(TenantTestFixtures.KEYCLOAK_ID, null, null, PageRequest.of(0, 10)))
          .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    @DisplayName("Succès – sans filtre, retourne tous les dossiers non archivés")
    void list_withoutStatus_returnsAll() throws CustomException {
      User admin = TenantTestFixtures.buildUser();
      Pageable pag = PageRequest.of(0, 10);
      User u1 = TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      User u2 = TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Page<Tenant> page =
          new PageImpl<>(
              List.of(
                  TenantTestFixtures.buildTenant(UUID.randomUUID(), TenantStatus.INCOMPLETE, u1),
                  TenantTestFixtures.buildTenant(UUID.randomUUID(), TenantStatus.QUALIFIED, u2)));

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findByDeletedAtIsNull(pag)).thenReturn(page);

      Page<TenantResponse> result = service.list(TenantTestFixtures.KEYCLOAK_ID, null, null, pag);

      assertThat(result.getContent()).hasSize(2);
      verify(tenantRepo).findByDeletedAtIsNull(pag);
      verify(tenantRepo, never()).findByStatusAndDeletedAtIsNull(any(), any());
    }
  }

  @Nested
  @DisplayName("qualify()")
  class Qualify {

    @Test
    @DisplayName("Succès – dossier PENDING_REVIEW → QUALIFIED, champs mis à jour")
    void qualify_success_setsQualifiedStatus() throws CustomException {
      User reviewer = TenantTestFixtures.buildUser("reviewer-kc-id", UUID.randomUUID());
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, user);

      when(userRepo.findByKeycloakId("reviewer-kc-id")).thenReturn(Optional.of(reviewer));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response = service.qualify(TenantTestFixtures.TENANT_ID, "reviewer-kc-id");

      assertThat(response.status()).isEqualTo(TenantStatus.QUALIFIED);
      assertThat(response.qualified()).isTrue();
      assertThat(response.qualifiedAt()).isNotNull();

      ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
      verify(tenantRepo).save(captor.capture());
      assertThat(captor.getValue().getQualifiedBy()).isEqualTo(reviewer);
      assertThat(captor.getValue().getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("Succès – PARTNER de la bonne agence qualifie le dossier")
    void qualify_partnerCorrectAgency_success() throws CustomException {
      User partner = TenantTestFixtures.buildUserWithAgency();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.isLinkedToAgency(TenantTestFixtures.TENANT_ID, TenantTestFixtures.AGENCY_ID))
          .thenReturn(true);
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response =
          service.qualify(TenantTestFixtures.TENANT_ID, TenantTestFixtures.KEYCLOAK_ID);

      assertThat(response.status()).isEqualTo(TenantStatus.QUALIFIED);
      assertThat(response.qualified()).isTrue();
    }

    @Test
    @DisplayName("Échec – PARTNER d'une autre agence → UnAuthorizedException")
    void qualify_partnerWrongAgency_throwsUnauthorized() {
      User partner = TenantTestFixtures.buildUserWithAgency();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.isLinkedToAgency(TenantTestFixtures.TENANT_ID, TenantTestFixtures.AGENCY_ID))
          .thenReturn(false);

      assertThatThrownBy(
              () -> service.qualify(TenantTestFixtures.TENANT_ID, TenantTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(UnAuthorizedException.class);

      verify(tenantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – dossier non PENDING_REVIEW → CustomException TENANT_UPDATE_FAILURE")
    void qualify_wrongStatus_throwsCustomException() {
      User reviewer = TenantTestFixtures.buildUser("reviewer-kc-id", UUID.randomUUID());
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);

      when(userRepo.findByKeycloakId("reviewer-kc-id")).thenReturn(Optional.of(reviewer));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

      assertThatThrownBy(() -> service.qualify(TenantTestFixtures.TENANT_ID, "reviewer-kc-id"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_UPDATE_FAILURE);

      verify(tenantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – reviewer introuvable → CustomException USER_NOT_FOUND")
    void qualify_reviewerNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId("unknown-kc")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.qualify(TenantTestFixtures.TENANT_ID, "unknown-kc"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);

      verify(tenantRepo, never()).findById(any());
    }
  }

  @Nested
  @DisplayName("reject()")
  class Reject {

    @Test
    @DisplayName("Succès – admin rejette avec motif (sans vérification agence)")
    void reject_success_adminNoOwnershipCheck() throws CustomException {
      User admin = TenantTestFixtures.buildUser();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);
      String reason = "Revenus insuffisants par rapport au loyer demandé";

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response =
          service.reject(TenantTestFixtures.TENANT_ID, reason, TenantTestFixtures.KEYCLOAK_ID);

      assertThat(response.status()).isEqualTo(TenantStatus.REJECTED);
      assertThat(response.qualified()).isFalse();
      assertThat(response.rejectionReason()).isEqualTo(reason);
      verify(tenantRepo, never()).isLinkedToAgency(any(), any());
    }

    @Test
    @DisplayName("Succès – PARTNER de la bonne agence rejette le dossier")
    void reject_partnerCorrectAgency_success() throws CustomException {
      User partner = TenantTestFixtures.buildUserWithAgency();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);
      String reason = "Dossier incomplet";

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.isLinkedToAgency(TenantTestFixtures.TENANT_ID, TenantTestFixtures.AGENCY_ID))
          .thenReturn(true);
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      TenantResponse response =
          service.reject(TenantTestFixtures.TENANT_ID, reason, TenantTestFixtures.KEYCLOAK_ID);

      assertThat(response.status()).isEqualTo(TenantStatus.REJECTED);
      assertThat(response.rejectionReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("Échec – PARTNER d'une autre agence → UnAuthorizedException")
    void reject_partnerWrongAgency_throwsUnauthorized() {
      User partner = TenantTestFixtures.buildUserWithAgency();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildCompleteTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(partner));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.isLinkedToAgency(TenantTestFixtures.TENANT_ID, TenantTestFixtures.AGENCY_ID))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  service.reject(
                      TenantTestFixtures.TENANT_ID, "motif", TenantTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(UnAuthorizedException.class);

      verify(tenantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – motif null → CustomException TENANT_UPDATE_FAILURE")
    void reject_nullReason_throwsCustomException() {
      User admin = TenantTestFixtures.buildUser();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

      assertThatThrownBy(
              () ->
                  service.reject(
                      TenantTestFixtures.TENANT_ID, null, TenantTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_UPDATE_FAILURE);

      verify(tenantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – motif vide → CustomException TENANT_UPDATE_FAILURE")
    void reject_blankReason_throwsCustomException() {
      User admin = TenantTestFixtures.buildUser();
      User tenantUser =
          TenantTestFixtures.buildUser(UUID.randomUUID().toString(), UUID.randomUUID());
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.PENDING_REVIEW, tenantUser);

      when(userRepo.findByKeycloakId(TenantTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

      assertThatThrownBy(
              () ->
                  service.reject(
                      TenantTestFixtures.TENANT_ID, "   ", TenantTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_UPDATE_FAILURE);

      verify(tenantRepo, never()).save(any());
    }
  }

  @Nested
  @DisplayName("delete()")
  class Delete {

    @Test
    @DisplayName("Succès – soft delete : deletedAt renseigné, save appelé")
    void delete_success_setsDeletedAt() throws CustomException {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);

      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(tenantRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.delete(TenantTestFixtures.TENANT_ID);

      ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
      verify(tenantRepo).save(captor.capture());
      assertThat(captor.getValue().getDeletedAt()).isNotNull();
      assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Échec – dossier déjà supprimé → CustomException")
    void delete_alreadyDeleted_throwsCustomException() {
      User user = TenantTestFixtures.buildUser();
      Tenant tenant =
          TenantTestFixtures.buildTenant(
              TenantTestFixtures.TENANT_ID, TenantStatus.INCOMPLETE, user);
      tenant.setDeletedAt(java.time.LocalDateTime.now().minusDays(1));

      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));

      assertThatThrownBy(() -> service.delete(TenantTestFixtures.TENANT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND);

      verify(tenantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – ID inexistant → CustomException")
    void delete_notFound_throwsCustomException() {
      when(tenantRepo.findById(TenantTestFixtures.TENANT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(TenantTestFixtures.TENANT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND);
    }
  }
}
