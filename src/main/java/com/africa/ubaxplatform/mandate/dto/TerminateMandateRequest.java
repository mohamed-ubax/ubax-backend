package com.africa.ubaxplatform.mandate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminateMandateRequest {

  @NotBlank(message = "terminationReason est obligatoire")
  private String terminationReason;
}
