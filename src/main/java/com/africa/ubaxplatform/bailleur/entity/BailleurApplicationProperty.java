package com.africa.ubaxplatform.bailleur.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bailleur_application_properties", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BailleurApplicationProperty extends BaseEntity {

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "address", nullable = false, columnDefinition = "TEXT")
  private String address;

  @Column(name = "property_type", nullable = false, length = 50)
  private String propertyType;

  @Column(name = "rooms")
  private Integer rooms;

  @Column(name = "surface", precision = 10, scale = 2)
  private BigDecimal surface;

  @Column(name = "desired_rent", precision = 15, scale = 2)
  private BigDecimal desiredRent;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "latitude", precision = 10, scale = 8)
  private BigDecimal latitude;

  @Column(name = "longitude", precision = 11, scale = 8)
  private BigDecimal longitude;

  @Column(name = "geo_verified", nullable = false)
  private boolean geoVerified;
}
