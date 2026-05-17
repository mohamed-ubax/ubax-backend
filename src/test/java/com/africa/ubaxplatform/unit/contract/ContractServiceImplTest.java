package com.africa.ubaxplatform.unit.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.dto.ContractResponse;
import com.africa.ubaxplatform.contract.dto.CreateContractRequest;
import com.africa.ubaxplatform.contract.dto.TerminateContractRequest;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.contract.service.impl.ContractServiceImpl;
import com.africa.ubaxplatform.document.service.interfaces.DocumentService;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import com.africa.ubaxplatform.tenant.repository.TenantRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
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
@DisplayName("ContractServiceImpl – tests unitaires")
class ContractServiceImplTest {

  @Mock private ContractRepository contractRepo;
  @Mock private UserRepository userRepo;
  @Mock private PropertyRepository propertyRepo;
  @Mock private TenantRepository tenantRepo;
  @Mock private PaymentRepository paymentRepo;
  @Mock private DocumentService documentService;

  @InjectMocks private ContractServiceImpl service;

  private Agency agency;
  private User caller;
  private Property property;
  private Contract draftContract;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    caller = SharedTestFixtures.buildPartnerUser();

    property =
        Property.builder()
            .owner(caller)
            .agency(agency)
            .title("Appartement T3 Cocody")
            .propertyType("APARTMENT")
            .transactionType("RENT")
            .price(BigDecimal.valueOf(350_000))
            .status(PropertyStatus.PUBLISHED)
            .city("Abidjan")
            .build();
    SharedTestFixtures.injectId(property, SharedTestFixtures.PROPERTY_ID);

