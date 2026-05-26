package com.africa.ubaxplatform.unit.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.document.codeList.DocumentStatus;
import com.africa.ubaxplatform.document.codeList.RefType;
import com.africa.ubaxplatform.document.entity.Document;
import com.africa.ubaxplatform.document.generator.ContractGenerator;
import com.africa.ubaxplatform.document.generator.InvoiceGenerator;
import com.africa.ubaxplatform.document.generator.ReceiptGenerator;
import com.africa.ubaxplatform.document.repository.DocumentRepository;
import com.africa.ubaxplatform.document.service.impl.DocumentServiceImpl;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl")
class DocumentServiceImplTest {

  @Mock DocumentRepository documentRepo;
  @Mock ContractRepository contractRepo;
  @Mock PaymentRepository paymentRepo;
  @Mock ContractGenerator contractGenerator;
  @Mock InvoiceGenerator invoiceGenerator;
  @Mock ReceiptGenerator receiptGenerator;
  @Mock MinioService minioService;
  @Mock EmailService emailService;

  @InjectMocks DocumentServiceImpl service;

  private User generatedBy;
  private Contract contract;
  private Payment payment;
  private Document pendingDoc;

  @BeforeEach
  void setUp() {
    generatedBy =
        SharedTestFixtures.buildAdminUser("kc-admin", SharedTestFixtures.USER_ID, UserRole.ADMIN);

    contract = Contract.builder().contractType("LEASE").referenceNumber("CTR-2025-ABCD").build();
    SharedTestFixtures.injectId(contract, SharedTestFixtures.CONTRACT_ID);

    payment = SharedTestFixtures.buildPayment(null, generatedBy, null);
    payment.setReference("PAY-2025-RENT-ABCD");
    payment.setPeriodLabel("Mai 2025");

    pendingDoc = Document.builder().status(DocumentStatus.PENDING).fileUrl("pending").build();
    SharedTestFixtures.injectId(pendingDoc, UUID.randomUUID());
  }

  // generateContractPdf

  @Nested
  @DisplayName("generateContractPdf()")
  class GenerateContractPdf {

    @Test
    @DisplayName("Succès – PDF généré et uploadé dans MinIO")
    void generateContractPdf_success() throws Exception {
      byte[] fakePdf = "PDF-CONTENT".getBytes();
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.of(contract));
      when(documentRepo.save(any(Document.class))).thenReturn(pendingDoc);
      when(contractGenerator.generate(contract)).thenReturn(fakePdf);
      when(minioService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
          .thenReturn("https://minio/contracts/contrat-CTR-2025-ABCD.pdf");

      Document result = service.generateContractPdf(SharedTestFixtures.CONTRACT_ID, generatedBy);

      assertThat(result).isNotNull();
      verify(contractGenerator).generate(contract);
      verify(minioService).uploadFile(anyString(), anyString(), any(), anyLong(), anyString());
      verify(documentRepo, times(2)).save(any(Document.class));
    }

    @Test
    @DisplayName("Echec générateur – statut FAILED enregistré sans lever d'exception")
    void generateContractPdf_generatorFails_statusFailed() throws Exception {
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.of(contract));
      when(documentRepo.save(any(Document.class))).thenReturn(pendingDoc);
      when(contractGenerator.generate(contract)).thenThrow(new RuntimeException("PDF error"));

      Document result = service.generateContractPdf(SharedTestFixtures.CONTRACT_ID, generatedBy);

      assertThat(result).isNotNull();
      verify(documentRepo, times(2)).save(any(Document.class));
    }

    @Test
    @DisplayName("Echec – contrat introuvable → NotFoundException")
    void generateContractPdf_contractNotFound_throws() {
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.generateContractPdf(SharedTestFixtures.CONTRACT_ID, generatedBy))
          .isInstanceOf(NotFoundException.class);
    }
  }

  // generateInvoice

  @Nested
  @DisplayName("generateInvoice()")
  class GenerateInvoice {

    @Test
    @DisplayName("Succès – facture PDF générée et uploadée")
    void generateInvoice_success() throws Exception {
      byte[] fakePdf = "INVOICE-PDF".getBytes();
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.of(payment));
      when(documentRepo.save(any(Document.class))).thenReturn(pendingDoc);
      when(invoiceGenerator.generate(payment)).thenReturn(fakePdf);
      when(minioService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
          .thenReturn("https://minio/invoices/facture-PAY-2025.pdf");

      Document result = service.generateInvoice(SharedTestFixtures.PAYMENT_ID, generatedBy);

      assertThat(result).isNotNull();
      verify(invoiceGenerator).generate(payment);
    }

    @Test
    @DisplayName("Echec – paiement introuvable → NotFoundException")
    void generateInvoice_paymentNotFound_throws() {
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.generateInvoice(SharedTestFixtures.PAYMENT_ID, generatedBy))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Echec générateur – statut FAILED enregistré")
    void generateInvoice_generatorFails_statusFailed() throws Exception {
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.of(payment));
      when(documentRepo.save(any(Document.class))).thenReturn(pendingDoc);
      when(invoiceGenerator.generate(payment)).thenThrow(new RuntimeException("render error"));

      Document result = service.generateInvoice(SharedTestFixtures.PAYMENT_ID, generatedBy);

      assertThat(result).isNotNull();
      verify(documentRepo, times(2)).save(any(Document.class));
    }
  }

  // generateReceipt

  @Nested
  @DisplayName("generateReceipt()")
  class GenerateReceipt {

    @Test
    @DisplayName("Succès – reçu PDF généré et uploadé")
    void generateReceipt_success() throws Exception {
      byte[] fakePdf = "RECEIPT-PDF".getBytes();
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.of(payment));
      when(documentRepo.save(any(Document.class))).thenReturn(pendingDoc);
      when(receiptGenerator.generate(payment)).thenReturn(fakePdf);
      when(minioService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
          .thenReturn("https://minio/receipts/recu-PAY-2025.pdf");

      Document result = service.generateReceipt(SharedTestFixtures.PAYMENT_ID, generatedBy);

      assertThat(result).isNotNull();
      verify(receiptGenerator).generate(payment);
    }

    @Test
    @DisplayName("Echec – paiement introuvable → NotFoundException")
    void generateReceipt_paymentNotFound_throws() {
      when(paymentRepo.findById(SharedTestFixtures.PAYMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.generateReceipt(SharedTestFixtures.PAYMENT_ID, generatedBy))
          .isInstanceOf(NotFoundException.class);
    }
  }

  // getDocumentsByRef

  @Nested
  @DisplayName("getDocumentsByRef()")
  class GetDocumentsByRef {

    @Test
    @DisplayName("Retourne la liste des documents associés à une référence")
    void getDocumentsByRef_returnsList() {
      when(documentRepo.findByRefIdAndRefType(SharedTestFixtures.CONTRACT_ID, RefType.CONTRACT))
          .thenReturn(List.of(pendingDoc));

      List<Document> result =
          service.getDocumentsByRef(SharedTestFixtures.CONTRACT_ID, RefType.CONTRACT);

      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Retourne une liste vide si aucun document trouvé")
    void getDocumentsByRef_empty() {
      when(documentRepo.findByRefIdAndRefType(any(), any())).thenReturn(List.of());

      List<Document> result =
          service.getDocumentsByRef(SharedTestFixtures.PAYMENT_ID, RefType.PAYMENT);

      assertThat(result).isEmpty();
    }
  }
}
