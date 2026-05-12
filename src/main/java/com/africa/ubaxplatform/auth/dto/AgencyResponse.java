package com.africa.ubaxplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Agence immobilière disponible sur la plateforme UBAX")
public record AgencyResponse(
    @Schema(description = "Identifiant unique de l'agence") UUID id,
    @Schema(description = "Nom commercial de l'agence") String name,
    @Schema(description = "Description des services de l'agence", nullable = true)
        String description,
    @Schema(description = "URL du logo de l'agence", nullable = true) String logoUrl,
    @Schema(description = "Adresse du siège social", nullable = true) String address,
    @Schema(description = "Ville d'implantation principale", example = "Dakar") String city,
    @Schema(description = "Code pays ISO 3166-1 alpha-2", example = "SN") String country,
    @Schema(description = "Numéro de téléphone professionnel", nullable = true) String phone,
    @Schema(description = "Email professionnel", nullable = true) String email,
    @Schema(description = "Site web officiel", nullable = true) String website,
    @Schema(description = "Agence certifiée par UBAX") boolean verified) {}
