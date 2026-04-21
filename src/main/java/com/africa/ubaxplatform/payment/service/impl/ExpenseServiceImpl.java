package com.africa.ubaxplatform.payment.service.impl;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.payment.codeList.CostCenter;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.dto.ExpenseCreateRequest;
import com.africa.ubaxplatform.payment.dto.ExpenseResponse;
import com.africa.ubaxplatform.payment.entity.Expense;
import com.africa.ubaxplatform.payment.mapper.PaymentMapper;
import com.africa.ubaxplatform.payment.repository.ExpenseRepository;
import com.africa.ubaxplatform.payment.service.interfaces.ExpenseService;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import java.time.LocalDate;
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
public class ExpenseServiceImpl implements ExpenseService {

  private final ExpenseRepository expenseRepo;
  private final UserRepository userRepo;
  private final PropertyRepository propertyRepo;

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

  private Expense resolveExpense(UUID id) throws CustomException {
    return expenseRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Dépense introuvable"),
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

  private void requireSameAgency(Expense expense, User caller) throws CustomException {
    Agency callerAgency = caller.getAgency();
    if (callerAgency == null
        || !callerAgency.getId().equals(expense.getAgency().getId())) {
      throw new CustomException(
          new UnAuthorizedException("Accès refusé – dépense appartenant à une autre agence"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  // ── CRUD ───────────────────────────────────────────────────────

  @Override
  @Transactional
  public ExpenseResponse create(String callerKeycloakId, ExpenseCreateRequest req)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Agency agency = requireAgency(caller);

    if (req.costCenter() == CostCenter.PROPERTY_SPECIFIC && req.propertyId() == null) {
      throw new CustomException(
          new BadRequestException(
              "propertyId est obligatoire pour le centre de coût PROPERTY_SPECIFIC"),
          ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
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

    Expense expense =
        Expense.builder()
            .agency(agency)
            .property(property)
            .createdBy(caller)
            .category(req.category())
            .costCenter(req.costCenter())
            .label(req.label())
            .amount(req.amount())
            .paymentMethod(req.paymentMethod())
            .expenseDate(req.expenseDate())
            .provider(req.provider())
            .invoiceReference(req.invoiceReference())
            .justificationUrl(req.justificationUrl())
            .note(req.note())
            .build();

    return PaymentMapper.toResponse(expenseRepo.save(expense));
  }

  @Override
  public ExpenseResponse getById(UUID id, String callerKeycloakId) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Expense expense = resolveExpense(id);

    if (caller.getAgency() != null) {
      requireSameAgency(expense, caller);
    }
    return PaymentMapper.toResponse(expense);
  }

  @Override
  public Page<ExpenseResponse> list(
      String callerKeycloakId,
      ExpenseCategory category,
      UUID propertyId,
      LocalDate from,
      LocalDate to,
      Pageable pageable)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Agency agency = requireAgency(caller);

    return expenseRepo
        .findWithFilters(agency.getId(), category, propertyId, from, to, pageable)
        .map(PaymentMapper::toResponse);
  }

  @Override
  @Transactional
  public void delete(UUID id, String callerKeycloakId) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Expense expense = resolveExpense(id);

    if (caller.getAgency() != null) {
      requireSameAgency(expense, caller);
    }
    expenseRepo.delete(expense);
  }
}
