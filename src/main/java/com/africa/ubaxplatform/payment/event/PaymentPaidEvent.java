package com.africa.ubaxplatform.payment.event;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.payment.entity.Payment;

public record PaymentPaidEvent(Payment payment, User triggeredBy) {}
