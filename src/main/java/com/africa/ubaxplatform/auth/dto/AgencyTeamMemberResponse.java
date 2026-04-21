package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représentation d'un membre de l'équipe agence retournée par l'API.
 *
 * <p>Contient les informations d'identité de l'utilisateur et son rôle interne au sein de l'agence
 * ({@link PartnerRole}).
 */
@Schema(description = "Membre de l'équipe d'une agence partenaire avec son rôle interne")
public record AgencyTeamMemberResponse(
    @Schema(
            description = "Identifiant UUID du membre",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,
    @Schema(description = "Identifiant Keycloak (sub JWT) du membre", example = "a1b2c3d4-...")
        String keycloakId,
    @Schema(description = "Prénom du membre", example = "Moussa") String firstName,
    @Schema(description = "Nom de famille du membre", example = "Diallo") String lastName,
    @Schema(description = "Adresse email professionnelle", example = "moussa.diallo@agence.sn")
        String email,
    @Schema(description = "Numéro de téléphone au format international", example = "+221781234567")
        String phone,
    @Schema(description = "URL de l'avatar (bucket MinIO users-avatars)") String avatarUrl,
    @Schema(
            description = "Rôle interne au sein de l'agence",
            allowableValues = {"DIRECTEUR_AGENCE", "COMMERCIAL", "COMPTABLE_AGENCE", "AGENT_SAV"})
        PartnerRole partnerRole,
    @Schema(description = "Indique si le compte est actif") boolean active,
    @Schema(description = "Date de dernière connexion") LocalDateTime lastLoginAt,
    @Schema(description = "Date d'ajout à l'équipe") LocalDateTime createdAt) {}
