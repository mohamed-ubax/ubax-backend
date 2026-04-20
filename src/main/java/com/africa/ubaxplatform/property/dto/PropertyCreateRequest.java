package com.africa.ubaxplatform.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PropertyCreateRequest(
    @NotBlank String title,
    String description,
    @NotBlank String propertyType,
    @NotBlank String transactionType,
    @NotNull @DecimalMin("0") BigDecimal price,
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
    @NotBlank String city,
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

    /** UUID du propriétaire réel du bien. Optionnel : si absent, l'appelant est le propriétaire. */
    UUID ownerId) {}
