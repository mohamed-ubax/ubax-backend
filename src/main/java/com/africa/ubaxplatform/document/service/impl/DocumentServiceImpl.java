package com.africa.ubaxplatform.document.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.document.codeList.DocumentStatus;
import com.africa.ubaxplatform.document.codeList.DocumentType;
import com.africa.ubaxplatform.document.codeList.RefType;
import com.africa.ubaxplatform.document.entity.Document;
import com.africa.ubaxplatform.document.generator.ContractGenerator;
import com.africa.ubaxplatform.document.generator.InvoiceGenerator;
import com.africa.ubaxplatform.document.generator.ReceiptGenerator;
import com.africa.ubaxplatform.document.repository.DocumentRepository;
import com.africa.ubaxplatform.document.service.interfaces.DocumentService;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.event.PaymentPaidEvent;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import java.io.ByteArrayInputStream;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

  private static final String BUCKET = "documents-generated";

  private final DocumentRepository documentRepo;
  private final ContractRepository contractRepo;
  private final PaymentRepository paymentRepo;
  private final ContractGenerator contractGenerator;
  private final InvoiceGenerator invoiceGenerator;
  private final ReceiptGenerator receiptGenerator;
  private final MinioService minioService;
  private final EmailService emailService;

  @Override
  @Transactional
  public Document generateContractPdf(UUID contractId, User generatedBy) {
    Contract contract =
        contractRepo
            .findById(contractId)
            .orElseThrow(() -> new NotFoundException("Contrat introuvable : " + contractId));

    Document record =
        initDocument(DocumentType.CONTRACT, contractId, RefType.CONTRACT, generatedBy);
    record.setTitle(
        "Contrat " + contract.getContractType() + " – " + contract.getReferenceNumber());
    record = documentRepo.save(record);

    try {
      byte[] pdf = contractGenerator.generate(contract);
      String fileName = "contrat-" + contract.getReferenceNumber() + ".pdf";
      String objectName = "contracts/" + contractId + "/" + fileName;
      String url =
          minioService.uploadFile(
              BUCKET, objectName, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");

      record.setFileUrl(url);
      record.setFileName(fileName);
      record.setFileSize((long) pdf.length);
      record.setReferenceNumber(generateRef("CTR"));
      record.setStatus(DocumentStatus.GENERATED);

      contract.setFileUrl(url);
      contractRepo.save(contract);

      log.info("Contrat PDF généré : contractId={}, url={}", contractId, url);
    } catch (Exception e) {
      record.setStatus(DocumentStatus.FAILED);
      record.setErrorMessage(e.getMessage());
      log.error("Échec génération contrat PDF contractId={}", contractId, e);
    }

    return documentRepo.save(record);
  }

  @Override
  @Transactional
  public Document generateInvoice(UUID paymentId, User generatedBy) {
    Payment payment = requirePayment(paymentId);

    Document record = initDocument(DocumentType.INVOICE, paymentId, RefType.PAYMENT, generatedBy);
    record.setTitle("Facture – " + payment.getPeriodLabel() + " – " + payment.getReference());
    record = documentRepo.save(record);

    try {
      byte[] pdf = invoiceGenerator.generate(payment);
      String fileName = "facture-" + payment.getReference() + ".pdf";
      String objectName = "invoices/" + paymentId + "/" + fileName;
      String url =
          minioService.uploadFile(
              BUCKET, objectName, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");

      record.setFileUrl(url);
      record.setFileName(fileName);
      record.setFileSize((long) pdf.length);
      record.setReferenceNumber(generateRef("INV"));
      record.setStatus(DocumentStatus.GENERATED);

      log.info("Facture PDF générée : paymentId={}, url={}", paymentId, url);
    } catch (Exception e) {
      record.setStatus(DocumentStatus.FAILED);
      record.setErrorMessage(e.getMessage());
      log.error("Échec génération facture paymentId={} : {}", paymentId, e.getMessage());
    }

    return documentRepo.save(record);
  }

  @Override
  @Transactional
  public Document generateReceipt(UUID paymentId, User generatedBy) {
    Payment payment = requirePayment(paymentId);

    Document record = initDocument(DocumentType.RECEIPT, paymentId, RefType.PAYMENT, generatedBy);
    record.setTitle("Reçu – " + payment.getPeriodLabel() + " – " + payment.getReference());
    record = documentRepo.save(record);

    try {
      byte[] pdf = receiptGenerator.generate(payment);
      String fileName = "recu-" + payment.getReference() + ".pdf";
      String objectName = "receipts/" + paymentId + "/" + fileName;
      String url =
          minioService.uploadFile(
              BUCKET, objectName, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");

      record.setFileUrl(url);
      record.setFileName(fileName);
      record.setFileSize((long) pdf.length);
      record.setReferenceNumber(generateRef("RCP"));
      record.setStatus(DocumentStatus.GENERATED);

      log.info("Reçu PDF généré : paymentId={}, url={}", paymentId, url);
    } catch (Exception e) {
      record.setStatus(DocumentStatus.FAILED);
      record.setErrorMessage(e.getMessage());
      log.error("Échec génération reçu paymentId={} : {}", paymentId, e.getMessage());
    }

    return documentRepo.save(record);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Document> getDocumentsByRef(UUID refId, RefType refType) {
    return documentRepo.findByRefIdAndRefType(refId, refType);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  /**
   * Envoie le reçu PDF par email au locataire associé au paiement.
   *
   * <p>L'envoi est best-effort : un échec ne doit jamais faire échouer la génération du reçu. Si le
   * locataire n'a pas d'email (champ optionnel sur {@code User}), l'envoi est silencieusement
   * ignoré.
   */
  private void sendReceiptEmailToTenant(Payment payment, byte[] pdfBytes, String fileName) {
    if (payment.getTenant() == null
        || payment.getTenant().getUser() == null
        || payment.getTenant().getUser().getEmail() == null) {
      log.info(
          "[DOC] Envoi email reçu ignoré (pas d'email locataire) – paymentId={}", payment.getId());
      return;
    }

    com.africa.ubaxplatform.auth.entity.User tenantUser = payment.getTenant().getUser();
    String email = tenantUser.getEmail();
    String firstName = tenantUser.getFirstName();

    try {
      emailService.sendReceiptEmail(
          email,
          firstName,
          payment.getReference(),
          payment.getPeriodLabel(),
          payment.getPaidDate(),
          payment.getAmountPaid() != null ? payment.getAmountPaid() : payment.getAmount(),
          pdfBytes,
          fileName);
      log.info("[DOC] Email reçu envoyé à {} pour paymentId={}", email, payment.getId());
    } catch (Exception e) {
      // Envoi best-effort : on logge sans faire échouer la transaction
      log.warn(
          "[DOC] Échec envoi email reçu à {} pour paymentId={} : {}",
          email,
          payment.getId(),
          e.getMessage());
    }
  }

  private Payment requirePayment(UUID paymentId) {
    return paymentRepo
        .findById(paymentId)
        .orElseThrow(() -> new NotFoundException("Paiement introuvable : " + paymentId));
  }

  private Document initDocument(DocumentType type, UUID refId, RefType refType, User generatedBy) {
    return Document.builder()
        .docType(type)
        .refId(refId)
        .refType(refType)
        .generatedBy(generatedBy)
        .status(DocumentStatus.PENDING)
        .fileUrl("pending")
        .build();
  }

  private String generateRef(String prefix) {
    String year = String.valueOf(Year.now().getValue());
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    return "DOC-" + year + "-" + prefix + "-" + suffix;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public void onPaymentPaid(PaymentPaidEvent event) {
    Payment payment = event.payment();
    User triggeredBy = event.triggeredBy();

    // Garde anti-doublon : ne pas regénérer si un reçu GENERATED existe déjà pour ce paiement
    boolean receiptAlreadyGenerated =
        documentRepo.findByRefIdAndRefType(payment.getId(), RefType.PAYMENT).stream()
            .anyMatch(
                d ->
                    d.getDocType() == DocumentType.RECEIPT
                        && d.getStatus() == DocumentStatus.GENERATED);
    if (receiptAlreadyGenerated) {
      log.warn("[DOC] Reçu déjà généré pour paymentId={}, génération ignorée.", payment.getId());
      return;
    }

    log.info("[DOC] Génération automatique du reçu pour paymentId={}", payment.getId());

    Document record =
        initDocument(DocumentType.RECEIPT, payment.getId(), RefType.PAYMENT, triggeredBy);
    record.setTitle("Reçu – " + payment.getPeriodLabel() + " – " + payment.getReference());
    record = documentRepo.save(record);

    try {
      byte[] pdf = receiptGenerator.generate(payment);
      String fileName = "recu-" + payment.getReference() + ".pdf";
      String objectName = "recus/" + payment.getId() + "/" + fileName;
      String url =
          minioService.uploadFile(
              BUCKET, objectName, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");

      record.setFileUrl(url);
      record.setFileName(fileName);
      record.setFileSize((long) pdf.length);
      record.setReferenceNumber(generateRef("RCP"));
      record.setStatus(DocumentStatus.GENERATED);

      // Mettre à jour le receiptUrl sur le payment
      payment.setReceiptUrl(url);
      paymentRepo.save(payment);

      log.info("[DOC] Reçu généré automatiquement : paymentId={}, url={}", payment.getId(), url);

      // Envoyer le reçu par email au locataire (si email disponible)
      sendReceiptEmailToTenant(payment, pdf, fileName);

    } catch (Exception e) {
      record.setStatus(DocumentStatus.FAILED);
      record.setErrorMessage(e.getMessage());
      log.error(
          "[DOC] Échec génération reçu automatique paymentId={} : {}",
          payment.getId(),
          e.getMessage());
    }

    documentRepo.save(record);
  }
}
