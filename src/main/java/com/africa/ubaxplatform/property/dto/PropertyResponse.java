package com.africa.ubaxplatform.property.dto;

import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Bien immobilier publié sur la plateforme UBAX")
public record PropertyResponse(
    @Schema(description = "Identifiant unique du bien") UUID id,
    @Schema(description = "Identifiant du propriétaire ou agent") UUID ownerId,
    @Schema(description = "Nom complet du propriétaire ou agent") String ownerName,
    @Schema(
            description = "Identifiant de l'agence gestionnaire (null si particulier)",
            nullable = true)
        UUID agencyId,
    @Schema(description = "Nom de l'agence gestionnaire (null si particulier)", nullable = true)
        String agencyName,
    @Schema(
            description = "Identifiant de l'hôtel propriétaire (null si bien immobilier classique)",
            nullable = true)
        UUID hotelId,
    @Schema(
            description = "Nom de l'hôtel propriétaire (null si bien immobilier classique)",
            nullable = true)
        String hotelName,
    @Schema(description = "Titre accrocheur de l'annonce") String title,
    @Schema(description = "Description complète du bien") String description,
    @Schema(
            description = "Type de bien (APARTMENT, VILLA, HOUSE, LAND, OFFICE…)",
            example = "VILLA")
        String propertyType,
    @Schema(
            description = "Type de transaction (SALE, RENT, RENT_FURNISHED, SHORT_STAY)",
            example = "SALE")
        String transactionType,
    @Schema(description = "Prix de vente ou loyer mensuel en XOF", example = "150000000")
        BigDecimal price,
    @Schema(description = "État général du bien (NEW, GOOD, RENOVATE)", example = "GOOD")
        String condition,
    @Schema(description = "Année de construction", example = "2018") Integer yearBuilt,
    @Schema(description = "Surface totale du terrain (m²)", example = "400.0")
        BigDecimal surfaceTotal,
    @Schema(description = "Surface habitable nette (m²)", example = "280.0")
        BigDecimal surfaceLiving,
    @Schema(description = "Nombre total de pièces principales", example = "6") Integer rooms,
    @Schema(description = "Nombre de chambres", example = "4") Integer bedrooms,
    @Schema(description = "Nombre de salles de bain", example = "3") Integer bathrooms,
    @Schema(description = "Nombre de balcons/terrasses", example = "2") Integer balconies,
    @Schema(description = "Étage du bien (0 = RDC, null = villa/terrain)", nullable = true)
        Integer floor,
    @Schema(description = "Nombre total d'étages de l'immeuble", nullable = true)
        Integer totalFloors,
    @Schema(description = "Adresse complète du bien") String address,
    @Schema(description = "Ville du bien", example = "Dakar") String city,
    @Schema(description = "Quartier ou commune", example = "Almadies") String district,
    @Schema(description = "Nom de la rue", example = "Rue des Almadies") String street,
    @Schema(description = "Latitude GPS", example = "14.745500") BigDecimal latitude,
    @Schema(description = "Longitude GPS", example = "-17.489200") BigDecimal longitude,
    @Schema(description = "Commodités du bien (standard + personnalisées)")
        List<PropertyAmenityResponse> amenities,
    @Schema(description = "Annonce boostée (mise en avant payante)") boolean boosted,
    @Schema(description = "Date d'expiration du boost", nullable = true)
        LocalDateTime boostExpiresAt,
    @Schema(description = "Statut de l'annonce dans son cycle de publication")
        PropertyStatus status,
    @Schema(description = "Motif de refus par le modérateur", nullable = true)
        String rejectionReason,
    @Schema(description = "Date de publication de l'annonce", nullable = true)
        LocalDateTime publishedAt,
    @Schema(
            description = "Type de lit principal (hôtel) : SINGLE, DOUBLE, TWIN, KING, QUEEN, BUNK",
            nullable = true)
        String bedType,
    @Schema(description = "Capacité maximale d'accueil (hôtel)", nullable = true)
        Integer maxOccupancy,
    @Schema(
            description =
                "Formule repas (hôtel) : ROOM_ONLY, BREAKFAST, HALF_BOARD, FULL_BOARD, ALL_INCLUSIVE",
            nullable = true)
        String mealPlan,
    @Schema(
            description = "Fréquence de facturation (hôtel) : NIGHTLY, WEEKLY, MONTHLY",
            nullable = true)
        String paymentFrequency,
    @Schema(
            description = "URL de la photo de couverture (null si aucun média uploadé)",
            nullable = true)
        String coverPhotoUrl,
    @Schema(description = "Date de création") LocalDateTime createdAt,
    @Schema(description = "Date de dernière modification") LocalDateTime updatedAt) {}
