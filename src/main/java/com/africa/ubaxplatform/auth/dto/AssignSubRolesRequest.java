package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Requête d'assignation de sous-rôles à un utilisateur. */
@Getter
@Setter
public class AssignSubRolesRequest {

  @NotNull(message = "Le scope est obligatoire")
  @Schema(
      description = "Portée des sous-rôles",
      allowableValues = {"UBAX_INTERNAL", "AGENCE", "HOTEL"},
      example = "AGENCE")
  private RoleScope scope;

  @NotEmpty(message = "Au moins un rôle est requis")
  @Schema(
      description =
          "Liste des sous-rôles à assigner selon le scope :\n"
              + "- UBAX_INTERNAL : DIRECTEUR_GENERAL, SUPPORT_CLIENT, OPERATIONS, FINANCE, COMMERCIAL\n"
              + "- AGENCE : DIRECTEUR_AGENCE, COMMERCIAL, COMPTABLE_AGENCE, AGENT_SAV\n"
              + "- HOTEL : GERANT_HOTEL, RECEPTIONNISTE, COMPTABLE_HOTEL, RESPONSABLE_HEBERGEMENT",
      example = "[\"DIRECTEUR_AGENCE\"]")
  private List<String> roles;
}
