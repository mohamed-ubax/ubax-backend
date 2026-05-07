package com.africa.ubaxplatform.property.dto;

import java.math.BigDecimal;

/** Mise à jour partielle d'un bien. Seuls les champs non-null sont appliqués. */
public record PropertyUpdateRequest(
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
    Boolean hasPool,
    Boolean hasGenerator,
    Boolean hasWaterTank,
    Boolean hasAc,
    Boolean hasSecurity,
    Boolean hasParking,
    Boolean hasElevator,
    Boolean hasGarden,
    Boolean furnished,
    Boolean acceptsPets,
    Boolean pmrAccessible,

    // ── Champs hôtel ──────────────────────────────────────────────
    String bedType,
    Integer maxOccupancy,
    String mealPlan,
    String paymentFrequency) {}
