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
import com.africa.ubaxplatform.contract.mapper.PropertyToContractMapper;
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
    req.setContractType("LEASE");
    req.setStartDate(LocalDate.now());
    req.setMonthlyRent(BigDecimal.valueOf(300_000));
    req.setTenantId(SharedTestFixtures.TENANT_ID);
    return req;
  }

  private CreateContractRequest buildRequestWithoutTenant() {
    CreateContractRequest req = new CreateContractRequest();
    req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
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

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingContract);

      ContractResponse resp =
          service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(ContractStatus.ACTIVE);
      verify(paymentRepo, never()).existsByContractIdAndDueDateAndDeletedAtIsNull(any(), any());
      // Le bien doit passer en RESERVED
      assertThat(property.getStatus()).isEqualTo(PropertyStatus.RESERVED);
      verify(propertyRepo).save(property);
    }

    @Test
    @DisplayName("Succès – activate() marque le bien RESERVED (retrait portail public)")
    void activate_setsPropertyStatusToReserved() throws CustomException {
      assertThat(property.getStatus()).isEqualTo(PropertyStatus.PUBLISHED);

      Contract pendingContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .contractType("LEASE")
              .startDate(LocalDate.now())
              .status(ContractStatus.PENDING_SIGNATURE)
              .monthlyRent(BigDecimal.valueOf(300_000))
              .paymentDay(5)
              .build();
      SharedTestFixtures.injectId(pendingContract, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingContract);
      when(paymentRepo.existsByContractIdAndDueDateAndDeletedAtIsNull(any(), any()))
          .thenReturn(false);
      when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(property.getStatus())
          .as("Le bien doit être RESERVED après activation du contrat")
          .isEqualTo(PropertyStatus.RESERVED);
    }

    @Test
    @DisplayName("Succès – LEASE PENDING_SIGNATURE → ACTIVE, premier loyer = startDate + 1 mois")
    void activate_leaseContract_firstRentCreated() throws CustomException {
      // Entrée le 14 novembre → premier loyer attendu le 14 décembre
      LocalDate startDate = LocalDate.of(2026, 11, 14);
      LocalDate expectedDueDate = LocalDate.of(2026, 12, 14);

      Contract pendingLease =
          Contract.builder()
              .property(property)
              .owner(caller)
              .contractType("LEASE")
              .startDate(startDate)
              .status(ContractStatus.PENDING_SIGNATURE)
              .monthlyRent(BigDecimal.valueOf(300_000))
              .paymentDay(14) // auto-dérivé du jour d'entrée
              .build();
      SharedTestFixtures.injectId(pendingLease, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingLease));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingLease);
      when(paymentRepo.existsByContractIdAndDueDateAndDeletedAtIsNull(
              SharedTestFixtures.CONTRACT_ID, expectedDueDate))
          .thenReturn(false);
      when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      ContractResponse resp =
          service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      assertThat(resp.status()).isEqualTo(ContractStatus.ACTIVE);
      verify(paymentRepo)
          .existsByContractIdAndDueDateAndDeletedAtIsNull(
              SharedTestFixtures.CONTRACT_ID, expectedDueDate);
      verify(paymentRepo).save(any());
    }

    @Test
    @DisplayName("Succès – LEASE ACTIVE, premier loyer idempotent (déjà existant)")
    void activate_leaseContract_rentAlreadyExists_skipped() throws CustomException {
      // Entrée le 14 novembre → premier loyer le 14 décembre (déjà existant → skipped)
      Contract pendingLease =
          Contract.builder()
              .property(property)
              .owner(caller)
              .contractType("LEASE")
              .startDate(LocalDate.of(2026, 11, 14))
              .status(ContractStatus.PENDING_SIGNATURE)
              .monthlyRent(BigDecimal.valueOf(300_000))
              .paymentDay(14)
              .build();
      SharedTestFixtures.injectId(pendingLease, SharedTestFixtures.CONTRACT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(pendingLease));
      when(contractRepo.save(any(Contract.class))).thenReturn(pendingLease);
      when(paymentRepo.existsByContractIdAndDueDateAndDeletedAtIsNull(any(), any()))
          .thenReturn(true);

      service.activate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID);

      verify(paymentRepo, never()).save(any());
    }

    @Test
    @DisplayName("Echec – transition invalide (DRAFT → ACTIVE)")
    void activate_invalidTransition_throwsBadRequest() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
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
    @DisplayName("Succès – ACTIVE → TERMINATED avec motif + bien remis en PUBLISHED")
    void terminate_active_success() throws CustomException {
      property.setStatus(PropertyStatus.RESERVED); // bien déjà réservé par un contrat actif
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
      // Le bien doit repasser en PUBLISHED (à nouveau visible sur le portail)
      assertThat(property.getStatus()).isEqualTo(PropertyStatus.PUBLISHED);
      verify(propertyRepo).save(property);
    }

    @Test
    @DisplayName("Succès – terminate() remet le bien en PUBLISHED (remis sur le portail)")
    void terminate_setsPropertyStatusToPublished() throws CustomException {
      property.setStatus(PropertyStatus.RESERVED);

      Contract activeContract =
          Contract.builder()
              .property(property)
              .owner(caller)
              .createdBy(caller)
              .contractType("SALE")
              .startDate(LocalDate.now())
              .status(ContractStatus.ACTIVE)
              .paymentDay(1)
              .build();
      SharedTestFixtures.injectId(activeContract, SharedTestFixtures.CONTRACT_ID);

      TerminateContractRequest req = new TerminateContractRequest();
      req.setTerminationReason("Vente annulée par l'acquéreur");

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID))
          .thenReturn(Optional.of(activeContract));
      when(contractRepo.save(any(Contract.class))).thenReturn(activeContract);

      service.terminate(SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.CONTRACT_ID, req);

      assertThat(property.getStatus())
          .as("Le bien doit être PUBLISHED après résiliation du contrat")
          .isEqualTo(PropertyStatus.PUBLISHED);
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

  @Nested
  @DisplayName("PropertyToContractMapping – Mapping & Coherence")
  class PropertyToContractMappingTests {

    @Nested
    @DisplayName("mapToContractType()")
    class MapToContractType {

      @Test
      @DisplayName("SALE property → SALE contract")
      void mapSale() {
        String result = PropertyToContractMapper.mapToContractType("SALE");
        assertThat(result).isEqualTo("SALE");
      }

      @Test
      @DisplayName("RENT property → LEASE contract")
      void mapRent() {
        String result = PropertyToContractMapper.mapToContractType("RENT");
        assertThat(result).isEqualTo("LEASE");
      }

      @Test
      @DisplayName("RENT_FURNISHED property → LEASE contract")
      void mapRentFurnished() {
        String result = PropertyToContractMapper.mapToContractType("RENT_FURNISHED");
        assertThat(result).isEqualTo("LEASE");
      }

      @Test
      @DisplayName("SHORT_STAY property → RESERVATION contract")
      void mapShortStay() {
        String result = PropertyToContractMapper.mapToContractType("SHORT_STAY");
        assertThat(result).isEqualTo("RESERVATION");
      }

      @Test
      @DisplayName("Unknown type → null")
      void mapUnknownType() {
        String result = PropertyToContractMapper.mapToContractType("UNKNOWN");
        assertThat(result).isNull();
      }

      @Test
      @DisplayName("null input → null output")
      void mapNullInput() {
        String result = PropertyToContractMapper.mapToContractType(null);
        assertThat(result).isNull();
      }
    }

    @Nested
    @DisplayName("isCoherent() – Valid combinations")
    class IsCoherentValid {

      @Test
      @DisplayName("SALE property + SALE contract → true")
      void coherent_sale_sale() {
        boolean result = PropertyToContractMapper.isCoherent("SALE", "SALE");
        assertThat(result).isTrue();
      }

      @Test
      @DisplayName("RENT property + LEASE contract → true")
      void coherent_rent_lease() {
        boolean result = PropertyToContractMapper.isCoherent("RENT", "LEASE");
        assertThat(result).isTrue();
      }

      @Test
      @DisplayName("RENT_FURNISHED property + LEASE contract → true")
      void coherent_rentFurnished_lease() {
        boolean result = PropertyToContractMapper.isCoherent("RENT_FURNISHED", "LEASE");
        assertThat(result).isTrue();
      }

      @Test
      @DisplayName("SHORT_STAY property + RESERVATION contract → true")
      void coherent_shortStay_reservation() {
        boolean result = PropertyToContractMapper.isCoherent("SHORT_STAY", "RESERVATION");
        assertThat(result).isTrue();
      }

      @Test
      @DisplayName("RENT property + RENT_TO_OWN contract → true")
      void coherent_rent_rentToOwn() {
        boolean result = PropertyToContractMapper.isCoherent("RENT", "RENT_TO_OWN");
        assertThat(result).isTrue();
      }

      @Test
      @DisplayName("RENT_FURNISHED property + RENT_TO_OWN contract → true")
      void coherent_rentFurnished_rentToOwn() {
        boolean result = PropertyToContractMapper.isCoherent("RENT_FURNISHED", "RENT_TO_OWN");
        assertThat(result).isTrue();
      }

      @Test
      @DisplayName("Any property + MANDATE contract → true (mandate is flexible)")
      void coherent_any_mandate() {
        assertThat(PropertyToContractMapper.isCoherent("SALE", "MANDATE")).isTrue();
        assertThat(PropertyToContractMapper.isCoherent("RENT", "MANDATE")).isTrue();
        assertThat(PropertyToContractMapper.isCoherent("SHORT_STAY", "MANDATE")).isTrue();
      }

      @Test
      @DisplayName("null values → true (defensive: allow non-matching if missing data)")
      void coherent_nullValues() {
        assertThat(PropertyToContractMapper.isCoherent(null, "LEASE")).isTrue();
        assertThat(PropertyToContractMapper.isCoherent("RENT", null)).isTrue();
        assertThat(PropertyToContractMapper.isCoherent(null, null)).isTrue();
      }
    }

    @Nested
    @DisplayName("isCoherent() – Invalid combinations")
    class IsCoherentInvalid {

      @Test
      @DisplayName("SALE property + LEASE contract → false")
      void incoherent_sale_lease() {
        boolean result = PropertyToContractMapper.isCoherent("SALE", "LEASE");
        assertThat(result).isFalse();
      }

      @Test
      @DisplayName("RENT property + SALE contract → false")
      void incoherent_rent_sale() {
        boolean result = PropertyToContractMapper.isCoherent("RENT", "SALE");
        assertThat(result).isFalse();
      }

      @Test
      @DisplayName("SHORT_STAY property + LEASE contract → false")
      void incoherent_shortStay_lease() {
        boolean result = PropertyToContractMapper.isCoherent("SHORT_STAY", "LEASE");
        assertThat(result).isFalse();
      }

      @Test
      @DisplayName("RENT property + RESERVATION contract → false")
      void incoherent_rent_reservation() {
        boolean result = PropertyToContractMapper.isCoherent("RENT", "RESERVATION");
        assertThat(result).isFalse();
      }

      @Test
      @DisplayName("SALE property + RENT_TO_OWN contract → false")
      void incoherent_sale_rentToOwn() {
        boolean result = PropertyToContractMapper.isCoherent("SALE", "RENT_TO_OWN");
        assertThat(result).isFalse();
      }

      @Test
      @DisplayName("Unknown contract type → false")
      void incoherent_unknownContractType() {
        boolean result = PropertyToContractMapper.isCoherent("RENT", "UNKNOWN");
        assertThat(result).isFalse();
      }
    }
  }

  @Nested
  @DisplayName("create() – Auto-fill & Coherence Validation")
  class CreateWithAutoFillAndCoherence {

    @Test
    @DisplayName("Success – SALE property auto-fills salePrice from property.price")
    void create_saleProperty_autoFillsSalePrice() throws CustomException {
      Property saleProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Villa à vendre")
              .propertyType("VILLA")
              .transactionType("SALE")
              .price(BigDecimal.valueOf(500_000_000)) // 500M CFA
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(saleProperty, SharedTestFixtures.PROPERTY_ID);

      Tenant buyer = new Tenant();
      SharedTestFixtures.injectId(buyer, SharedTestFixtures.TENANT_ID);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setTenantId(SharedTestFixtures.TENANT_ID);
      req.setContractType("SALE");
      req.setStartDate(LocalDate.now());
      // salePrice NOT provided – should be auto-filled from property.price

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(saleProperty));
      when(tenantRepo.findById(SharedTestFixtures.TENANT_ID)).thenReturn(Optional.of(buyer));
      when(contractRepo.save(any(Contract.class)))
          .thenAnswer(
              inv -> {
                Contract c = inv.getArgument(0);
                SharedTestFixtures.injectId(c, SharedTestFixtures.CONTRACT_ID);
                return c;
              });

      ContractResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp).isNotNull();
      assertThat(resp.contractType()).isEqualTo("SALE");
      assertThat(resp.tenantId()).isEqualTo(SharedTestFixtures.TENANT_ID);
    }

    @Test
    @DisplayName("Success – RENT property auto-fills monthlyRent from property.price")
    void create_rentProperty_autoFillsMonthlyRent() throws CustomException {
      Property rentProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Apt meublé à louer")
              .propertyType("APARTMENT")
              .transactionType("RENT_FURNISHED")
              .price(BigDecimal.valueOf(450_000)) // 450k CFA/month
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(rentProperty, SharedTestFixtures.PROPERTY_ID);

      Tenant tenant = new Tenant();
      SharedTestFixtures.injectId(tenant, SharedTestFixtures.TENANT_ID);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setContractType("LEASE");
      req.setStartDate(LocalDate.now());
      // monthlyRent NOT provided – should be auto-filled
      req.setTenantId(SharedTestFixtures.TENANT_ID);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(rentProperty));
      when(tenantRepo.findById(any(UUID.class))).thenReturn(Optional.of(tenant));
      when(contractRepo.save(any(Contract.class)))
          .thenAnswer(
              inv -> {
                Contract c = inv.getArgument(0);
                SharedTestFixtures.injectId(c, SharedTestFixtures.CONTRACT_ID);
                return c;
              });

      ContractResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp).isNotNull();
      assertThat(resp.contractType()).isEqualTo("LEASE");
      assertThat(resp.monthlyRent()).isEqualByComparingTo(BigDecimal.valueOf(450_000));
    }

    @Test
    @DisplayName("Failure – SALE property + LEASE contract → incoherent")
    void create_incoherent_salePropertyLeaseContract() {
      Property saleProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Villa à vendre")
              .propertyType("VILLA")
              .transactionType("SALE")
              .price(BigDecimal.valueOf(500_000_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(saleProperty, SharedTestFixtures.PROPERTY_ID);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setContractType("LEASE"); // Wrong: should be SALE for SALE property
      req.setStartDate(LocalDate.now());
      req.setMonthlyRent(BigDecimal.valueOf(300_000));
      // tenantId intentionally not set — coherence check fires before type validation

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(saleProperty));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_CREATE_FAILURE)
          .cause()
          .hasMessageContaining("Incohérence métier");
    }

    @Test
    @DisplayName("Failure – SHORT_STAY property + LEASE contract → incoherent")
    void create_incoherent_shortStayPropertyLeaseContract() {
      Property shortStayProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Studio court terme")
              .propertyType("APARTMENT")
              .transactionType("SHORT_STAY")
              .price(BigDecimal.valueOf(150_000)) // Per night?
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(shortStayProperty, SharedTestFixtures.PROPERTY_ID);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setContractType("LEASE"); // Wrong: should be RESERVATION for SHORT_STAY
      req.setStartDate(LocalDate.now());
      req.setMonthlyRent(BigDecimal.valueOf(300_000));
      // tenantId intentionally not set — coherence check fires before type validation

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(shortStayProperty));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_CREATE_FAILURE)
          .cause()
          .hasMessageContaining("Incohérence métier");
    }

    @Test
    @DisplayName("Success – RENT_TO_OWN with durationYears computes endDate automatically")
    void create_rentToOwn_withDurationYears_calculatesEndDate() throws CustomException {
      Property rentProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Villa location-vente")
              .propertyType("VILLA")
              .transactionType("RENT")
              .price(BigDecimal.valueOf(200_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(rentProperty, SharedTestFixtures.PROPERTY_ID);

      Tenant tenant = new Tenant();
      SharedTestFixtures.injectId(tenant, SharedTestFixtures.TENANT_ID);

      LocalDate startDate = LocalDate.of(2026, 6, 1);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setContractType("RENT_TO_OWN");
      req.setStartDate(startDate);
      req.setTenantId(SharedTestFixtures.TENANT_ID);
      req.setSalePrice(BigDecimal.valueOf(12_000_000));
      req.setMonthlyInstallment(BigDecimal.valueOf(200_000));
      req.setDurationYears(5); // endDate should be computed as 2031-06-01

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(rentProperty));
      when(tenantRepo.findById(any(UUID.class))).thenReturn(Optional.of(tenant));
      when(contractRepo.save(any(Contract.class)))
          .thenAnswer(
              inv -> {
                Contract c = inv.getArgument(0);
                SharedTestFixtures.injectId(c, SharedTestFixtures.CONTRACT_ID);
                return c;
              });

      ContractResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp).isNotNull();
      assertThat(resp.contractType()).isEqualTo("RENT_TO_OWN");
      assertThat(resp.endDate()).isEqualTo(startDate.plusYears(5));
    }

    @Test
    @DisplayName(
        "Failure – RENT_TO_OWN without endDate and durationYears → CONTRACT_CREATE_FAILURE")
    void create_rentToOwn_missingEndDateAndDurationYears_throwsError() {
      Property rentProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Villa location-vente")
              .propertyType("VILLA")
              .transactionType("RENT")
              .price(BigDecimal.valueOf(200_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(rentProperty, SharedTestFixtures.PROPERTY_ID);

      Tenant tenant = new Tenant();
      SharedTestFixtures.injectId(tenant, SharedTestFixtures.TENANT_ID);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setContractType("RENT_TO_OWN");
      req.setStartDate(LocalDate.of(2026, 6, 1));
      req.setTenantId(SharedTestFixtures.TENANT_ID);
      req.setSalePrice(BigDecimal.valueOf(12_000_000));
      req.setMonthlyInstallment(BigDecimal.valueOf(200_000));
      // endDate and durationYears both absent

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(rentProperty));
      when(tenantRepo.findById(any(UUID.class))).thenReturn(Optional.of(tenant));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_CREATE_FAILURE)
          .cause()
          .hasMessageContaining("endDate ou durationYears");
    }

    @Test
    @DisplayName("Echec – MANDATE bloqué sur POST /v1/contracts → CONTRACT_CREATE_FAILURE")
    void create_mandateType_isBlocked() {
      Property saleProperty =
          Property.builder()
              .owner(caller)
              .agency(agency)
              .title("Villa à vendre")
              .propertyType("VILLA")
              .transactionType("SALE")
              .price(BigDecimal.valueOf(500_000_000))
              .status(PropertyStatus.PUBLISHED)
              .city("Abidjan")
              .build();
      SharedTestFixtures.injectId(saleProperty, SharedTestFixtures.PROPERTY_ID);

      CreateContractRequest req = new CreateContractRequest();
      req.setPropertyId(SharedTestFixtures.PROPERTY_ID);
      req.setContractType("MANDATE");
      req.setStartDate(LocalDate.now());

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(SharedTestFixtures.PROPERTY_ID))
          .thenReturn(Optional.of(saleProperty));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_CREATE_FAILURE)
          .cause()
          .hasMessageContaining("/v1/mandates");

      verify(contractRepo, never()).save(any());
    }
  }
}
