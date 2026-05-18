package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Requête de création d'un bien immobilier (statut DRAFT)")
public record PropertyCreateRequest(
    @Schema(
            description = "Titre accrocheur de l'annonce",
            example = "Villa 4 chambres aux Almadies")
        @NotBlank
        String title,
    @Schema(description = "Description complète du bien") String description,
    @Schema(
            description =
                "Type de bien. Immobilier : APARTMENT, VILLA, HOUSE, LAND, OFFICE, WAREHOUSE, STORE."
                    + " Hôtel : HOTEL_ROOM, HOTEL_SUITE, HOTEL_STUDIO, EVENT_SPACE,"
                    + " CONFERENCE_ROOM, RESTAURANT_SPACE.",
            example = "VILLA")
        @NotBlank
        @Pattern(
            regexp =
                "APARTMENT|VILLA|HOUSE|LAND|OFFICE|WAREHOUSE|STORE"
                    + "|HOTEL_ROOM|HOTEL_SUITE|HOTEL_STUDIO|EVENT_SPACE|CONFERENCE_ROOM|RESTAURANT_SPACE",
            message =
                "propertyType invalide. Valeurs acceptées : APARTMENT, VILLA, HOUSE, LAND, OFFICE,"
                    + " WAREHOUSE, STORE, HOTEL_ROOM, HOTEL_SUITE, HOTEL_STUDIO, EVENT_SPACE,"
                    + " CONFERENCE_ROOM, RESTAURANT_SPACE")
        String propertyType,
    @Schema(
            description =
                "Type de transaction. Immobilier : SALE, RENT, RENT_FURNISHED."
                    + " Hôtel : SHORT_STAY. Un bien hôtelier doit obligatoirement être en SHORT_STAY.",
            example = "SALE")
        @NotBlank
        @Pattern(
            regexp = "SALE|RENT|RENT_FURNISHED|SHORT_STAY",
            message =
                "transactionType invalide. Valeurs acceptées : SALE, RENT, RENT_FURNISHED, SHORT_STAY")
        String transactionType,
    @Schema(description = "Prix de vente ou loyer mensuel en XOF", example = "150000000")
        @NotNull
        @DecimalMin("0")
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
    @Schema(description = "Étage du bien (0 = RDC, null = villa/terrain)", example = "null")
        Integer floor,
    @Schema(description = "Nombre total d'étages de l'immeuble", example = "null")
        Integer totalFloors,
    @Schema(description = "Adresse complète", example = "12 Rue des Almadies, Dakar")
        String address,
    @Schema(description = "Ville du bien", example = "Dakar") @NotBlank String city,
    @Schema(description = "Quartier ou commune", example = "Almadies") String district,
    @Schema(description = "Nom de la rue", example = "Rue des Almadies") String street,
    @Schema(description = "Latitude GPS", example = "14.745500") BigDecimal latitude,
    @Schema(description = "Longitude GPS", example = "-17.489200") BigDecimal longitude,
    @Schema(
            description =
                "Liste des commodités. Chaque entrée est soit un code standard"
                    + " (ex: POOL, AC, PARKING) soit une commodité personnalisée (customValue + customDescription).",
            nullable = true)
        List<PropertyAmenityRequest> amenities,
    @Schema(
            description =
                "UUID du propriétaire réel du bien. Optionnel : si absent, l'appelant est le propriétaire.",
            nullable = true)
        UUID ownerId,
    @Schema(
            description = "Type de lit (hôtel) : SINGLE, DOUBLE, TWIN, KING, QUEEN, BUNK",
            nullable = true)
        String bedType,
    @Schema(description = "Capacité maximale d'accueil (hôtel)", example = "2", nullable = true)
        Integer maxOccupancy,
    @Schema(
            description =
                "Formule repas (hôtel) : ROOM_ONLY, BREAKFAST, HALF_BOARD, FULL_BOARD, ALL_INCLUSIVE",
            nullable = true)
        String mealPlan,
    @Schema(
            description =
                "Fréquence de facturation (hôtel) : NIGHTLY, WEEKLY, MONTHLY."
                    + " Obligatoire pour un bien SHORT_STAY.",
            nullable = true)
        @Pattern(
            regexp = "NIGHTLY|WEEKLY|MONTHLY",
            message = "paymentFrequency invalide. Valeurs acceptées : NIGHTLY, WEEKLY, MONTHLY")
        String paymentFrequency) {}
