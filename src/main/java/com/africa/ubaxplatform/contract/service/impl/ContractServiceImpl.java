package com.africa.ubaxplatform.contract.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.dto.ContractResponse;
import com.africa.ubaxplatform.contract.dto.ContractStatsResponse;
import com.africa.ubaxplatform.contract.dto.CreateContractRequest;
import com.africa.ubaxplatform.contract.dto.TerminateContractRequest;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.mapper.ContractMapper;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.contract.service.interfaces.ContractService;
import com.africa.ubaxplatform.document.service.interfaces.DocumentService;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import com.africa.ubaxplatform.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractServiceImpl implements ContractService {

  private final ContractRepository contractRepo;
  private final UserRepository userRepo;
  private final PropertyRepository propertyRepo;
  private final TenantRepository tenantRepo;
  private final PaymentRepository paymentRepo;
  private final DocumentService documentService;

  @Override
  @Transactional
  public ContractResponse create(String keycloakId, CreateContractRequest req)
      throws CustomException {
    User caller = requireUser(keycloakId);
    Property property = requireProperty(req.getPropertyId());
    User owner = requireUserById(req.getOwnerId());

    Tenant tenant = null;
    if (req.getTenantId() != null) {
      tenant =
          tenantRepo
              .findById(req.getTenantId())
              .orElseThrow(
                  () ->
                      new CustomException(
                          new NotFoundException("Dossier locataire introuvable"),
                          ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND));
    }

    int paymentDay = req.getPaymentDay() != null ? req.getPaymentDay() : 5;

    Contract contract =
        Contract.builder()
            .property(property)
            .owner(owner)
            .tenant(tenant)
            .createdBy(caller)
            .contractType(req.getContractType())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .monthlyRent(req.getMonthlyRent())
            .monthlyCharges(req.getMonthlyCharges())
            .depositAmount(req.getDepositAmount())
            .salePrice(req.getSalePrice())
            .reservationDeposit(req.getReservationDeposit())
            .reservationDurationDays(req.getReservationDurationDays())
            .agencyCommissionRate(req.getAgencyCommissionRate())
            .paymentDay(paymentDay)
            .specialClauses(req.getSpecialClauses())
            .terminationConditions(req.getTerminationConditions())
            .build();

    Contract saved = contractRepo.save(contract);
    saved.setReferenceNumber(generateReference(saved));
    saved = contractRepo.save(saved);

    log.info(
        "Contrat créé : id={}, type={}, property={}",
        saved.getId(),
        req.getContractType(),
        req.getPropertyId());
    return ContractMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContractResponse> list(
      String keycloakId, ContractStatus status, String search, Pageable pageable)
      throws CustomException {
    User caller = requireUser(keycloakId);
    String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

    if (caller.getAgency() != null) {
      return contractRepo
          .searchByAgency(caller.getAgency().getId(), status, normalizedSearch, pageable)
          .map(ContractMapper::toResponse);
    }

    if (caller.getHotel() == null) {
      return contractRepo
          .searchByOwner(caller.getId(), status, normalizedSearch, pageable)
          .map(ContractMapper::toResponse);
    }

    throw new CustomException(
        new BadRequestException("Aucune agence ou hôtel associé au compte"),
        ResponseMessageConstants.CONTRACT_GET_FAILURE_NOT_FOUND);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContractResponse> listAll(ContractStatus status, String search, Pageable pageable) {
    String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
    return contractRepo
        .searchAll(status, normalizedSearch, pageable)
        .map(ContractMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ContractStatsResponse getStats(String keycloakId) throws CustomException {
    User caller = requireUser(keycloakId);

    List<Object[]> rows;
    if (caller.getAgency() != null) {
      rows = contractRepo.countByAgencyGroupByStatus(caller.getAgency().getId());
    } else if (caller.getHotel() == null) {
      rows = contractRepo.countByOwnerGroupByStatus(caller.getId());
    } else {
      rows = contractRepo.countAllGroupByStatus();
    }

    return buildStats(rows);
  }

  @Override
  @Transactional(readOnly = true)
  public ContractResponse getById(String keycloakId, UUID id) throws CustomException {
    Contract contract = requireContract(id);
    checkReadAccess(requireUser(keycloakId), contract);
    return ContractMapper.toResponse(contract);
  }

  @Override
  @Transactional
  public ContractResponse update(String keycloakId, UUID id, CreateContractRequest req)
      throws CustomException {
    User caller = requireUser(keycloakId);
    Contract contract = requireContract(id);
    checkWriteAccess(caller, contract);

    if (contract.getStatus() != ContractStatus.DRAFT) {
      throw new CustomException(
          new BadRequestException("Seul un contrat DRAFT peut être modifié"),
          ResponseMessageConstants.CONTRACT_UPDATE_FAILURE);
    }

    if (req.getMonthlyRent() != null) contract.setMonthlyRent(req.getMonthlyRent());
    if (req.getMonthlyCharges() != null) contract.setMonthlyCharges(req.getMonthlyCharges());
    if (req.getDepositAmount() != null) contract.setDepositAmount(req.getDepositAmount());
    if (req.getSalePrice() != null) contract.setSalePrice(req.getSalePrice());
    if (req.getReservationDeposit() != null)
      contract.setReservationDeposit(req.getReservationDeposit());
    if (req.getReservationDurationDays() != null)
      contract.setReservationDurationDays(req.getReservationDurationDays());
    if (req.getAgencyCommissionRate() != null)
      contract.setAgencyCommissionRate(req.getAgencyCommissionRate());
    if (req.getPaymentDay() != null) contract.setPaymentDay(req.getPaymentDay());
    if (req.getStartDate() != null) contract.setStartDate(req.getStartDate());
    if (req.getEndDate() != null) contract.setEndDate(req.getEndDate());
    if (req.getSpecialClauses() != null) contract.setSpecialClauses(req.getSpecialClauses());
    if (req.getTerminationConditions() != null)
      contract.setTerminationConditions(req.getTerminationConditions());

    return ContractMapper.toResponse(contractRepo.save(contract));
  }

  @Override
  @Transactional
  public ContractResponse submit(String keycloakId, UUID id) throws CustomException {
    User caller = requireUser(keycloakId);
    Contract contract = requireContract(id);
    checkWriteAccess(caller, contract);
    requireTransition(contract, ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE);
    contract.setStatus(ContractStatus.PENDING_SIGNATURE);
    Contract saved = contractRepo.save(contract);
    log.info("Contrat {} → PENDING_SIGNATURE", id);

    try {
      documentService.generateContractPdf(saved.getId(), caller);
    } catch (Exception e) {
      log.error("Échec génération PDF contrat {} : {}", id, e.getMessage());
    }

    return ContractMapper.toResponse(requireContract(id));
  }

  @Override
  @Transactional
  public ContractResponse activate(String keycloakId, UUID id) throws CustomException {
    Contract contract = requireContract(id);
    requireTransition(contract, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE);
    contract.setStatus(ContractStatus.ACTIVE);
    Contract saved = contractRepo.save(contract);
    log.info("Contrat {} → ACTIVE", id);

    // Pour les baux LEASE, créer immédiatement le premier loyer
    if (Constants.CodeList.ContractType.LEASE.equals(saved.getContractType())
        && saved.getMonthlyRent() != null) {
      createFirstRentPayment(saved);
    }

    return ContractMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ContractResponse terminate(String keycloakId, UUID id, TerminateContractRequest req)
      throws CustomException {
    User caller = requireUser(keycloakId);
    Contract contract = requireContract(id);
    checkWriteAccess(caller, contract);
    requireTransition(contract, ContractStatus.ACTIVE, ContractStatus.TERMINATED);
    contract.setStatus(ContractStatus.TERMINATED);
    contract.setTerminatedAt(LocalDateTime.now());
    contract.setTerminationReason(req.getTerminationReason());
    contract.setTerminatedBy(caller);
    log.info("Contrat {} → TERMINATED ({})", id, req.getTerminationReason());
    return ContractMapper.toResponse(contractRepo.save(contract));
  }

  @Override
  @Transactional
  public ContractResponse cancel(String keycloakId, UUID id) throws CustomException {
    User caller = requireUser(keycloakId);
    Contract contract = requireContract(id);
    checkWriteAccess(caller, contract);

    if (contract.getStatus() != ContractStatus.DRAFT
        && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
      throw new CustomException(
          new BadRequestException(
              "Seul un contrat DRAFT ou PENDING_SIGNATURE peut être annulé. Statut actuel : "
                  + contract.getStatus()),
          ResponseMessageConstants.CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION);
    }
    contract.setStatus(ContractStatus.CANCELLED);
    log.info("Contrat {} → CANCELLED", id);
    return ContractMapper.toResponse(contractRepo.save(contract));
  }

  @Override
  @Transactional
  public ContractResponse regeneratePdf(String keycloakId, UUID id) throws CustomException {
    User caller = requireUser(keycloakId);
    Contract contract = requireContract(id);
    documentService.generateContractPdf(contract.getId(), caller);
    log.info("Régénération PDF demandée pour contrat {}", id);
    return ContractMapper.toResponse(requireContract(id));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Crée le premier loyer d'un bail à l'activation du contrat.
   *
   * <p>La date d'échéance = premier {@code paymentDay} survenant APRÈS la {@code startDate}. Cela
   * garantit au moins un cycle complet avant la première échéance, quel que soit le jour d'entrée.
   *
   * <p>Exemples :
   *
   * <ul>
   *   <li>Entrée le 15 mars, paymentDay = 15 → échéance le 15 avril
   *   <li>Entrée le 15 mars, paymentDay = 5 → le 5 mars est déjà passé → échéance le 5 avril
   *   <li>Entrée le 3 mars, paymentDay = 10 → échéance le 10 mars (dans le même mois, 7 jours)
   * </ul>
   *
   * <p>Idempotent : si un paiement existe déjà pour cette échéance, rien n'est créé.
   */
  private void createFirstRentPayment(Contract contract) {
    LocalDate start = contract.getStartDate();
    int payDay = Math.min(contract.getPaymentDay(), start.lengthOfMonth());

    // Première échéance : le prochain paymentDay strictement après startDate
    LocalDate dueDate = start.withDayOfMonth(payDay);
    if (!dueDate.isAfter(start)) {
      LocalDate next = start.plusMonths(1);
      dueDate = next.withDayOfMonth(Math.min(contract.getPaymentDay(), next.lengthOfMonth()));
    }

    if (paymentRepo.existsByContractIdAndDueDate(contract.getId(), dueDate)) {
      log.debug("Premier loyer déjà existant pour contrat {}, ignoré.", contract.getId());
      return;
    }

    LocalDate period = dueDate;
    String month = period.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
    String periodLabel =
        Character.toUpperCase(month.charAt(0)) + month.substring(1) + " " + period.getYear();

    String reference =
        "PAY-%d-%02d-RENT-%s"
            .formatted(
                period.getYear(),
                period.getMonthValue(),
                contract.getId().toString().substring(0, 8).toUpperCase());

    BigDecimal amount =
        contract.getTotalMonthlyAmount() != null
            ? contract.getTotalMonthlyAmount()
            : contract.getMonthlyRent();

    Payment payment =
        Payment.builder()
            .contract(contract)
            .tenant(contract.getTenant())
            .property(contract.getProperty())
            .agency(contract.getProperty() != null ? contract.getProperty().getAgency() : null)
            .paymentType(PaymentType.RENT)
            .status(PaymentStatus.PENDING)
            .amount(amount)
            .dueDate(dueDate)
            .periodLabel(periodLabel)
            .reference(reference)
            .build();

    paymentRepo.save(payment);
    log.info("Premier loyer créé pour contrat {} : dueDate={}", contract.getId(), dueDate);
  }

  private ContractStatsResponse buildStats(List<Object[]> rows) {
    long active = 0, pending = 0, terminated = 0;
    for (Object[] row : rows) {
      ContractStatus status = (ContractStatus) row[0];
      long count = (Long) row[1];
      switch (status) {
        case ACTIVE -> active += count;
        case DRAFT, PENDING_SIGNATURE -> pending += count;
        case TERMINATED, CANCELLED, EXPIRED -> terminated += count;
      }
    }
    return new ContractStatsResponse(active + pending + terminated, active, pending, terminated);
  }

  private User requireUser(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private User requireUserById(UUID userId) throws CustomException {
    return userRepo
        .findById(userId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private Property requireProperty(UUID id) throws CustomException {
    return propertyRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Bien introuvable"),
                    ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND));
  }

  private Contract requireContract(UUID id) throws CustomException {
    return contractRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Contrat introuvable"),
                    ResponseMessageConstants.CONTRACT_GET_FAILURE_NOT_FOUND));
  }

  private void requireTransition(Contract c, ContractStatus from, ContractStatus to)
      throws CustomException {
    if (c.getStatus() != from) {
      throw new CustomException(
          new BadRequestException(
              "Transition invalide : statut actuel "
                  + c.getStatus()
                  + ", attendu "
                  + from
                  + " pour passer à "
                  + to),
          ResponseMessageConstants.CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION);
    }
  }

  private void checkReadAccess(User caller, Contract contract) throws CustomException {
    if (caller.getAgency() != null
        && contract.getProperty().getAgency() != null
        && caller.getAgency().getId().equals(contract.getProperty().getAgency().getId())) return;
    if (contract.getOwner() != null && contract.getOwner().getId().equals(caller.getId())) return;
    if (contract.getCreatedBy() != null && contract.getCreatedBy().getId().equals(caller.getId()))
      return;
    throw new CustomException(
        new UnAuthorizedException("Accès refusé"), ResponseMessageConstants.USER_FORBIDDEN);
  }

  private void checkWriteAccess(User caller, Contract contract) throws CustomException {
    checkReadAccess(caller, contract);
  }

  private String generateReference(Contract c) {
    String type = c.getContractType() != null ? c.getContractType().substring(0, 3) : "CTR";
    String year = String.valueOf(Year.now().getValue());
    String suffix = c.getId().toString().replace("-", "").substring(0, 6).toUpperCase();
    return "CTR-" + year + "-" + type + "-" + suffix;
  }
}
