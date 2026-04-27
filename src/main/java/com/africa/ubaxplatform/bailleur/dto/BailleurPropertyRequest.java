package com.africa.ubaxplatform.bailleur.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BailleurPropertyRequest {

  @NotBlank(message = "L'adresse du bien est obligatoire")
  private String address;

  @NotBlank(message = "Le type de bien est obligatoire")
  private String propertyType;

  @Positive(message = "Le nombre de pièces doit être positif")
  private Integer rooms;

  @Positive(message = "La surface doit être positive")
  private BigDecimal surface;

  @Positive(message = "Le loyer souhaité doit être positif")
  private BigDecimal desiredRent;

  private String description;

  @DecimalMin(value = "-90.0", message = "Latitude invalide (min -90)")
  @DecimalMax(value = "90.0", message = "Latitude invalide (max 90)")
  private BigDecimal latitude;

  @DecimalMin(value = "-180.0", message = "Longitude invalide (min -180)")
  @DecimalMax(value = "180.0", message = "Longitude invalide (max 180)")
  private BigDecimal longitude;
}
