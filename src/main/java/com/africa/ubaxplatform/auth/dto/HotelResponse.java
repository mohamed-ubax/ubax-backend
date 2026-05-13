package com.africa.ubaxplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Hôtel partenaire disponible sur la plateforme UBAX")
public record HotelResponse(
    @Schema(description = "Identifiant unique de l'hôtel") UUID id,
    @Schema(description = "Nom de l'hôtel") String name,
    @Schema(description = "Description de l'hôtel", nullable = true) String description,
    @Schema(description = "URL du logo", nullable = true) String logoUrl,
    @Schema(description = "Adresse", nullable = true) String address,
    @Schema(description = "Ville", example = "Dakar") String city,
    @Schema(description = "Code pays ISO 3166-1 alpha-2", example = "SN") String country,
    @Schema(description = "Téléphone", nullable = true) String phone,
    @Schema(description = "Email", nullable = true) String email,
    @Schema(description = "Site web", nullable = true) String website,
    @Schema(description = "Nombre d'étoiles (1-5)", nullable = true) Integer stars,
    @Schema(description = "Nombre total de chambres", nullable = true) Integer totalRooms,
    @Schema(description = "Hôtel certifié par UBAX") boolean verified) {}
