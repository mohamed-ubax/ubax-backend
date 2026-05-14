package com.africa.ubaxplatform.technicien.entity;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Prestataire / technicien externe géré par une agence ou un hôtel.
 *
 * <p>Un technicien n'a pas de compte sur la plateforme UBAX. Il est référencé dans le système
 * uniquement pour faciliter son assignation aux tickets SAV et conserver un historique des
 * interventions.
 *
 * <p>La contrainte {@code chk_technicien_structure} garantit qu'un technicien appartient à une
 * agence <strong>ou</strong> à un hôtel — jamais aux deux, jamais à aucun.
 */
@Entity
@Table(name = "techniciens", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Technicien extends BaseEntity {

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  @Column(name = "email", length = 150)
  private String email;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  /** Métier principal — valeur de {@code la_code_list} de type {@code TECHNICIEN_PROFESSION}. */
  @Column(name = "profession", nullable = false, length = 100)
  private String profession;

  @Column(name = "address", length = 255)
  private String address;

  /** {@code true} si le technicien est disponible pour de nouvelles interventions. */
  @Column(name = "available", nullable = false)
  @Builder.Default
  private boolean available = true;

  /** Agence gestionnaire. Mutuellement exclusif avec {@code hotel}. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agency_id", foreignKey = @ForeignKey(name = "fk_technicien_agency"))
  private Agency agency;

  /** Hôtel gestionnaire. Mutuellement exclusif avec {@code agency}. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hotel_id", foreignKey = @ForeignKey(name = "fk_technicien_hotel"))
  private Hotel hotel;

  /** Soft delete — {@code null} = actif. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** Nom complet du technicien. */
  public String getFullName() {
    return firstName + " " + lastName;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
