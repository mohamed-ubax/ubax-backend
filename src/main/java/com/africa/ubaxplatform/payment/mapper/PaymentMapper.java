package com.africa.ubaxplatform.payment.mapper;

import com.africa.ubaxplatform.payment.dto.ExpenseResponse;
import com.africa.ubaxplatform.payment.dto.PaymentResponse;
import com.africa.ubaxplatform.payment.entity.Expense;
import com.africa.ubaxplatform.payment.entity.Payment;

public class PaymentMapper {

  private PaymentMapper() {}

  public static PaymentResponse toResponse(Payment p) {
    return new PaymentResponse(
        p.getId(),
        p.getContract() != null ? p.getContract().getId() : null,
        p.getTenant() != null ? p.getTenant().getId() : null,
        p.getAgency() != null ? p.getAgency().getId() : null,
        p.getProperty() != null ? p.getProperty().getId() : null,
        p.getRecordedBy() != null ? p.getRecordedBy().getId() : null,
        p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : null,
        p.getPaymentType(),
        p.getPaymentMethod(),
        p.getStatus(),
        p.getAmount(),
        p.getAmountPaid(),
        p.getDueDate(),
        p.getPaidDate(),
        p.getPeriodLabel(),
        p.getReference(),
        p.getReceiptUrl(),
        p.getNote(),
        p.isOverdue(),
        p.getCreatedAt(),
        p.getUpdatedAt());
  }

  public static ExpenseResponse toResponse(Expense e) {
    return new ExpenseResponse(
        e.getId(),
        e.getAgency().getId(),
        e.getProperty() != null ? e.getProperty().getId() : null,
        e.getCreatedBy() != null ? e.getCreatedBy().getId() : null,
        e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null,
        e.getCategory(),
        e.getCostCenter(),
        e.getLabel(),
        e.getAmount(),
        e.getPaymentMethod(),
        e.getExpenseDate(),
        e.getProvider(),
        e.getInvoiceReference(),
        e.getJustificationUrl(),
        e.getNote(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
