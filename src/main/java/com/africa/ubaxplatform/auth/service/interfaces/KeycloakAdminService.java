package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.List;
import java.util.Map;

/**
 * Contrat du service d'administration Keycloak.
 *
 * <p>Regroupe toutes les opérations machine-to-machine effectuées via l'API Admin REST de Keycloak.
 * Chaque opération obtient un token admin {@code client_credentials} en interne ; le client
 * Keycloak doit avoir le service account activé avec les rôles {@code realm-management →
 * manage-users} et {@code realm-management → manage-roles}.
 */
public interface KeycloakAdminService {

  /**
   * Envoie un email de réinitialisation du mot de passe à l'utilisateur via Keycloak.
   *
   * <p>Keycloak génère un lien sécurisé à durée limitée ({@code UPDATE_PASSWORD}) et l'envoie à
   * l'adresse email associée au compte.
   *
   * @param email adresse email de l'utilisateur cible
   * @throws CustomException si l'utilisateur n'existe pas ou si l'appel Keycloak échoue
   */
  void sendForgotPasswordEmail(String email) throws CustomException;

  /**
   * Réinitialise directement le mot de passe d'un utilisateur Keycloak (sans email).
   *
   * @param keycloakId identifiant Keycloak (UUID) de l'utilisateur
   * @param newPassword nouveau mot de passe en clair
   * @param temporary si {@code true}, l'utilisateur devra changer son mot de passe à la prochaine
   *     connexion
   * @throws CustomException si l'utilisateur est introuvable ou si l'appel Keycloak échoue
   */
  void resetPassword(String keycloakId, String newPassword, boolean temporary)
      throws CustomException;

  /**
   * Attribue un rôle realm Keycloak à un utilisateur.
   *
   * <p>Le rôle est préfixé {@code UBAX_} (ex : {@code UBAX_CLIENT}, {@code UBAX_ADMIN}) et doit
   * être préalablement défini dans le realm.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param role rôle fonctionnel à attribuer
   * @throws CustomException si le rôle ou l'utilisateur est introuvable
   */
  void assignRole(String keycloakId, UserRole role) throws CustomException;

  /**
   * Retire un rôle realm Keycloak d'un utilisateur.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param role rôle fonctionnel à retirer
   * @throws CustomException si le rôle ou l'utilisateur est introuvable
   */
  void removeRole(String keycloakId, UserRole role) throws CustomException;

  /**
   * Crée un utilisateur dans Keycloak et retourne son identifiant (sub UUID).
   *
   * <p>Le numéro de téléphone est stocké dans l'attribut custom {@code phone}. L'email n'est pas
   * marqué comme vérifié à la création.
   *
   * @param request données d'inscription (nom, prénom, téléphone, email optionnel, mot de passe)
   * @return identifiant Keycloak (UUID) de l'utilisateur créé
   * @throws CustomException si l'email ou le téléphone est déjà utilisé dans Keycloak (409)
   */
  String createUser(RegisterCompleteRequest request) throws CustomException;

  /**
   * Supprime un utilisateur Keycloak par son identifiant.
   *
   * <p>Utilisé comme rollback manuel lorsque la persistance en base de données échoue après la
   * création Keycloak réussie. Les erreurs sont loguées sans être propagées.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur à supprimer
   */
  void deleteUser(String keycloakId);

  /**
   * Désactive un utilisateur Keycloak ({@code enabled = false}).
   *
   * <p>Le compte est conservé dans Keycloak pour l'historique ; l'utilisateur ne peut plus se
   * connecter. Utilisé pour la suppression logique des administrateurs UBAX.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur à désactiver
   */
  void disableUser(String keycloakId);

  /**
   * Réactive un utilisateur Keycloak ({@code enabled = true}).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur à réactiver
   */
  void enableUser(String keycloakId);

  /**
   * Récupère tous les rôles realm définis dans Keycloak.
   *
   * @return liste des représentations de rôles (champs : {@code id}, {@code name}, {@code
   *     description}, {@code composite}, {@code clientRole})
   */
  List<Map<String, Object>> getRoles();

  /**
   * Recherche l'identifiant Keycloak (UUID) d'un utilisateur par son adresse email.
   *
   * @param email adresse email de l'utilisateur
   * @return identifiant Keycloak (UUID)
   * @throws com.africa.ubaxplatform.common.exception.NotFoundException si aucun utilisateur ne
   *     correspond
   */
  String findUserIdByEmail(String email);

