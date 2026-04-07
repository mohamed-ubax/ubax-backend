package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;

/**
 * Contrat du service de réinitialisation de mot de passe via OTP SMS.
 *
 * <p>Définit le flux en 3 étapes pour la récupération de mot de passe mobile :
 *
 * <ol>
 *   <li><b>Envoi OTP</b> – {@link #sendResetOtp(String)} : vérifie l'existence du compte dans
 *       Keycloak et envoie un code OTP par SMS.
 *   <li><b>Vérification OTP</b> – {@link #verifyResetOtp(String, String)} : valide le code sans le
 *       consommer, permettant à l'UI de passer à l'étape suivante.
 *   <li><b>Réinitialisation</b> – {@link #resetPassword(String, String, String)} : consomme l'OTP
 *       et met à jour le mot de passe dans Keycloak.
 * </ol>
 */
public interface PasswordResetService {

  /**
   * Étape 1 – Vérifie que le compte existe dans Keycloak puis envoie un OTP {@code PASSWORD_RESET}
   * par SMS.
   *
   * <p>Tout OTP {@code PASSWORD_RESET} non utilisé et encore valide pour ce numéro est invalidé
   * avant d'en générer un nouveau.
   *
   * @param phone numéro de téléphone au format international (ex : {@code +221781234567})
   * @throws CustomException si aucun compte Keycloak ne correspond au numéro ({@code
   *     USER_NOT_FOUND})
   */
  void sendResetOtp(String phone) throws CustomException;

  /**
   * Étape 2 – Vérifie la validité du code OTP sans le consommer.
   *
   * <p>Cette étape permet à l'interface mobile de valider le code puis d'afficher le formulaire de
   * saisie du nouveau mot de passe sans risque que l'OTP soit considéré comme utilisé trop tôt.
   *
   * @param phone numéro de téléphone
   * @param code code OTP à 6 chiffres reçu par SMS
   * @throws CustomException si aucun OTP valide n'est trouvé ({@code OTP_NOT_FOUND}) ou si le code
   *     est incorrect ({@code OTP_INVALID})
   */
  void verifyResetOtp(String phone, String code) throws CustomException;

  /**
   * Étape 3 – Consomme l'OTP et réinitialise le mot de passe dans Keycloak.
   *
   * <p>Vérifie à nouveau le code (sécurité contre les attaques par rejeu), marque l'OTP comme
   * utilisé, puis appelle l'API Admin Keycloak pour mettre à jour le mot de passe de façon
   * permanente ({@code temporary = false}).
   *
   * @param phone numéro de téléphone
   * @param code code OTP reçu par SMS (doit correspondre au dernier OTP valide)
   * @param newPassword nouveau mot de passe en clair respectant la politique Keycloak
   * @throws CustomException si l'OTP est invalide, expiré ou si le compte est introuvable
   */
  void resetPassword(String phone, String code, String newPassword) throws CustomException;
}
