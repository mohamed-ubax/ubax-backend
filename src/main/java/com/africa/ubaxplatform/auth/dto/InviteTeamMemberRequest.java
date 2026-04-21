package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête pour inviter un utilisateur existant dans l'équipe d'une agence.
 *
 * <p>L'utilisateur ciblé doit déjà posséder un compte UBAX actif et ne pas être déjà rattaché à une
 * autre agence.
 */
@Schema(description = "Données pour rattacher un utilisateur existant à l'équipe de l'agence")
public record InviteTeamMemberRequest(
    @Schema(
            description = "Email du compte UBAX de l'utilisateur à inviter",
            example = "moussa.diallo@email.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,
    @Schema(
            description = "Rôle interne à assigner dans l'agence",
            example = "COMMERCIAL",
            allowableValues = {"DIRECTEUR_AGENCE", "COMMERCIAL", "COMPTABLE_AGENCE", "AGENT_SAV"},
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Le rôle partenaire est obligatoire")
        PartnerRole partnerRole) {}
