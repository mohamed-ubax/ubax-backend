package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôle fonctionnel d'un utilisateur sur la plateforme UBAX. Synchronisé depuis le realm Keycloak
 * ({@code UBAX_<name>}) et dupliqué en base pour les requêtes métier sans appel supplémentaire à
 * Keycloak.
 *
 * <p>La distinction agence / hôtel n'est <b>pas</b> portée par ce rôle mais par {@link
 * com.africa.ubaxplatform.auth.codeList.PartnerRole} stocké en base ({@code users.partner_role}).
 */
public enum UserRole {
  /** Acheteur ou locataire cherchant un bien. */
  CLIENT,
  /** Propriétaire particulier gérant ses propres biens. */
  OWNER,
  /** Administrateur de la plateforme. */
  ADMIN,
  /** Super Administrateur de la plateforme. */
  SUPER_ADMIN,
  /**
   * Partenaire de la plateforme (agence immobilière ou hôtel) – compte créé automatiquement à
   * l'approbation de sa demande d'adhésion. Le type de structure et le rôle interne sont gérés via
   * {@link com.africa.ubaxplatform.auth.codeList.PartnerRole}.
   */
  PARTNER
}
