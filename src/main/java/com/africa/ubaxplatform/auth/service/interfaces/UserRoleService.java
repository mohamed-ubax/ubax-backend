package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.dto.AddTeamMemberRequest;
import com.africa.ubaxplatform.auth.dto.UserResponse;
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

  /**
   * Assigne des sous-rôles AGENCE ou HOTEL par un PARTNER de la même structure.
   *
   * <p>Le caller doit être PARTNER et appartenir à la même agence/hôtel que la cible.
   * L'auto-assignation est autorisée (targetUserId == caller's own ID).
   *
   * @param callerKeycloakId keycloak ID du PARTNER appelant
   * @param targetUserId ID DB de l'utilisateur cible
   * @param roles liste des sous-rôles à assigner
   * @param scope AGENCE ou HOTEL uniquement
   */
  List<UserSubRoleResponse> assignPartnerSubRoles(
      String callerKeycloakId, UUID targetUserId, List<String> roles, RoleScope scope)
      throws CustomException;

  /** Retourne les sous-rôles d'un membre de la structure du PARTNER appelant. */
  List<UserSubRoleResponse> getPartnerSubRoles(
      String callerKeycloakId, UUID targetUserId, RoleScope scope) throws CustomException;

  /** Révoque un sous-rôle d'un membre de la structure du PARTNER appelant. */
  void revokePartnerSubRole(
      String callerKeycloakId, UUID targetUserId, String role, RoleScope scope)
      throws CustomException;

  /**
   * Retourne la liste des membres de la structure du PARTNER appelant (agence ou hôtel).
   *
   * @param callerKeycloakId keycloak ID du PARTNER appelant
   * @param scope AGENCE ou HOTEL
   */
  List<UserResponse> getTeamMembers(String callerKeycloakId, RoleScope scope)
      throws CustomException;

  /**
   * Retourne la liste des membres d'une agence — accès admin back-office.
   *
   * @param agencyId identifiant de l'agence
   */
  List<UserResponse> getAgencyMembersForAdmin(UUID agencyId);

  /**
   * Retourne la liste des membres d'un hôtel — accès admin back-office.
   *
   * @param hotelId identifiant de l'hôtel
   */
  List<UserResponse> getHotelMembersForAdmin(UUID hotelId);

  /**
   * Retire un membre de l'équipe (soft delete + désactivation Keycloak).
   *
   * <p>Réservé au fondateur (PARTNER_ADMIN) ou au directeur (DG/GERANT) de la même structure.
   * Impossible de se supprimer soi-même.
   *
   * @param callerKeycloakId keycloak ID du PARTNER appelant
   * @param targetUserId ID DB du membre à retirer
   * @param scope AGENCE ou HOTEL
   */
  void removeTeamMember(String callerKeycloakId, UUID targetUserId, RoleScope scope)
      throws CustomException;

  /**
   * Crée un nouveau membre d'équipe dans la structure du PARTNER appelant.
   *
   * <p>Crée le compte Keycloak (rôle PARTNER, action UPDATE_PASSWORD), envoie l'email de définition
   * du mot de passe, crée l'entité User en DB liée à l'agence/hôtel, puis assigne les sous-rôles
   * fournis. En cas d'échec après la création Keycloak, le compte est supprimé en rollback.
   *
   * @param callerKeycloakId keycloak ID du PARTNER appelant
   * @param request données du nouveau membre
   * @param scope AGENCE ou HOTEL
   */
  UserResponse addTeamMember(String callerKeycloakId, AddTeamMemberRequest request, RoleScope scope)
      throws CustomException;
}
