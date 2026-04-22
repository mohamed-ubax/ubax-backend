package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Requête d'assignation d'un ticket à un agent SAV ou prestataire interne. */
@Getter
@Setter
public class AssignTicketRequest {

  @NotNull(message = "L'identifiant de l'agent assigné est obligatoire")
  @Schema(description = "Identifiant (DB) de l'agent SAV à qui assigner le ticket")
  private UUID assignedToId;
}
