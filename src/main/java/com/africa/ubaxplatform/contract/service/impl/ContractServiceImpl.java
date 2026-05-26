package com.africa.ubaxplatform.contract.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
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
import com.africa.ubaxplatform.contract.mapper.PropertyToContractMapper;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.contract.service.interfaces.ContractService;
import com.africa.ubaxplatform.document.service.interfaces.DocumentService;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
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
    User owner = property.getOwner();

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

    // ── Unicité — un bien ne peut avoir qu'un seul contrat ACTIVE ou PENDING_SIGNATURE ───────────
    List<ContractStatus> blockingStatuses =
        List.of(ContractStatus.ACTIVE, ContractStatus.PENDING_SIGNATURE);

    if (contractRepo.existsByPropertyIdAndStatusIn(property.getId(), blockingStatuses)) {
      throw new CustomException(
          new ConflictException(
              "Ce bien possède déjà un contrat actif ou en attente de signature."
                  + " Il doit être résilié ou annulé avant d'en créer un nouveau."),
          ResponseMessageConstants.CONTRACT_CREATE_FAILURE_PROPERTY_CONFLICT);
    }

    if (req.getTenantId() != null
        && contractRepo.existsByPropertyIdAndTenantIdAndStatusIn(
            property.getId(),
            req.getTenantId(),
            List.of(
                ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE))) {
      throw new CustomException(
          new ConflictException(
              "Ce locataire a déjà un contrat en cours sur ce bien (DRAFT, PENDING_SIGNATURE ou ACTIVE)."),
          ResponseMessageConstants.CONTRACT_CREATE_FAILURE_TENANT_CONFLICT);
    }

    // ── Auto-fill & validation cohérence ──────────────────────────────────────
    // Valider la cohérence entre le transactionType du bien et le contractType proposé
    validatePropertyToContractCoherence(property, req.getContractType());

    // Auto-remplir les champs manquants depuis la propriété
    autoFillFromProperty(req, property);

    validateByType(req);

    // RENT_TO_OWN : si endDate absent, le calculer depuis durationYears
    LocalDate resolvedEndDate = req.getEndDate();
    if (resolvedEndDate == null
        && req.getDurationYears() != null
        && Constants.CodeList.ContractType.RENT_TO_OWN.equals(req.getContractType())) {
      resolvedEndDate = req.getStartDate().plusYears(req.getDurationYears());
      log.debug(
          "RENT_TO_OWN : endDate calculée depuis durationYears={} → {}",
          req.getDurationYears(),
          resolvedEndDate);
    }

    // Pour un bail LEASE, le paymentDay est le jour du mois d'entrée du locataire
    // (ex: entrée le 14 → loyer dû le 14 de chaque mois). L'agent ne le saisit pas.
    int paymentDay;
    if (Constants.CodeList.ContractType.LEASE.equals(req.getContractType())
        || Constants.CodeList.ContractType.RENT_TO_OWN.equals(req.getContractType())) {
      paymentDay = req.getStartDate().getDayOfMonth();
    } else {
      paymentDay = req.getPaymentDay() != null ? req.getPaymentDay() : 5;
    }

    Contract contract =
        Contract.builder()
            .property(property)
            .owner(owner)
            .tenant(tenant)
            .createdBy(caller)
            .contractType(req.getContractType())
            .startDate(req.getStartDate())
            .endDate(resolvedEndDate)
            .monthlyRent(req.getMonthlyRent())
            .monthlyCharges(req.getMonthlyCharges())
            .depositAmount(req.getDepositAmount())
            .salePrice(req.getSalePrice())
            .monthlyInstallment(req.getMonthlyInstallment())
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

    if (caller.getHotel() != null) {
      return contractRepo
          .searchByHotel(caller.getHotel().getId(), status, normalizedSearch, pageable)
          .map(ContractMapper::toResponse);
    }

    return contractRepo
        .searchByOwner(caller.getId(), status, normalizedSearch, pageable)
        .map(ContractMapper::toResponse);
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
    } else if (caller.getHotel() != null) {
      rows = contractRepo.countByHotelGroupByStatus(caller.getHotel().getId());
    } else {
      rows = contractRepo.countByOwnerGroupByStatus(caller.getId());
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
    User caller = requireUser(keycloakId);
    Contract contract = requireContract(id);
    requireTransition(contract, ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE);
    contract.setStatus(ContractStatus.ACTIVE);
    Contract saved = contractRepo.save(contract);
    log.info("Contrat {} → ACTIVE", id);

    // Marquer le bien comme RESERVED (retiré des annonces publiques PUBLISHED)
    setPropertyStatus(saved, PropertyStatus.RESERVED);

    // Pour les baux LEASE, créer immédiatement le premier loyer (avec l'agent qui active)
    if (Constants.CodeList.ContractType.LEASE.equals(saved.getContractType())
        && saved.getMonthlyRent() != null) {
      createFirstRentPayment(saved, caller);
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
    Contract saved = contractRepo.save(contract);
    log.info("Contrat {} → TERMINATED ({})", id, req.getTerminationReason());

    // Remettre le bien en PUBLISHED (disponible à nouveau sur le portail public)
    setPropertyStatus(saved, PropertyStatus.PUBLISHED);

    return ContractMapper.toResponse(saved);
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
   * <p>La date d'échéance = {@code startDate + 1 mois}, conformément au droit locatif UEMOA : le
   * locataire dispose d'un mois complet avant sa première échéance, quel que soit le jour d'entrée.
   *
   * <p>Exemples :
   *
   * <ul>
   *   <li>Entrée le 14 novembre → premier loyer le 14 décembre
   *   <li>Entrée le 3 mars → premier loyer le 3 avril
   *   <li>Entrée le 31 janvier → premier loyer le 28 février (dernier jour du mois)
   * </ul>
   *
   * <p>Idempotent : si un paiement existe déjà pour cette échéance, rien n'est créé.
   */
  private void createFirstRentPayment(Contract contract, User recordedBy) {
    LocalDate start = contract.getStartDate();

    // Premier loyer = 1 mois après l'entrée (Java gère automatiquement les mois courts)
    LocalDate dueDate = start.plusMonths(1);

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
            .recordedBy(recordedBy)
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

  /**
   * Met à jour le statut du bien lié au contrat.
   *
   * <p>Transitions pilotées automatiquement par le cycle de vie du contrat :
   *
   * <ul>
   *   <li>{@code ACTIVE} → bien passe en {@code RESERVED} (retiré des annonces publiques)
   *   <li>{@code TERMINATED} → bien repasse en {@code PUBLISHED} (à nouveau disponible)
   * </ul>
   *
   * <p>Appel best-effort : un échec ne bloque pas la transition du contrat.
   */
  private void setPropertyStatus(Contract contract, PropertyStatus newStatus) {
    if (contract.getProperty() == null) return;
    try {
      Property property = contract.getProperty();
      PropertyStatus previous = property.getStatus();
      property.setStatus(newStatus);
      propertyRepo.save(property);
      log.info(
          "[CONTRACT] Bien {} : {} → {} (contractId={})",
          property.getId(),
          previous,
          newStatus,
          contract.getId());
    } catch (Exception e) {
      log.error(
          "[CONTRACT] Échec mise à jour statut bien pour contractId={} : {}",
          contract.getId(),
          e.getMessage());
    }
  }

  private void validateByType(CreateContractRequest req) throws CustomException {
    String type = req.getContractType();
    switch (type) {
      case Constants.CodeList.ContractType.MANDATE ->
          throw new CustomException(
              new BadRequestException(
                  "Le type MANDATE ne peut pas être créé via cet endpoint. "
                      + "Utilisez POST /v1/mandates pour créer un mandat de gestion agence↔bailleur."),
              ResponseMessageConstants.CONTRACT_CREATE_FAILURE);
      case Constants.CodeList.ContractType.LEASE -> {
        requireField(req.getTenantId(), "tenantId est requis pour un bail LEASE");
        requireField(req.getMonthlyRent(), "monthlyRent est requis pour un bail LEASE");
      }
      case Constants.CodeList.ContractType.SALE -> {
        requireField(
            req.getTenantId(), "tenantId (acheteur) est requis pour un acte de vente SALE");
        requireField(req.getSalePrice(), "salePrice est requis pour un acte de vente SALE");
      }
      case Constants.CodeList.ContractType.RENT_TO_OWN -> {
        requireField(req.getTenantId(), "tenantId est requis pour un contrat RENT_TO_OWN");
        requireField(req.getSalePrice(), "salePrice (prix total) est requis pour RENT_TO_OWN");
        requireField(req.getMonthlyInstallment(), "monthlyInstallment est requis pour RENT_TO_OWN");
        if (req.getEndDate() == null && req.getDurationYears() == null) {
          requireField(
              null,
              "endDate ou durationYears est obligatoire pour RENT_TO_OWN"
                  + " (ex: 5 ans → durationYears=5)");
        }
      }
      case Constants.CodeList.ContractType.RESERVATION ->
          requireField(
              req.getReservationDeposit(), "reservationDeposit est requis pour une RESERVATION");
      default -> {
        // MANDATE : aucun champ financier obligatoire
      }
    }
  }

  private void requireField(Object value, String message) throws CustomException {
    if (value == null) {
      throw new CustomException(
          new BadRequestException(message), ResponseMessageConstants.CONTRACT_CREATE_FAILURE);
    }
  }

  /**
   * Valide la cohérence entre le transactionType du bien et le contractType proposé.
   *
   * <p>Permet de détecter les incohérences métier : par exemple, un bien en SALE ne peut pas être
   * contracté en LEASE.
   *
   * @param property le bien immobilier lié au contrat
   * @param contractType le type de contrat proposé
   * @throws CustomException si incohérence détectée
   */
  private void validatePropertyToContractCoherence(Property property, String contractType)
      throws CustomException {
    String propTransactionType = property.getTransactionType();

    if (!PropertyToContractMapper.isCoherent(propTransactionType, contractType)) {
      String message =
          "Incohérence métier : bien en "
              + propTransactionType
              + " ne peut pas être contracté en "
              + contractType;
      throw new CustomException(
          new BadRequestException(message), ResponseMessageConstants.CONTRACT_CREATE_FAILURE);
    }
  }

  /**
   * Auto-remplit les champs financiers du contrat depuis les valeurs du bien.
   *
   * <p>Logique :
   *
   * <ul>
   *   <li>Si salePrice manquant et bien en SALE : copie property.price
   *   <li>Si monthlyRent manquant et bien en RENT/RENT_FURNISHED : copie property.price
   *   <li>Préserve les valeurs explicitement fournies par le client (ne surcharge pas)
   * </ul>
   *
   * @param req la requête de création de contrat (modifiée en place)
   * @param property le bien immobilier lié au contrat
   */
  private void autoFillFromProperty(CreateContractRequest req, Property property) {
    String propTransactionType = property.getTransactionType();

    // Auto-fill prix selon le type de transaction du bien
    if (Constants.CodeList.TransactionType.SALE.equals(propTransactionType)) {
      if (req.getSalePrice() == null) {
        req.setSalePrice(property.getPrice());
        log.debug("Auto-fill salePrice={} depuis property.price", property.getPrice());
      }
    } else if (Constants.CodeList.TransactionType.RENT.equals(propTransactionType)
        || Constants.CodeList.TransactionType.RENT_FURNISHED.equals(propTransactionType)) {
      if (req.getMonthlyRent() == null) {
        req.setMonthlyRent(property.getPrice());
        log.debug("Auto-fill monthlyRent={} depuis property.price", property.getPrice());
      }
    } else if (Constants.CodeList.TransactionType.SHORT_STAY.equals(propTransactionType)) {
      // SHORT_STAY → RESERVATION, pas de prix auto à copier de prime abord
    }
  }

  private String generateReference(Contract c) {
    String type = c.getContractType() != null ? c.getContractType().substring(0, 3) : "CTR";
    String year = String.valueOf(Year.now().getValue());
    String suffix = c.getId().toString().replace("-", "").substring(0, 6).toUpperCase();
    return "CTR-" + year + "-" + type + "-" + suffix;
  }
}