    draftContract =
        Contract.builder()
            .property(property)
            .owner(caller)
            .createdBy(caller)
            .contractType("LEASE")
            .startDate(LocalDate.now())
            .monthlyRent(BigDecimal.valueOf(300_000))
            .paymentDay(5)
            .status(ContractStatus.DRAFT)
            .build();
    SharedTestFixtures.injectId(draftContract, SharedTestFixtures.CONTRACT_ID);
  }

  private CreateContractRequest buildRequest() {
    CreateContractRequest req = new CreateContractRequest();
    req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
    req.setOwnerId(SharedTestFixtures.USER_ID);
    req.setContractType("LEASE");
    req.setStartDate(LocalDate.now());
    req.setMonthlyRent(BigDecimal.valueOf(300_000));
    req.setTenantId(SharedTestFixtures.TENANT_ID);
    return req;
  }

  private CreateContractRequest buildRequestWithoutTenant() {
    CreateContractRequest req = new CreateContractRequest();
    req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
    req.setOwnerId(SharedTestFixtures.USER_ID);
    req.setContractType("LEASE");
    req.setStartDate(LocalDate.now());
    req.setMonthlyRent(BigDecimal.valueOf(300_000));
    return req;
  }

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – contrat LEASE créé avec locataire")
    void create_lease_success() throws CustomException {
      Tenant tenant = new Tenant();
      SharedTestFixtures.injectId(tenant, SharedTestFixtures.TENANT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID)).thenReturn(Optional.of(property));
      when(userRepo.findById(SharedTestFixtures.USER_ID)).thenReturn(Optional.of(caller));
      when(tenantRepo.findById(SharedTestFixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
      when(contractRepo.save(any(Contract.class)))
          .thenAnswer(
              inv -> {
                Contract c = inv.getArgument(0);
                SharedTestFixtures.injectId(c, SharedTestFixtures.CONTRACT_ID);
                return c;
              });

      ContractResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, buildRequest());

      assertThat(resp).isNotNull();
      assertThat(resp.contractType()).isEqualTo("LEASE");
      assertThat(resp.status()).isEqualTo(ContractStatus.DRAFT);
    }

    @Test
    @DisplayName("Echec – LEASE sans tenantId → CONTRACT_CREATE_FAILURE")
    void create_leaseWithoutTenant_throwsContractCreateFailure() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID)).thenReturn(Optional.of(property));
      when(userRepo.findById(SharedTestFixtures.USER_ID)).thenReturn(Optional.of(caller));

      assertThatThrownBy(
              () -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildRequestWithoutTenant()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_CREATE_FAILURE);
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void create_userNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – bien introuvable → PROPERTY_GET_FAILURE_NOT_FOUND")
    void create_propertyNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – propriétaire introuvable → USER_NOT_FOUND")
    void create_ownerNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID)).thenReturn(Optional.of(property));
      when(userRepo.findById(SharedTestFixtures.USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("list()")
  class ListContracts {

    @Test
    @DisplayName("Succès – PARTNER filtre par agence sans statut ni recherche")
    void list_partnerNoStatus_filtersByAgency() throws CustomException {
      Page<Contract> page = new PageImpl<>(java.util.List.of(draftContract));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.searchByAgency(
              SharedTestFixtures.AGENCY_ID, null, null, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, null, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – PARTNER filtre par agence avec statut")
    void list_partnerWithStatus_filtersByAgencyAndStatus() throws CustomException {
      Page<Contract> page = new PageImpl<>(java.util.List.of(draftContract));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.searchByAgency(
              SharedTestFixtures.AGENCY_ID, ContractStatus.DRAFT, null, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result =
          service.list(
              SharedTestFixtures.KEYCLOAK_ID, ContractStatus.DRAFT, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – OWNER sans agence ni hôtel voit ses contrats")
    void list_ownerNoAgencyNoHotel_filtersByOwner() throws CustomException {
      User owner =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      Page<Contract> page = new PageImpl<>(java.util.List.of(draftContract));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(owner));
      when(contractRepo.searchByOwner(
              SharedTestFixtures.USER_ID, null, null, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, null, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – accès accordé via agence commune")
    void getById_sameAgency_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(draftContract));

      ContractResponse resp =
          service.getById(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Succès – accès accordé via propriété du contrat (owner)")
    void getById_owner_success() throws CustomException {
      Contract contract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(null)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.DRAFT)
              .build();
      SharedTestFixtures.injectId(contract, SharedTestFixtures.CONTRACT_ID);

      User noAgencyCaller =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgencyCaller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.of(contract));

      ContractResponse resp =
          service.getById(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Echec – contrat introuvable → CONTRACT_GET_FAILURE_NOT_FOUND")
    void getById_notFound_throwsCustomException() {
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.getById(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_GET_FAILURE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("update()")
  class Update {

    @Test
    @DisplayName("Succès – mise à jour d'un contrat DRAFT")
    void update_draft_success() throws CustomException {
      CreateContractRequest req = buildRequest();
      req.setMonthlyRent(BigDecimal.valueOf(400_000));

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(draftContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(draftContract);

      ContractResponse resp =
          service.update(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID, req);

      assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("Echec – contrat non DRAFT → CONTRACT_UPDATE_FAILURE")
    void update_nonDraft_throwsBadRequest() {
      Contract activeContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(caller)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.ACTIVE)
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(activeContract, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(activeContract));

      assertThatThrownBy(
              () ->
                  service.update(
                      SharedTestFixtures.KEYCLOAK_ID,
                      SharedTestFixtures.CONTRACT_ID,
                      buildRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_UPDATE_FAILURE);
    }
  }

  @Nested
  @DisplayName("submit()")
  class Submit {

    @Test
    @DisplayName("Succès – DRAFT → PENDING_SIGNATURE, PDF généré")
    void submit_draft_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(draftContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(draftContract);

      ContractResponse resp =
          service.submit(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(ContractStatus.PENDING_SIGNATURE);
    }

    @Test
    @DisplayName("Echec – contrat déjà PENDING_SIGNATURE → transition invalide")
    void submit_alreadyPending_throwsBadRequest() {
      Contract pendingContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(caller)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.PENDING_SIGNATURE)
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(pendingContract, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingContract));

      assertThatThrownBy(
              () -> service.submit(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("activate()")
  class Activate {

    @Test
    @DisplayName("Succès – PENDING_SIGNATURE → ACTIVE (non LEASE, pas de loyer créé)")
    void activate_mandateContract_noRentCreated() throws CustomException {
      Contract pendingContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .contractType("MANDATE")
              .startDate(LocalDate.now())
              .status(ContractStatus.PENDING_SIGNATURE)
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(pendingContract, SharedTestFixtures.CONTRACT_ID);

      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingContract);

      ContractResponse resp =
          service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(ContractStatus.ACTIVE);
      verify(paymentRepo, never()).existsByContractIdAndDueDate(any(), any());
    }

    @Test
    @DisplayName("Succès – LEASE PENDING_SIGNATURE → ACTIVE, premier loyer créé")
    void activate_leaseContract_firstRentCreated() throws CustomException {
      Contract pendingLease =
          Contract.builder()
              .property(property)
              .owner(caller)
              .contractType("LEASE")
              .startDate(LocalDate.of(2026, 3, 15))
              .status(ContractStatus.PENDING_SIGNATURE)
              .monthlyRent(BigDecimal.valueOf(300_000))
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(pendingLease, SharedTestFixtures.CONTRACT_ID);

      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingLease));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingLease);
      when(paymentRepo.existsByContractIdAndDueDate(any(), any())).thenReturn(false);
      when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      ContractResponse resp =
          service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp.status()).isEqualTo(ContractStatus.ACTIVE);
      verify(paymentRepo).existsByContractIdAndDueDate(any(), any());
      verify(paymentRepo).save(any());
    }

    @Test
    @DisplayName("Succès – LEASE ACTIVE, premier loyer idempotent (déjà existant)")
    void activate_leaseContract_rentAlreadyExists_skipped() throws CustomException {
      Contract pendingLease =
          Contract.builder()
              .property(property)
              .owner(caller)
              .contractType("LEASE")
              .startDate(LocalDate.of(2026, 3, 15))
              .status(ContractStatus.PENDING_SIGNATURE)
              .monthlyRent(BigDecimal.valueOf(300_000))
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(pendingLease, SharedTestFixtures.CONTRACT_ID);

      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingLease));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingLease);
      when(paymentRepo.existsByContractIdAndDueDate(any(), any())).thenReturn(true);

      service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      verify(paymentRepo, never()).save(any());
    }

    @Test
    @DisplayName("Echec – transition invalide (DRAFT → ACTIVE)")
    void activate_invalidTransition_throwsBadRequest() {
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(draftContract));

      assertThatThrownBy(
              () ->
                  service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("terminate()")
  class Terminate {

    @Test
    @DisplayName("Succès – ACTIVE → TERMINATED avec motif")
    void terminate_active_success() throws CustomException {
      Contract activeContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(caller)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.ACTIVE)
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(activeContract, SharedTestFixtures.CONTRACT_ID);

      TerminateContractRequest req = new TerminateContractRequest();
      req.setTerminationReason("Départ anticipé du locataire");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(activeContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(activeContract);

      ContractResponse resp =
          service.terminate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID, req);

      assertThat(resp.status()).isEqualTo(ContractStatus.TERMINATED);
    }

    @Test
    @DisplayName("Echec – DRAFT ne peut pas être résilié")
    void terminate_draft_throwsBadRequest() {
      TerminateContractRequest req = new TerminateContractRequest();
      req.setTerminationReason("Erreur de test");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(draftContract));

      assertThatThrownBy(
              () ->
                  service.terminate(
                      SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  @Nested
  @DisplayName("cancel()")
  class Cancel {

    @Test
    @DisplayName("Succès – DRAFT → CANCELLED")
    void cancel_draft_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(draftContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(draftContract);

      ContractResponse resp =
          service.cancel(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp.status()).isEqualTo(ContractStatus.CANCELLED);
    }

    @Test
    @DisplayName("Succès – PENDING_SIGNATURE → CANCELLED")
    void cancel_pendingSignature_success() throws CustomException {
      Contract pendingContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(caller)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.PENDING_SIGNATURE)
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(pendingContract, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingContract);

      ContractResponse resp =
          service.cancel(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp.status()).isEqualTo(ContractStatus.CANCELLED);
    }

    @Test
    @DisplayName("Echec – ACTIVE ne peut pas être annulé")
    void cancel_active_throwsBadRequest() {
      Contract activeContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(caller)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.ACTIVE)
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(activeContract, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(activeContract));

      assertThatThrownBy(
              () -> service.cancel(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(
              ResponseMessageConstants.CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }
}
