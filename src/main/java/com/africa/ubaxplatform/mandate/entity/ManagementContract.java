package com.africa.ubaxplatform.mandate.entity;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "management_contracts", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ManagementContract extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agency_id", nullable = false)
  private Agency agency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "terminated_by_id")
  private User terminatedBy;

  @Column(name = "reference_number")
  private String referenceNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MandateStatus status = MandateStatus.DRAFT;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "commission_rate", precision = 5, scale = 2)
  private BigDecimal commissionRate;

  @Column(name = "special_clauses", columnDefinition = "TEXT")
  private String specialClauses;

  @Column(name = "termination_conditions", columnDefinition = "TEXT")
  private String terminationConditions;

  @Column(name = "file_url", columnDefinition = "TEXT")
  private String fileUrl;

  @Column(name = "terminated_at")
  private LocalDateTime terminatedAt;

  @Column(name = "termination_reason", columnDefinition = "TEXT")
  private String terminationReason;
}
