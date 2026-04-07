package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.common.exception.CustomException;

/**
 * Contrat du service d'inscription mobile via OTP SMS.
 *
 * <p>Définit le flux en 3 étapes pour l'inscription d'un nouvel utilisateur depuis l'application
 * mobile :
 *
 * <ol>
 *   <li><b>Envoi OTP</b> – {@link #sendOtp(String)} : vérifie la disponibilité du numéro et envoie
 *       un code OTP.
 *   <li><b>Vérification OTP</b> – {@link #verifyOtp(String, String)} : valide et consomme l'OTP.
 *   <li><b>Inscription complète</b> – {@link #register(RegisterCompleteRequest)} : crée le compte
 *       dans Keycloak, persiste en base PostgreSQL et envoie l'email de bienvenue.
 * </ol>
 */
public interface RegistrationService {

  /**
   * Étape 1 – Vérifie la disponibilité du numéro de téléphone et envoie un OTP {@code REGISTRATION}
   * par SMS.
   *
   * <p>Lève une exception si le numéro est déjà associé à un compte existant. Tout OTP {@code
   * REGISTRATION} non utilisé pour ce numéro est invalidé avant génération.
   *
   * @param phone numéro de téléphone au format international (ex : {@code +221781234567})
   * @throws CustomException si le numéro est déjà enregistré ({@code
   *     USER_CREATE_FAILURE_ALREADY_EXISTS})
   */
  void sendOtp(String phone) throws CustomException;

  /**
   * Étape 2 – Valide et consomme le code OTP reçu par SMS.
   *
   * <p>Marque l'OTP comme utilisé ({@code used = true}) pour empêcher toute réutilisation. Le
   * numéro de téléphone est considéré comme vérifié après cette étape.
   *
   * @param phone numéro de téléphone
   * @param code code OTP à 6 chiffres reçu par SMS
   * @throws CustomException si aucun OTP valide n'est trouvé ({@code OTP_NOT_FOUND}) ou si le code
   *     est incorrect ({@code OTP_INVALID})
   */
  void verifyOtp(String phone, String code) throws CustomException;

  /**
   * Étape 3 – Finalise l'inscription de l'utilisateur.
   *
   * <p>Séquence d'opérations :
   *
   * <ol>
   *   <li>Création du compte dans Keycloak (username = numéro de téléphone).
   *   <li>Attribution du rôle {@code UBAX_CLIENT} dans Keycloak.
   *   <li>Persistance de l'utilisateur dans PostgreSQL ({@code administrative.users}).
   *   <li>Envoi de l'email de bienvenue si un email a été fourni.
   * </ol>
   *
   * <p>En cas d'échec après la création Keycloak, le compte Keycloak est supprimé automatiquement
   * (rollback manuel) pour garantir la cohérence.
   *
   * @param request informations complètes : prénom, nom, téléphone, email (optionnel), mot de passe
   * @return données de l'utilisateur créé (UUID local, keycloakId, rôles assignés)
   * @throws CustomException si l'email ou le téléphone est déjà utilisé, ou si Keycloak échoue
   */
  RegisterResponse register(RegisterCompleteRequest request) throws CustomException;
}
