package com.africa.ubaxplatform.property.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "property_amenities", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PropertyAmenity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "property_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_amenity_property"))
  private Property property;

  /** Code référençant une commodité standard dans {@code la_code_list} (type PROPERTY_AMENITY). */
  @Column(name = "code", length = 100)
  private String code;

  /** Valeur libre saisie par l'utilisateur quand la commodité n'existe pas dans la liste. */
  @Column(name = "custom_value", length = 100)
  private String customValue;

  /** Description optionnelle de la commodité personnalisée. */
  @Column(name = "custom_description", length = 255)
  private String customDescription;

  /** Description affichée pour cette commodité (standard ou personnalisée). */
  @Column(name = "description", length = 500)
  private String description;
}
