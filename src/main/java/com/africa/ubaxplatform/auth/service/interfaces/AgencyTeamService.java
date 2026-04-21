 package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.AgencyTeamMemberResponse;
import com.africa.ubaxplatform.auth.dto.InviteTeamMemberRequest;
import com.africa.ubaxplatform.auth.dto.UpdateTeamMemberRoleRequest;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.List;
import java.util.UUID;

/**
 * Contrat du service de gestion de l'équipe d'une agence partenaire.
 *
 * <p>Toutes les opérations sont restreintes au périmètre de l'agence de l'appelant. Le contrôle
 * d'accès (rôle {@code DIRECTEUR_AGENCE}) est effectué dans la couche contrôleur avant d'appeler
 * ces méthodes.
 *
 * <p>Rôles Keycloak requis au niveau HTTP (Spring Security) :
 *
 * <ul>
 *   <li>Lecture : {@code UBAX_PARTNER} ou {@code UBAX_AGENT}
 *   <li>Écriture (invite / update / remove) : {@code UBAX_PARTNER} + rôle interne {@code
 *       DIRECTEUR_AGENCE}
 * </ul>
 */
public interface AgencyTeamService {

  /**
   * Retourne la liste de tous les membres actifs de l'agence identifiée par l'appelant.
   *
   * @param callerKeycloakId identifiant Keycloak de l'utilisateur appelant
   * @return liste des membres de l'agence, triée par prénom croissant
   * @throws CustomException 404 si l'appelant n'est pas trouvé en base
   * @throws CustomException 403 si l'appelant n'est pas rattaché à une agence
   */
  List<AgencyTeamMemberResponse> listTeam(String callerKeycloakId) throws CustomException;

  /**
   * Rattache un utilisateur existant à l'agence de l'appelant avec le rôle spécifié.
   *
   * <p>Conditions requises :
   *
   * <ul>
   *   <li>L'utilisateur cible doit exister en base (identifié par email)
   *   <li>L'utilisateur cible ne doit pas déjà être rattaché à une agence
   * </ul>
   *
   * @param callerKeycloakId identifiant Keycloak du directeur appelant
   * @param request email de l'utilisateur cible et rôle à assigner
   * @return DTO du membre ajouté
   * @throws CustomException 404 si l'appelant ou l'utilisateur cible n'est pas trouvé
   * @throws CustomException 403 si l'appelant n'est pas rattaché à une agence
   * @throws CustomException 409 si l'utilisateur cible est déjà membre d'une agence
   */
  AgencyTeamMemberResponse inviteMember(String callerKeycloakId, InviteTeamMemberRequest request)
      throws CustomException;

  /**
   * Modifie le rôle interne d'un membre de l'équipe.
   *
   * <p>Le membre cible doit appartenir à la même agence que l'appelant.
   *
   * @param callerKeycloakId identifiant Keycloak du directeur appelant
   * @param memberId identifiant UUID du membre à mettre à jour
   * @param request nouveau rôle partenaire
   * @return DTO du membre mis à jour
   * @throws CustomException 404 si l'appelant ou le membre cible n'est pas trouvé
   * @throws CustomException 403 si le membre cible n'appartient pas à la même agence
   */
  AgencyTeamMemberResponse updateMemberRole(
      String callerKeycloakId, UUID memberId, UpdateTeamMemberRoleRequest request)
      throws CustomException;

  /**
   * Retire un membre de l'équipe agence (supprime le rattachement et le rôle interne).
   *
   * <p>L'opération ne supprime pas le compte utilisateur (soft-delete non appliqué). Le champ
   * {@code agency} et {@code partnerRole} sont remis à {@code null}. Un directeur ne peut pas se
   * retirer lui-même.
   *
   * @param callerKeycloakId identifiant Keycloak du directeur appelant
   * @param memberId identifiant UUID du membre à retirer
   * @throws CustomException 404 si l'appelant ou le membre n'est pas trouvé
   * @throws CustomException 403 si le membre n'appartient pas à la même agence, ou si l'appelant
   *     tente de se retirer lui-même
   */
  void removeMember(String callerKeycloakId, UUID memberId) throws CustomException;
}
