package com.africa.ubaxplatform.payment.service.impl;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.dto.PaymentCreateRequest;
import com.africa.ubaxplatform.payment.dto.PaymentResponse;
import com.africa.ubaxplatform.payment.dto.PaymentStatusUpdateRequest;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.event.PaymentPaidEvent;
import com.africa.ubaxplatform.payment.mapper.PaymentMapper;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.payment.service.interfaces.PaymentService;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import com.africa.ubaxplatform.tenant.repository.TenantRepository;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepo;
  private final UserRepository userRepo;
  private final PropertyRepository propertyRepo;
  private final TenantRepository tenantRepo;
  private final ContractRepository contractRepo;
  private final ApplicationEventPublisher eventPublisher;

  // ── Helpers ────────────────────────────────────────────────────

  private User resolveUser(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private Payment resolvePayment(UUID id) throws CustomException {
    return paymentRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Paiement introuvable"),
                    ResponseMessageConstants.PAYMENT_GET_FAILURE_NOT_FOUND));
  }

  private Agency requireAgency(User caller) throws CustomException {
    Agency agency = caller.getAgency();
    if (agency == null) {
      throw new CustomException(
          new BadRequestException("Aucune agence associée à ce compte"),
          ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
    }
    return agency;
  }

  private void requireSameAgency(Payment payment, User caller) throws CustomException {
    if (payment.getAgency() == null) return;
    Agency callerAgency = caller.getAgency();
    if (callerAgency == null || !callerAgency.getId().equals(payment.getAgency().getId())) {
      throw new CustomException(
          new UnAuthorizedException("Accès refusé – paiement appartenant à une autre agence"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  private String generateReference(PaymentType type) {
    String short_ = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return "PAY-%d-%s-%s".formatted(Year.now().getValue(), type.name(), short_);
  }

  // ── CRUD ───────────────────────────────────────────────────────

  @Override
  @Transactional
  public PaymentResponse create(String callerKeycloakId, PaymentCreateRequest req)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Agency agency = requireAgency(caller);

    Contract contract = null;
    if (req.contractId() != null) {
      contract =
          contractRepo
              .findById(req.contractId())
              .orElseThrow(
                  () ->
                      new CustomException(
                          new NotFoundException("Contrat introuvable"),
                          ResponseMessageConstants.CONTRACT_GET_FAILURE_NOT_FOUND));
    }

    Tenant tenant = null;
    if (req.tenantId() != null) {
      tenant =
          tenantRepo
              .findById(req.tenantId())
              .orElseThrow(
                  () ->
                      new CustomException(
                          new NotFoundException("Locataire introuvable"),
                          ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND));
    }

    Property property = null;
    if (req.propertyId() != null) {
      property =
          propertyRepo
              .findById(req.propertyId())
              .orElseThrow(
                  () ->
                      new CustomException(
                          new NotFoundException("Bien introuvable"),
                          ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND));
    }

    // Déterminer le statut initial selon les données fournies
    PaymentStatus initialStatus;
    if (req.paidDate() != null && req.amountPaid() != null) {
      initialStatus =
          req.amountPaid().compareTo(req.amount()) >= 0
              ? PaymentStatus.PAID
              : PaymentStatus.PARTIAL;
    } else if (req.dueDate().isBefore(LocalDate.now())) {
      initialStatus = PaymentStatus.LATE;
    } else {
      initialStatus = PaymentStatus.PENDING;
    }

    String reference =
        (req.reference() != null && !req.reference().isBlank())
            ? req.reference()
            : generateReference(req.paymentType());

    Payment payment =
        Payment.builder()
            .contract(contract)
            .tenant(tenant)
            .agency(agency)
            .property(property)
            .recordedBy(caller)
            .paymentType(req.paymentType())
            .paymentMethod(req.paymentMethod())
            .status(initialStatus)
            .amount(req.amount())
            .amountPaid(req.amountPaid())
            .dueDate(req.dueDate())
            .paidDate(req.paidDate())
            .periodLabel(req.periodLabel())
            .reference(reference)
            .receiptUrl(req.receiptUrl())
            .note(req.note())
            .build();
    Payment saved = paymentRepo.save(payment);
    log.info("La confirmation a ete validee avec Succés");
    if (saved.getStatus() == PaymentStatus.PAID) {
      eventPublisher.publishEvent(new PaymentPaidEvent(saved, caller));
      log.info("La génération du recu à reussie avec avec Succés");
    }

    return PaymentMapper.toResponse(saved);
  }

  @Override
  public PaymentResponse getById(UUID id) throws CustomException {
    return PaymentMapper.toResponse(resolvePayment(id));
  }

  @Override
  public Page<PaymentResponse> list(
      String callerKeycloakId,
      PaymentStatus status,
      PaymentType type,
      UUID propertyId,
      UUID contractId,
      UUID tenantId,
      LocalDate from,
      LocalDate to,
      Pageable pageable)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);

    // ADMIN / SUPER_ADMIN voient tout (agencyId = null → aucun filtre sur l'agence)
    UUID agencyId = (caller.getAgency() != null) ? caller.getAgency().getId() : null;

    return paymentRepo
        .findWithFilters(
            agencyId, status, type, propertyId, contractId, tenantId, from, to, pageable)
        .map(PaymentMapper::toResponse);
  }

  @Override
  public List<PaymentResponse> listLate(String callerKeycloakId) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Agency agency = requireAgency(caller);

    return paymentRepo.findLateByAgency(agency.getId(), LocalDate.now()).stream()
        .map(PaymentMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public PaymentResponse updateStatus(
      UUID id, String callerKeycloakId, PaymentStatusUpdateRequest req) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Payment payment = resolvePayment(id);
    requireSameAgency(payment, caller);

    if (payment.getStatus() == PaymentStatus.CANCELLED) {
      throw new CustomException(
          new BadRequestException("Impossible de modifier un paiement annulé"),
          ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
    }

    // Capturer le statut AVANT modification pour détecter la transition → PAID
    PaymentStatus previousStatus = payment.getStatus();

    payment.setStatus(req.status());
    if (req.paymentMethod() != null) payment.setPaymentMethod(req.paymentMethod());
    if (req.amountPaid() != null) payment.setAmountPaid(req.amountPaid());
    if (req.paidDate() != null) payment.setPaidDate(req.paidDate());
    if (req.receiptUrl() != null) payment.setReceiptUrl(req.receiptUrl());
    if (req.note() != null) payment.setNote(req.note());

    // Auto-correction statut PAID si montant complet
    if (req.status() == PaymentStatus.PARTIAL
        && req.amountPaid() != null
        && req.amountPaid().compareTo(payment.getAmount()) >= 0) {
      payment.setStatus(PaymentStatus.PAID);
    }

    Payment saved = paymentRepo.save(payment);

    // Générer le reçu uniquement sur la TRANSITION vers PAID (pas si déjà PAID avant la MAJ)
    if (saved.getStatus() == PaymentStatus.PAID && previousStatus != PaymentStatus.PAID) {
      eventPublisher.publishEvent(new PaymentPaidEvent(saved, caller));
    }

    return PaymentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(UUID id, String callerKeycloakId) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Payment payment = resolvePayment(id);
    requireSameAgency(payment, caller);

    if (payment.getStatus() == PaymentStatus.PAID) {
      throw new CustomException(
          new BadRequestException("Impossible de supprimer un paiement encaissé"),
          ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
    }
    paymentRepo.delete(payment);
  }
}
