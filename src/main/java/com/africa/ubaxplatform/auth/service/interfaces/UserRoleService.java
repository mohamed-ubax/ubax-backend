package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.dto.UserSubRoleResponse;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des sous-rôles applicatifs (table {@code user_sub_roles}).
 *
 * <p>Règles de cohérence des scopes :
 *
 * <ul>
 *   <li>{@code UBAX_INTERNAL} → réservé aux utilisateurs ADMIN / SUPER_ADMIN
 *   <li>{@code AGENCE} → réservé aux utilisateurs PARTNER rattachés à une agence
 *   <li>{@code HOTEL} → réservé aux utilisateurs PARTNER rattachés à un hôtel
 * </ul>
 */
public interface UserRoleService {

  /**
   * Assigne une liste de sous-rôles à un utilisateur pour un scope donné. Les sous-rôles existants
   * dans ce scope sont préservés — pas de remplacement global.
   *
   * @param userId identifiant de l'utilisateur cible
   * @param roles liste des valeurs de sous-rôles à assigner
   * @param scope portée (UBAX_INTERNAL, AGENCE, HOTEL)
   * @throws CustomException si l'utilisateur est introuvable ou si le scope est incompatible
   */
  List<UserSubRoleResponse> assignSubRoles(UUID userId, List<String> roles, RoleScope scope)
      throws CustomException;

  /**
   * Retourne tous les sous-rôles d'un utilisateur, optionnellement filtrés par scope.
   *
   * @param userId identifiant de l'utilisateur
   * @param scope scope de filtrage (null = tous les scopes)
   */
  List<UserSubRoleResponse> getSubRoles(UUID userId, RoleScope scope);

  /**
   * Révoque un sous-rôle spécifique d'un utilisateur.
   *
   * @throws CustomException si l'entrée est introuvable
   */
  void revokeSubRole(UUID userId, String role, RoleScope scope) throws CustomException;

  /**
   * Vérifie si un utilisateur possède un sous-rôle donné dans un scope.
   *
   * @return {@code true} si le sous-rôle est assigné
   */
  boolean hasSubRole(UUID userId, String role, RoleScope scope);
}