  /**
   * Recherche l'identifiant Keycloak (UUID) d'un utilisateur par son numéro de téléphone.
   *
   * <p>La recherche s'appuie sur l'attribut custom {@code phone} stocké dans Keycloak.
   *
   * @param phone numéro de téléphone au format international (ex : {@code +221781234567})
   * @return identifiant Keycloak (UUID)
   * @throws com.africa.ubaxplatform.common.exception.NotFoundException si aucun utilisateur ne
   *     correspond
   */
  String findUserIdByPhone(String phone);

  /**
   * Recherche le username Keycloak d'un utilisateur par son numéro de téléphone.
   *
   * <p>Le username Keycloak correspond au numéro de téléphone saisi à l'inscription. Utilisé pour
   * le flux ROPC (Resource Owner Password Credentials) de connexion mobile.
   *
   * @param phone numéro de téléphone au format international
   * @return username Keycloak de l'utilisateur
   * @throws com.africa.ubaxplatform.common.exception.NotFoundException si aucun utilisateur ne
   *     correspond
   */
  String findUsernameByPhone(String phone);

  /**
   * Crée un compte Keycloak pour un partenaire approuvé.
   *
   * <p>Le login est l'email du partenaire. Aucun mot de passe n'est défini à la création ; {@code
   * requiredActions: [UPDATE_PASSWORD]} force le partenaire à définir son mot de passe via le lien
   * envoyé par {@link #sendSetPasswordLink(String)}.
   *
   * @param email adresse email (sert de username et d'email Keycloak)
   * @param firstName prénom du représentant légal
   * @param lastName nom de famille du représentant légal
   * @param phone numéro de téléphone au format international – stocké dans l'attribut custom {@code
   *     phone}
   * @return identifiant Keycloak (UUID) du compte créé
   * @throws CustomException si l'email est déjà utilisé (409) ou si l'appel échoue
   */
  String createPartnerAccount(String email, String firstName, String lastName, String phone)
      throws CustomException;

  /**
   * Crée un compte Keycloak pour un administrateur interne UBAX.
   *
   * <p>Le username est l'email de l'administrateur. Aucun mot de passe n'est défini à la création ;
   * {@code requiredActions: [UPDATE_PASSWORD]} force l'administrateur à définir son mot de passe
   * via le lien envoyé par {@link #sendSetPasswordLink(String)}.
   *
   * @param email adresse email professionnelle (sert de username et d'email Keycloak)
   * @param firstName prénom de l'administrateur
   * @param lastName nom de famille de l'administrateur
   * @param phone numéro de téléphone (optionnel, peut être {@code null})
   * @return identifiant Keycloak (UUID) du compte créé
   * @throws CustomException si l'email est déjà utilisé (409) ou si l'appel échoue
   */
  String createAdminAccount(String email, String firstName, String lastName, String phone)
      throws CustomException;

  /**
   * Envoie un lien sécurisé de définition du mot de passe via Keycloak ({@code
   * execute-actions-email} avec l'action {@code UPDATE_PASSWORD}).
   *
   * <p>Le lien expire selon la configuration du realm Keycloak (défaut : 24h).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur cible
   * @throws CustomException si l'appel Keycloak échoue
   */
  void sendSetPasswordLink(String keycloakId) throws CustomException;

  /**
   * Crée un compte Keycloak pour un bailleur approuvé.
   *
   * <p>Le username est le numéro de téléphone (compatible avec la connexion mobile via ROPC).
   * L'email est stocké en attribut optionnel. Le mot de passe temporaire est fourni en clair et
   * persisté immédiatement — l'application est responsable de l'envoyer par SMS au bailleur.
   *
   * @param firstName prénom du bailleur
   * @param lastName nom de famille du bailleur
   * @param phone numéro de téléphone (sert de username Keycloak)
   * @param email adresse email (optionnelle, stockée en attribut)
   * @param tempPassword mot de passe temporaire en clair
   * @return identifiant Keycloak (UUID) du compte créé
   * @throws CustomException si le téléphone est déjà utilisé (409) ou si l'appel échoue
   */
  String createBailleurAccount(
      String firstName, String lastName, String phone, String email, String tempPassword)
      throws CustomException;
}
