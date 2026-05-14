package com.africa.ubaxplatform.technicien.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTechnicienRequest {

  @Size(max = 100)
  private String firstName;

  @Size(max = 100)
  private String lastName;

  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format invalide. Ex : +2250712345678")
  private String phone;

  @Email
  @Size(max = 150)
  private String email;

  @Schema(description = "URL de l'avatar (déjà uploadé via /v1/storage)")
  private String avatarUrl;

  @Size(max = 100)
  @Schema(description = "Valeur de la code-list TECHNICIEN_PROFESSION")
  private String profession;

  @Size(max = 255)
  private String address;
}
