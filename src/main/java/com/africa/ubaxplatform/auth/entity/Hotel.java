package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.common.constants.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Hôtel partenaire sur la plateforme UBAX. */
@Entity
@Table(name = "hotels", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Hotel extends BaseEntity {

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "registration_number", length = 100)
  private String registrationNumber;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  @Column(name = "city", length = 100)
  private String city;

  @Column(name = "country", length = 5)
  @Builder.Default
  private String country = "SN";

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 150)
  private String email;

  @Column(name = "website", length = 300)
  private String website;

  @Column(name = "stars")
  private Integer stars;

  @Column(name = "total_rooms")
  private Integer totalRooms;

  @Column(name = "subscription_plan", length = 20)
  @Builder.Default
  private String subscriptionPlan = Constants.CodeList.SubscriptionPlan.BASIC;

  @Column(name = "subscription_expires_at")
  private LocalDateTime subscriptionExpiresAt;

  @Column(name = "is_verified", nullable = false)
  @Builder.Default
  private boolean verified = false;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public boolean isSubscriptionActive() {
    return subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(LocalDateTime.now());
  }
}
