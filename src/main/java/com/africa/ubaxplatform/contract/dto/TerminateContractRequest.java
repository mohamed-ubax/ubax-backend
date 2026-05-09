package com.africa.ubaxplatform.contract.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminateContractRequest {

  @NotBlank(message = "Le motif de résiliation est obligatoire")
  private String terminationReason;
}
