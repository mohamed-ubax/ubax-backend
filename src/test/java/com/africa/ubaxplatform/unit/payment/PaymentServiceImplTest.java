package com.africa.ubaxplatform.unit.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.document.generator.InvoiceGenerator;
import com.africa.ubaxplatform.document.generator.ReceiptGenerator;
import com.africa.ubaxplatform.document.repository.DocumentRepository;
import com.africa.ubaxplatform.document.service.impl.DocumentServiceImpl;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.dto.PaymentCreateRequest;
import com.africa.ubaxplatform.payment.dto.PaymentResponse;
import com.africa.ubaxplatform.payment.dto.PaymentStatusUpdateRequest;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.event.PaymentPaidEvent;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.payment.service.impl.PaymentServiceImpl;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import com.africa.ubaxplatform.tenant.repository.TenantRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl – tests unitaires")
class PaymentServiceImplTest {

  @Mock private PaymentRepository paymentRepo;
  @Mock private UserRepository userRepo;
  @Mock private PropertyRepository propertyRepo;
  @Mock private TenantRepository tenantRepo;
  @Mock private ContractRepository contractRepo;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private DocumentRepository documentRepo;
  @Mock private ReceiptGenerator receiptGenerator;
  @Mock private MinioService minioService;
  @Mock private InvoiceGenerator invoiceGenerator;
  @Mock private EmailService emailService;

  @InjectMocks private PaymentServiceImpl service;
  private DocumentServiceImpl documentService;
  private Agency agency;
  private User caller;
  private Payment pendingPayment;

