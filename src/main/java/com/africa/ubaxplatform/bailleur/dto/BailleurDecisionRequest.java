package com.africa.ubaxplatform.bailleur.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BailleurDecisionRequest {

  @NotNull(message = "La décision est obligatoire")
  private Decision decision;

  private String comment;

  public enum Decision {
    APPROVE,
    REJECT
  }
}
