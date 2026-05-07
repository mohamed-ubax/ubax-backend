package com.africa.ubaxplatform.property.dto;

import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant un bien immobilier publié sur la plateforme UBAX.
 *
 * @param id identifiant unique du bien
 * @param ownerId identifiant du propriétaire ou agent
 * @param ownerName nom complet du propriétaire ou agent
 * @param agencyId identifiant de l'agence gestionnaire, {@code null} si particulier
 * @param agencyName nom de l'agence gestionnaire, {@code null} si particulier
 * @param title titre accrocheur de l'annonce
 * @param description description complète du bien
 * @param propertyType catégorie du bien (APARTMENT, VILLA, HOUSE, LAND, OFFICE, WAREHOUSE, STORE)
 * @param transactionType nature de la transaction (SALE, RENT, RENT_FURNISHED)
 * @param price prix de vente ou loyer mensuel en XOF
 * @param condition état général du bien (NEW, GOOD, RENOVATE)
 * @param yearBuilt année de construction
 * @param surfaceTotal surface totale du terrain en m²
 * @param surfaceLiving surface habitable nette en m²
 * @param rooms nombre total de pièces principales
 * @param bedrooms nombre de chambres à coucher
 * @param bathrooms nombre de salles de bain
 * @param balconies nombre de balcons ou terrasses
 * @param floor étage du bien, {@code null} pour villas/terrains
 * @param totalFloors nombre total d'étages de l'immeuble
 * @param address adresse complète du bien
 * @param city ville où se situe le bien
 * @param district quartier ou commune
 * @param street rue ou avenue
 * @param latitude latitude GPS
 * @param longitude longitude GPS
 * @param hasPool piscine disponible
 * @param hasGenerator groupe électrogène disponible
 * @param hasWaterTank réservoir d'eau autonome
 * @param hasAc climatisation installée
 * @param hasSecurity gardiennage et sécurité présents
 * @param hasParking place de parking disponible
 * @param hasElevator ascenseur présent
 * @param hasGarden jardin ou espace vert privatif
 * @param furnished bien livré meublé
 * @param acceptsPets animaux de compagnie acceptés
 * @param pmrAccessible accessible aux personnes à mobilité réduite
 * @param boosted annonce boostée (mise en avant payante)
 * @param boostExpiresAt date d'expiration du boost payant
 * @param status statut de l'annonce dans son cycle de publication
 * @param rejectionReason motif de refus par le modérateur
 * @param publishedAt date de publication de l'annonce
 * @param bedType type de lit principal (hôtel) : SINGLE, DOUBLE, TWIN, KING, QUEEN, BUNK
 * @param maxOccupancy capacité maximale d'accueil (hôtel)
 * @param mealPlan formule repas incluse (hôtel) : ROOM_ONLY, BREAKFAST, HALF_BOARD, FULL_BOARD,
 *     ALL_INCLUSIVE
 * @param paymentFrequency fréquence de facturation : NIGHTLY, WEEKLY, MONTHLY
 * @param createdAt date de création
 * @param updatedAt date de dernière modification
 */
public record PropertyResponse(
    UUID id,
    UUID ownerId,
    String ownerName,
    UUID agencyId,
    String agencyName,
    String title,
    String description,
    String propertyType,
    String transactionType,
    BigDecimal price,
    String condition,
    Integer yearBuilt,
    BigDecimal surfaceTotal,
    BigDecimal surfaceLiving,
    Integer rooms,
    Integer bedrooms,
    Integer bathrooms,
    Integer balconies,
    Integer floor,
    Integer totalFloors,
    String address,
    String city,
    String district,
    String street,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean hasPool,
    boolean hasGenerator,
    boolean hasWaterTank,
    boolean hasAc,
    boolean hasSecurity,
    boolean hasParking,
    boolean hasElevator,
    boolean hasGarden,
    boolean furnished,
    boolean acceptsPets,
    boolean pmrAccessible,
    boolean boosted,
    LocalDateTime boostExpiresAt,
    PropertyStatus status,
    String rejectionReason,
    LocalDateTime publishedAt,
    String bedType,
    Integer maxOccupancy,
    String mealPlan,
    String paymentFrequency,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