  @BeforeEach
  void setUp() {
    documentService =
        new DocumentServiceImpl(
            documentRepo,
            contractRepo,
            paymentRepo,
            null,
            invoiceGenerator,
            receiptGenerator,
            minioService,
            emailService);
    agency = SharedTestFixtures.buildAgency();
    caller = SharedTestFixtures.buildPartnerUser();
    pendingPayment = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.PENDING);
  }

  private PaymentCreateRequest buildRequest(
      LocalDate dueDate, LocalDate paidDate, BigDecimal amountPaid) {
    return new PaymentCreateRequest(
        null,
        null,
        null,
        PaymentType.RENT,
        PaymentMethod.MOBILE_MONEY,
        BigDecimal.valueOf(350_000),
        amountPaid,
        dueDate,
        paidDate,
        "Octobre 2025",
        null,
        null,
        null);
  }

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – montant complet reçu → statut PAID")
    void create_fullPayment_returnsPaid() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });

      PaymentCreateRequest req =
          buildRequest(LocalDate.now().plusDays(5), LocalDate.now(), BigDecimal.valueOf(350_000));
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName(
        "Succès – paiement complet → statut PAID et reçu PDF généré automatiquement dans MinIO")
    void create_fullPayment_statusPaidAndReceiptGeneratedAutomatically() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });
      when(receiptGenerator.generate(any())).thenReturn(new byte[] {1, 2, 3});
      when(minioService.uploadFile(any(), any(), any(), anyLong(), any()))
          .thenReturn("https://minio/documents-generated/recus/test/recu.pdf");
      when(documentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      PaymentCreateRequest req =
          buildRequest(LocalDate.now().plusDays(5), LocalDate.now(), BigDecimal.valueOf(350_000));
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.status()).isEqualTo(PaymentStatus.PAID);

      // Capturer et exécuter le listener sur le vrai DocumentServiceImpl
      ArgumentCaptor<PaymentPaidEvent> eventCaptor =
          ArgumentCaptor.forClass(PaymentPaidEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      assertThatCode(() -> documentService.onPaymentPaid(eventCaptor.getValue()))
          .doesNotThrowAnyException();

      verify(minioService)
          .uploadFile(
              eq("documents-generated"),
              argThat(path -> path.startsWith("recus/")),
              any(),
              anyLong(),
              eq("application/pdf"));
    }

    @Test
    @DisplayName("Succès – montant partiel → statut PARTIAL")
    void create_partialPayment_returnsPartial() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });

      PaymentCreateRequest req =
          buildRequest(LocalDate.now().plusDays(5), LocalDate.now(), BigDecimal.valueOf(100_000));
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.status()).isEqualTo(PaymentStatus.PARTIAL);
    }

    @Test
    @DisplayName("Succès – échéance future sans paiement → statut PENDING")
    void create_futureDueDate_returnsPending() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });

      PaymentCreateRequest req = buildRequest(LocalDate.now().plusDays(5), null, null);
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Succès – échéance passée sans paiement → statut LATE")
    void create_pastDueDate_returnsLate() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });

      PaymentCreateRequest req = buildRequest(LocalDate.now().minusDays(5), null, null);
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.status()).isEqualTo(PaymentStatus.LATE);
    }

    @Test
    @DisplayName("Succès – référence explicite utilisée telle quelle")
    void create_withExplicitReference_usesProvidedReference() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });

      PaymentCreateRequest req =
          new PaymentCreateRequest(
              null,
              null,
              null,
              PaymentType.RENT,
              PaymentMethod.CASH,
              BigDecimal.valueOf(350_000),
              null,
              LocalDate.now().plusDays(5),
              null,
              null,
              "REF-CUSTOM-001",
              null,
              null);
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.reference()).isEqualTo("REF-CUSTOM-001");
    }

    @Test
    @DisplayName("Succès – avec contractId → charge le contrat")
    void create_withContractId_loadsContract() throws CustomException {
      UUID contractId = SharedTestFixtures.CONTRACT_ID;
      Contract contract = Contract.builder().build();
      SharedTestFixtures.injectId(contract, contractId);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(contractId)).thenReturn(Optional.of(contract));
      when(paymentRepo.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                SharedTestFixtures.injectId(p, SharedTestFixtures.PAYMENT_ID);
                return p;
              });

      PaymentCreateRequest req =
          new PaymentCreateRequest(
              contractId,
              null,
              null,
              PaymentType.RENT,
              PaymentMethod.MOBILE_MONEY,
              BigDecimal.valueOf(350_000),
              null,
              LocalDate.now().plusDays(5),
              null,
              null,
              null,
              null,
              null);
      PaymentResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp).isNotNull();
      verify(contractRepo).findById(contractId);
    }

    @Test
    @DisplayName("Echec – contractId inexistant → CONTRACT_GET_FAILURE_NOT_FOUND")
    void create_contractNotFound_throwsCustomException() {
      UUID contractId = UUID.randomUUID();
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(contractRepo.findById(contractId)).thenReturn(Optional.empty());

      PaymentCreateRequest req =
          new PaymentCreateRequest(
              contractId,
              null,
              null,
              PaymentType.RENT,
              null,
              BigDecimal.valueOf(350_000),
              null,
              LocalDate.now().plusDays(5),
              null,
              null,
              null,
              null,
              null);

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.CONTRACT_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void create_userNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.create(
                      SharedTestFixtures.KEYCLOAK_ID,
                      buildRequest(LocalDate.now().plusDays(5), null, null)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – utilisateur sans agence → PAYMENT_CREATE_FAILURE_BAD_REQUEST")
    void create_noAgency_throwsBadRequest() {
      User noAgency =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgency));

      assertThatThrownBy(
              () ->
                  service.create(
                      SharedTestFixtures.KEYCLOAK_ID,
                      buildRequest(LocalDate.now().plusDays(5), null, null)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);

      verify(paymentRepo, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – paiement trouvé")
    void getById_success() throws CustomException {
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID))
          .thenReturn(Optional.of(pendingPayment));

      PaymentResponse resp = service.getById(SharedTestFixtures.PAYMENT_ID);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Echec – paiement introuvable → PAYMENT_GET_FAILURE_NOT_FOUND")
    void getById_notFound_throwsCustomException() {
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(SharedTestFixtures.PAYMENT_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_GET_FAILURE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("list()")
  class ListPayments {

    @Test
    @DisplayName("Succès – PARTNER voit les paiements de son agence (agencyId filtré)")
    void list_partner_filtersByAgency() throws CustomException {
      Page<Payment> page = new PageImpl<>(List.of(pendingPayment));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findWithFilters(
              eq(SharedTestFixtures.AGENCY_ID),
              isNull(),
              isNull(),
              isNull(),
              isNull(),
              isNull(),
              isNull(),
              isNull(),
              any()))
          .thenReturn(page);

      var result =
          service.list(
              SharedTestFixtures.KEYCLOAK_ID,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – ADMIN sans agence voit tous les paiements (agencyId = null)")
    void list_admin_noAgencyFilter() throws CustomException {
      User admin =
          SharedTestFixtures.buildAdminUser(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID, UserRole.ADMIN);
      Page<Payment> page = new PageImpl<>(List.of(pendingPayment));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(paymentRepo.findWithFilters(
              isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
              any()))
          .thenReturn(page);

      var result =
          service.list(
              SharedTestFixtures.KEYCLOAK_ID,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void list_userNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.list(
                      SharedTestFixtures.KEYCLOAK_ID,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      PageRequest.of(0, 10)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("listLate()")
  class ListLate {

    @Test
    @DisplayName("Succès – retourne les paiements en retard de l'agence")
    void listLate_success() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findLateByAgency(eq(SharedTestFixtures.AGENCY_ID), any()))
          .thenReturn(List.of(pendingPayment));

      var result = service.listLate(SharedTestFixtures.KEYCLOAK_ID);

      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Echec – utilisateur sans agence → PAYMENT_CREATE_FAILURE_BAD_REQUEST")
    void listLate_noAgency_throwsBadRequest() {
      User noAgency =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgency));

      assertThatThrownBy(() -> service.listLate(SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
    }
  }

  @Nested
  @DisplayName("updateStatus()")
  class UpdateStatus {

    @Test
    @DisplayName("Succès – passage à PAID déclenche la génération du reçu")
    void updateStatus_toPaid_generatesReceipt() throws CustomException {
      Payment paidPayment = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.PENDING);
      PaymentStatusUpdateRequest req =
          new PaymentStatusUpdateRequest(PaymentStatus.PAID, null, null, null, null, null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID))
          .thenReturn(Optional.of(paidPayment));
      when(paymentRepo.save(any(Payment.class))).thenReturn(paidPayment);

      PaymentResponse resp =
          service.updateStatus(SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Succès – génération reçu échoue silencieusement (pas de propagation)")
    void updateStatus_toPaid_receiptFailureSilent() throws CustomException {
      Payment paidPayment = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.PENDING);
      PaymentStatusUpdateRequest req =
          new PaymentStatusUpdateRequest(PaymentStatus.PAID, null, null, null, null, null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID))
          .thenReturn(Optional.of(paidPayment));
      when(paymentRepo.save(any(Payment.class))).thenReturn(paidPayment);
      when(documentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(receiptGenerator.generate(any())).thenThrow(new RuntimeException("MinIO KO"));

      PaymentResponse resp =
          service.updateStatus(SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.status()).isEqualTo(PaymentStatus.PAID);

      ArgumentCaptor<PaymentPaidEvent> eventCaptor =
          ArgumentCaptor.forClass(PaymentPaidEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());

      // Le listener absorbe l'exception silencieusement
      assertThatCode(() -> documentService.onPaymentPaid(eventCaptor.getValue()))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Succès – PARTIAL avec montant complet → auto-corrigé en PAID")
    void updateStatus_partialFullAmount_autoCorrectsToPaid() throws CustomException {
      Payment payment = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.PARTIAL);
      PaymentStatusUpdateRequest req =
          new PaymentStatusUpdateRequest(
              PaymentStatus.PARTIAL, null, BigDecimal.valueOf(350_000), null, null, null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.of(payment));
      when(paymentRepo.save(any(Payment.class))).thenReturn(payment);

      PaymentResponse resp =
          service.updateStatus(SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Echec – paiement annulé → PAYMENT_CREATE_FAILURE_BAD_REQUEST")
    void updateStatus_cancelledPayment_throwsBadRequest() {
      Payment cancelled = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.CANCELLED);
      PaymentStatusUpdateRequest req =
          new PaymentStatusUpdateRequest(PaymentStatus.PAID, null, null, null, null, null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.of(cancelled));

      assertThatThrownBy(
              () ->
                  service.updateStatus(
                      SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
    }

    @Test
    @DisplayName("Echec – paiement introuvable → PAYMENT_GET_FAILURE_NOT_FOUND")
    void updateStatus_notFound_throwsCustomException() {
      PaymentStatusUpdateRequest req =
          new PaymentStatusUpdateRequest(PaymentStatus.PAID, null, null, null, null, null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.updateStatus(
                      SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – PARTNER d'une autre agence → USER_FORBIDDEN")
    void updateStatus_differentAgency_throwsForbidden() {
      Agency otherAgency = Agency.builder().name("Autre Agence").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());
      User otherCaller =
          SharedTestFixtures.buildPartnerUser("other-kc", UUID.randomUUID(), otherAgency);

      Payment payment = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.PENDING);
      PaymentStatusUpdateRequest req =
          new PaymentStatusUpdateRequest(PaymentStatus.PAID, null, null, null, null, null);

      when(userRepo.findByKeycloakId("other-kc")).thenReturn(Optional.of(otherCaller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.of(payment));

      assertThatThrownBy(() -> service.updateStatus(SharedTestFixtures.PAYMENT_ID, "other-kc", req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  @Nested
  @DisplayName("delete()")
  class Delete {

    @Test
    @DisplayName("Succès – paiement PENDING supprimé par le même PARTNER")
    void delete_pending_deletesSuccessfully() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID))
          .thenReturn(Optional.of(pendingPayment));

      service.delete(SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID);

      verify(paymentRepo).delete(pendingPayment);
    }

    @Test
    @DisplayName("Echec – paiement PAID ne peut pas être supprimé")
    void delete_paid_throwsBadRequest() {
      Payment paidPayment = SharedTestFixtures.buildPayment(agency, caller, PaymentStatus.PAID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID))
          .thenReturn(Optional.of(paidPayment));

      assertThatThrownBy(
              () -> service.delete(SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);

      verify(paymentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Echec – paiement introuvable → PAYMENT_GET_FAILURE_NOT_FOUND")
    void delete_notFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.delete(SharedTestFixtures.PAYMENT_ID, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_GET_FAILURE_NOT_FOUND);
    }
  }
}
