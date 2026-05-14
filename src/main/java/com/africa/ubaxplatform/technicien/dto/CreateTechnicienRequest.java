package com.africa.ubaxplatform.technicien.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTechnicienRequest {

  @NotBlank
  @Size(max = 100)
  @Schema(example = "Mamadou")
  private String firstName;

  @NotBlank
  @Size(max = 100)
  @Schema(example = "Koné")
  private String lastName;

  @NotBlank
  @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format invalide. Ex : +2250712345678")
  @Schema(example = "+2250712345678")
  private String phone;

  @Email
  @Size(max = 150)
  @Schema(example = "mamadou.kone@gmail.com")
  private String email;

  @Schema(description = "URL de l'avatar (déjà uploadé via /v1/storage)")
  private String avatarUrl;

  @NotBlank
  @Size(max = 100)
  @Schema(description = "Valeur de la code-list TECHNICIEN_PROFESSION", example = "PLOMBIER")
  private String profession;

  @Size(max = 255)
  @Schema(example = "Cocody, Abidjan")
  private String address;
}
