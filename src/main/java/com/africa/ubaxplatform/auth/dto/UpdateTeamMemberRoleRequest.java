package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête pour modifier le rôle interne d'un membre de l'équipe agence.
 *
 * <p>Seul le {@link PartnerRole} est modifiable via cet endpoint. Pour retirer complètement un
 * membre, utiliser {@code DELETE /v1/agency/team/{userId}}.
 */
@Schema(description = "Nouveau rôle interne à affecter au membre de l'équipe")
public record UpdateTeamMemberRoleRequest(
    @Schema(
            description = "Nouveau rôle interne dans l'agence",
            example = "COMPTABLE_AGENCE",
            allowableValues = {"DIRECTEUR_AGENCE", "COMMERCIAL", "COMPTABLE_AGENCE", "AGENT_SAV"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Le rôle partenaire est obligatoire")
        PartnerRole partnerRole) {}
