package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AdminUserResponse;
import com.africa.ubaxplatform.auth.dto.CreateAdminRequest;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.List;
import java.util.UUID;

/**
 * Contrat du service de gestion des administrateurs internes UBAX.
 *
 * <p>Orchestre la création des comptes administrateurs : création Keycloak, attribution du rôle,
 * synchronisation en base de données et envoi de l'email de définition du mot de passe.
 *
 * <p>Toutes les opérations sont réservées aux utilisateurs ayant le rôle {@code SUPER_ADMIN} (pour
 * la création) ou {@code ADMIN} / {@code SUPER_ADMIN} (pour la consultation).
 */
public interface AdminManagementService {

  /**
   * Crée un administrateur interne UBAX.
   *
   * <p>Flux d'exécution :
   *
   * <ol>
   *   <li>Validation que le rôle demandé est {@code ADMIN} ou {@code SUPER_ADMIN}
   *   <li>Vérification de l'unicité de l'email en base
   *   <li>Création du compte Keycloak (sans mot de passe, action {@code UPDATE_PASSWORD} requise)
   *   <li>Attribution du rôle Keycloak ({@code UBAX_ADMIN} ou {@code UBAX_SUPER_ADMIN})
   *   <li>Persistance de l'utilisateur en base de données
   *   <li>Envoi de l'email de définition du mot de passe
   * </ol>
   *
   * <p>En cas d'échec après la création Keycloak, un rollback est effectué pour supprimer le compte
   * Keycloak et éviter toute désynchronisation.
   *
   * @param request données du formulaire d'ajout d'administrateur
   * @return réponse contenant les informations du compte créé
   * @throws CustomException si l'email est déjà utilisé, si le rôle est invalide ou si une erreur
   *     Keycloak survient
   */
  AdminUserResponse createAdmin(CreateAdminRequest request) throws CustomException;

  /**
   * Retourne la liste de tous les administrateurs actifs de la plateforme.
   *
   * <p>Inclut les comptes ayant le rôle {@code ADMIN} ou {@code SUPER_ADMIN}.
   *
   * @return liste des administrateurs
   */
  List<AdminUserResponse> listAdmins();

  /**
   * Modifie le rôle d'un administrateur UBAX existant.
   *
   * <p>Flux :
   *
   * <ol>
   *   <li>Validation que le rôle cible est {@code ADMIN} ou {@code SUPER_ADMIN}
   *   <li>Chargement de l'administrateur en base
   *   <li>Retrait de l'ancien rôle admin Keycloak
   *   <li>Attribution du nouveau rôle Keycloak
   *   <li>Mise à jour du rôle en base
   * </ol>
   *
   * @param userId identifiant interne (UUID) de l'administrateur
   * @param newRole nouveau rôle à attribuer ({@code ADMIN} ou {@code SUPER_ADMIN})
   * @return réponse mise à jour
   * @throws CustomException si l'utilisateur est introuvable, n'est pas un administrateur, est
   *     supprimé, ou si le rôle est invalide
   */
  AdminUserResponse assignAdminRole(UUID userId, UserRole newRole) throws CustomException;

  /**
   * Supprime logiquement un administrateur UBAX.
   *
   * <p>Le compte n'est pas physiquement supprimé : {@code deletedAt} est positionné et {@code
   * active} est mis à {@code false} en base. Le compte Keycloak est désactivé ({@code enabled =
   * false}) mais conservé pour l'historique et l'audit.
   *
   * @param userId identifiant interne (UUID) de l'administrateur à supprimer
   * @throws CustomException si l'utilisateur est introuvable, n'est pas un administrateur ou est
   *     déjà supprimé
   */
  void deleteAdmin(UUID userId) throws CustomException;
}
