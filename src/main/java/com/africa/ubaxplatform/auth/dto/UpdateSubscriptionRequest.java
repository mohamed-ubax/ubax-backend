package com.africa.ubaxplatform.auth.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record UpdateSubscriptionRequest(
    @NotBlank String subscriptionPlan, @Future LocalDateTime subscriptionExpiresAt) {}
