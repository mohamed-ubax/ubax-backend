package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * Requête d'affectation (ou de changement) de rôle pour un administrateur interne UBAX.
 *
 * <p>Seuls les rôles {@code ADMIN} et {@code SUPER_ADMIN} sont acceptés. Les autres valeurs
 * déclenchent une {@code CustomException} dans la couche service.
 */
@Getter
public class AssignAdminRoleRequest {

  /**
   * Nouveau rôle à attribuer à l'administrateur. Doit être {@code ADMIN} ou {@code SUPER_ADMIN}.
   */
  @NotNull(message = "Le rôle est obligatoire")
  private UserRole role;
}
