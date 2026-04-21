package com.africa.ubaxplatform.auth.codeList;

/**
 * Finalité d'un code OTP (One-Time Password) envoyé par SMS à l'utilisateur.
 *
 * <p>Chaque code OTP généré par la plateforme est associé à l'une de ces finalités afin d'éviter
 * qu'un code destiné à l'inscription ne soit réutilisé pour une réinitialisation de mot de passe
 * (et vice-versa). La vérification côté service compare la finalité stockée en base avec la
 * finalité attendue avant de valider le code.
 *
 * <p><b>Durée de validité :</b> configurable via {@code otp.expiration-minutes} dans {@code
 * application.yml}. Par défaut : 5 minutes.
 *
 * @see com.africa.ubaxplatform.auth.service.interfaces.OtpService
 */
public enum OtpPurpose {

  /**
   * Vérification du numéro de téléphone lors de l'inscription.
   *
   * <p>Envoyé après la création du compte pour confirmer que le numéro de téléphone saisi
   * appartient bien à l'utilisateur. Le compte reste en statut {@code phoneVerified = false} tant
   * que ce code n'est pas validé.
   */
  REGISTRATION,

  /**
   * Réinitialisation du mot de passe via SMS.
   *
   * <p>Envoyé à la demande de l'utilisateur qui ne peut plus se connecter. La validation de ce code
   * autorise la saisie d'un nouveau mot de passe dans Keycloak via le flux de récupération de
   * compte.
   */
  PASSWORD_RESET
}
