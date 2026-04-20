package com.africa.ubaxplatform.property.dto;

import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusUpdateRequest(@NotNull PropertyStatus status, String rejectionReason) {}
