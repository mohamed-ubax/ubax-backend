package com.africa.ubaxplatform.common.codelist.dto;

import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO utilisé à la fois en entrée (création / mise à jour) et en sortie. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaCodeListDto {

  private UUID id;

  @NotBlank
  @Size(max = 100)
  private String type;

  @NotBlank
  @Size(max = 100)
  private String value;

  @NotBlank
  @Size(max = 500)
  private String description;

  private boolean isSystemAssign;

  // ── Conversion ────────────────────────────────────────────────

  public static LaCodeListDto from(LaCodeList entity) {
    return LaCodeListDto.builder()
        .id(entity.getId())
        .type(entity.getType())
        .value(entity.getValue())
        .description(entity.getDescription())
        .isSystemAssign(entity.isSystemAssign())
        .build();
  }

  public LaCodeList toEntity() {
    return LaCodeList.builder()
        .type(this.type)
        .value(this.value)
        .description(this.description)
        .isSystemAssign(this.isSystemAssign)
        .build();
  }
}
