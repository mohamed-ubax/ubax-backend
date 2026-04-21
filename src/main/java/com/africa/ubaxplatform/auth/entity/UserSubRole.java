package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Sous-rôle applicatif d'un utilisateur, persisté en base (hors Keycloak).
 *
 * <p>Règles de cohérence :
 *
 * <ul>
 *   <li>scope {@code UBAX_INTERNAL} → valeurs de {@link
 *       com.africa.ubaxplatform.auth.codeList.UbaxAdminRole}
 *   <li>scope {@code AGENCE} → valeurs de {@link com.africa.ubaxplatform.auth.codeList.AgenceRole}
 *   <li>scope {@code HOTEL} → valeurs de {@link com.africa.ubaxplatform.auth.codeList.HotelRole}
 * </ul>
 */
@Entity
@Table(
    name = "user_sub_roles",
    schema = "administrative",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_user_sub_roles",
            columnNames = {"user_id", "role", "scope"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubRole {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_user_sub_roles_user"))
  private User user;

  /** Valeur du sous-rôle (ex : FINANCE, DIRECTEUR_AGENCE, GERANT_HOTEL). */
  @Column(name = "role", nullable = false, length = 60)
  private String role;

  /** Portée du sous-rôle : UBAX_INTERNAL, AGENCE ou HOTEL. */
  @Enumerated(EnumType.STRING)
  @Column(name = "scope", nullable = false, length = 20)
  private RoleScope scope;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
