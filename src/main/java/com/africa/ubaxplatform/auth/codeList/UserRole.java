package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôle Keycloak d'un utilisateur sur la plateforme UBAX.
 *
 * <p>Chaque valeur correspond exactement à un rôle realm Keycloak préfixé {@code UBAX_} (ex :
 * {@code UBAX_PARTNER}). Ces rôles sont synchronisés depuis le JWT à chaque requête et dupliqués en
 * base pour les requêtes métier.
 *
 * <p>Les accès granulaires à l'intérieur d'un rôle sont gérés par les sous-rôles persistés dans
 * {@code user_sub_roles} :
 *
 * <ul>
 *   <li>{@code ADMIN} / {@code SUPER_ADMIN} → sous-rôles {@link UbaxAdminRole} (scope {@code
 *       ubax_internal})
 *   <li>{@code PARTNER} agence → sous-rôles {@link AgenceRole} (scope {@code agence})
 *   <li>{@code PARTNER} hôtel → sous-rôles {@link HotelRole} (scope {@code hotel})
 *   <li>{@code OWNER} / {@code CLIENT} → pas de sous-rôles
 * </ul>
 */
public enum UserRole {

  /** Acheteur ou locataire cherchant un bien. */
  CLIENT,

  /** Propriétaire particulier gérant ses propres biens. */
  OWNER,

  /** Administrateur de la plateforme (back-office UBAX). */
  ADMIN,

  /** Super Administrateur — accès total, gestion des admins et de la configuration. */
  SUPER_ADMIN,

  /**
   * Partenaire de la plateforme (agence immobilière ou hôtel).
   *
   * <p>Le type de structure (agence vs hôtel) et le rôle interne sont portés par les sous-rôles
   * dans {@code user_sub_roles} — scope {@code agence} ou {@code hotel}.
   */
  PARTNER,

  /**
   * Fondateur d'une agence ou d'un hôtel partenaire.
   *
   * <p>Assigné automatiquement lors de l'approbation de la demande d'adhésion, en complément de
   * {@code PARTNER}. Ce rôle identifie l'utilisateur qui a soumis la demande et lui permet de
   * s'auto-assigner le sous-rôle DG/GERANT et d'inviter les premiers membres de son équipe, avant
   * même d'avoir un sous-rôle attribué.
   *
   * <p>Les membres invités par la suite reçoivent uniquement {@code PARTNER}.
   */
  PARTNER_ADMIN
}
