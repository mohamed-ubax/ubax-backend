package com.africa.ubaxplatform.bailleur.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.bailleur.codeList.BailleurApplicationStatus;
import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bailleur_applications", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BailleurApplication extends BaseEntity {

  @Column(name = "agency_id", nullable = false)
  private UUID agencyId;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "phone", nullable = false, length = 25)
  private String phone;

  @Column(name = "email", nullable = false, length = 150)
  private String email;

  @Column(name = "id_type", nullable = false, length = 50)
  private String idType;

  @Column(name = "id_number", nullable = false, length = 100)
  private String idNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private BailleurApplicationStatus status;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  @Column(name = "conflict_detected", nullable = false)
  private boolean conflictDetected;

  @Column(name = "conflict_note", columnDefinition = "TEXT")
  private String conflictNote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_user_id")
  private User reviewedBy;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;
}
