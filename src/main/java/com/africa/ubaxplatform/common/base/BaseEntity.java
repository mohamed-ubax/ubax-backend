package com.africa.ubaxplatform.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseEntity {
  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
  private UUID id;

  /**
   * Date et heure de création de l'enregistrement.
   *
   * <p>Valorisée automatiquement par Hibernate au moment du premier {@code INSERT}. Non modifiable
   * ensuite ({@code updatable = false}).
   */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Date et heure de la dernière modification de l'enregistrement.
   *
   * <p>Mise à jour automatiquement par Hibernate à chaque {@code UPDATE}. Égale à {@code createdAt}
   * juste après l'insertion.
   */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
