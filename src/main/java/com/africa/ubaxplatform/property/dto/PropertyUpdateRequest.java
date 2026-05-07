package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Mise à jour partielle d'un bien. Seuls les champs non-null sont appliqués.")
public record PropertyUpdateRequest(
    @Schema(description = "Titre accrocheur de l'annonce", nullable = true) String title,
    @Schema(description = "Description complète du bien", nullable = true) String description,
    @Schema(description = "Type de bien (ex : APARTMENT, VILLA, HOTEL_ROOM…)", nullable = true)
        String propertyType,
    @Schema(
            description = "Type de transaction (SALE, RENT, RENT_FURNISHED, SHORT_STAY)",
            nullable = true)
        String transactionType,
    @Schema(description = "Prix de vente ou loyer mensuel en XOF", nullable = true)
        BigDecimal price,
    @Schema(description = "État général du bien (NEW, GOOD, RENOVATE)", nullable = true)
        String condition,
    @Schema(description = "Année de construction", nullable = true) Integer yearBuilt,
    @Schema(description = "Surface totale du terrain (m²)", nullable = true)
        BigDecimal surfaceTotal,
    @Schema(description = "Surface habitable nette (m²)", nullable = true) BigDecimal surfaceLiving,
    @Schema(description = "Nombre total de pièces principales", nullable = true) Integer rooms,
    @Schema(description = "Nombre de chambres", nullable = true) Integer bedrooms,
    @Schema(description = "Nombre de salles de bain", nullable = true) Integer bathrooms,
    @Schema(description = "Nombre de balcons/terrasses", nullable = true) Integer balconies,
    @Schema(description = "Étage du bien (0 = RDC)", nullable = true) Integer floor,
    @Schema(description = "Nombre total d'étages de l'immeuble", nullable = true)
        Integer totalFloors,
    @Schema(description = "Adresse complète", nullable = true) String address,
    @Schema(description = "Ville du bien", nullable = true) String city,
    @Schema(description = "Quartier ou commune", nullable = true) String district,
    @Schema(description = "Nom de la rue", nullable = true) String street,
    @Schema(description = "Latitude GPS", nullable = true) BigDecimal latitude,
    @Schema(description = "Longitude GPS", nullable = true) BigDecimal longitude,
    @Schema(
            description =
                "Remplace intégralement la liste des commodités. Null = aucune modification."
                    + " Liste vide = suppression de toutes les commodités.",
            nullable = true)
        List<PropertyAmenityRequest> amenities,
    @Schema(description = "Type de lit (hôtel)", nullable = true) String bedType,
    @Schema(description = "Capacité maximale d'accueil (hôtel)", nullable = true)
        Integer maxOccupancy,
    @Schema(description = "Formule repas (hôtel)", nullable = true) String mealPlan,
    @Schema(description = "Fréquence de facturation (hôtel)", nullable = true)
        String paymentFrequency) {}
