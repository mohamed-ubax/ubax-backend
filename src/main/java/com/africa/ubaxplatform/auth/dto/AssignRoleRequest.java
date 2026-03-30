package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Requête d'attribution d'un rôle realm Keycloak à un utilisateur. */
@Getter
@Setter
public class AssignRoleRequest {

  @NotNull(message = "Le rôle est obligatoire")
  private UserRole role;
}
