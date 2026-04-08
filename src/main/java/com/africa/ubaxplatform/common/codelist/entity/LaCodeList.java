package com.africa.ubaxplatform.common.codelist.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "la_code_list", schema = "administrative")
public class LaCodeList extends BaseEntity {

  @Basic(optional = false)
  @Column(name = "type", length = 100, nullable = false)
  private String type;

  @Basic(optional = false)
  @Column(name = "value", length = 100, nullable = false)
  private String value;

  @Basic(optional = false)
  @Column(name = "description", length = 500, nullable = false)
  private String description;

  /** {@code true} = valeur initialisée par le système (non supprimable). */
  @Basic(optional = false)
  @Column(name = "is_system_assign", nullable = false)
  private boolean isSystemAssign;

  @Override
  public String toString() {
    return "LaCodeList{"
        + "id="
        + getId()
        + ", type='"
        + type
        + '\''
        + ", value='"
        + value
        + '\''
        + ", description='"
        + description
        + '\''
        + ", isSystemAssign="
        + isSystemAssign
        + '}';
  }
}
