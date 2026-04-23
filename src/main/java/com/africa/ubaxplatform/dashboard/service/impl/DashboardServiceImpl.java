package com.africa.ubaxplatform.dashboard.service.impl;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.dashboard.dto.AgencyDashboardResponse;
import com.africa.ubaxplatform.dashboard.dto.ExpenseBreakdownItem;
import com.africa.ubaxplatform.dashboard.dto.RevenueBreakdownItem;
import com.africa.ubaxplatform.dashboard.service.interfaces.DashboardService;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.repository.ExpenseRepository;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

  private final UserRepository userRepo;
  private final PaymentRepository paymentRepo;
  private final ExpenseRepository expenseRepo;
  private final PropertyRepository propertyRepo;
  private final ContractRepository contractRepo;

  @Override
  public AgencyDashboardResponse getAgencyDashboard(
      String callerKeycloakId, LocalDate from, LocalDate to) throws CustomException {

    User caller =
        userRepo
            .findByKeycloakId(callerKeycloakId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Utilisateur introuvable"),
                        ResponseMessageConstants.USER_NOT_FOUND));

    Agency agency = caller.getAgency();
    if (agency == null) {
      throw new CustomException(
          new BadRequestException("Aucune agence associée à ce compte"),
          ResponseMessageConstants.DASHBOARD_GET_FAILURE);
    }

    UUID agencyId = agency.getId();
    LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
    LocalDate effectiveTo = to != null ? to : YearMonth.now().atEndOfMonth();

    // ── Financial KPIs ────────────────────────────────────────────
    BigDecimal totalRevenue =
        paymentRepo.sumPaidByAgencyAndPeriod(agencyId, effectiveFrom, effectiveTo);
    BigDecimal totalExpenses =
        expenseRepo.sumByAgencyAndPeriod(agencyId, effectiveFrom, effectiveTo);
    BigDecimal netRevenue = totalRevenue.subtract(totalExpenses);
    BigDecimal overdueAmount = paymentRepo.sumOverdueByAgency(agencyId, LocalDate.now());
    BigDecimal recoveryRate = computeRecoveryRate(totalRevenue, overdueAmount);

    // ── Portfolio KPIs ────────────────────────────────────────────
    long totalProperties = propertyRepo.countByAgencyId(agencyId);
    long publishedProperties =
        propertyRepo.countByAgencyIdAndStatus(agencyId, PropertyStatus.PUBLISHED);
    long reservedProperties =
        propertyRepo.countByAgencyIdAndStatus(agencyId, PropertyStatus.RESERVED);
    long activeContracts = contractRepo.countByAgencyAndStatus(agencyId, ContractStatus.ACTIVE);
    long pendingPaymentsCount =
        paymentRepo.countByAgencyIdAndStatus(agencyId, PaymentStatus.PENDING);
    long latePaymentsCount = paymentRepo.findLateByAgency(agencyId, LocalDate.now()).size();
    long paidPaymentsCount = paymentRepo.countByAgencyIdAndStatus(agencyId, PaymentStatus.PAID);

    // ── Breakdowns ────────────────────────────────────────────────
    List<RevenueBreakdownItem> revenueByType =
        paymentRepo.sumPaidByTypeAndPeriod(agencyId, effectiveFrom, effectiveTo).stream()
            .map(row -> new RevenueBreakdownItem(row[0].toString(), (BigDecimal) row[1]))
            .toList();

    List<ExpenseBreakdownItem> expensesByCategory =
        expenseRepo.sumByCategoryAndPeriod(agencyId, effectiveFrom, effectiveTo).stream()
            .map(row -> new ExpenseBreakdownItem(row[0].toString(), (BigDecimal) row[1]))
            .toList();

    return new AgencyDashboardResponse(
        effectiveFrom,
        effectiveTo,
        totalRevenue,
        totalExpenses,
        netRevenue,
        overdueAmount,
        recoveryRate,
        totalProperties,
        publishedProperties,
        reservedProperties,
        activeContracts,
        pendingPaymentsCount,
        latePaymentsCount,
        paidPaymentsCount,
        revenueByType,
        expensesByCategory);
  }

  private BigDecimal computeRecoveryRate(BigDecimal revenue, BigDecimal overdue) {
    BigDecimal total = revenue.add(overdue);
    if (total.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.valueOf(100);
    }
    return revenue.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
  }
}
