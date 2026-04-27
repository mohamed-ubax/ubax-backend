package com.africa.ubaxplatform.bailleur.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bailleur_agency_links", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BailleurAgencyLink extends BaseEntity {

  @Column(name = "bailleur_user_id", nullable = false)
  private UUID bailleurUserId;

  @Column(name = "agency_id", nullable = false)
  private UUID agencyId;

  @Column(name = "joined_at", nullable = false)
  private LocalDateTime joinedAt;
}
