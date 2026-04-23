package com.africa.ubaxplatform.auth.codeList;

/**
 * Sous-rôles opérationnels du back-office d'administration de la plateforme UBAX.
 *
 * <p>Ces rôles sont persistés dans {@code user_sub_roles} (scope {@code ubax_internal}) et non dans
 * Keycloak. Ils affinent les permissions à l'intérieur du back-office en complément des rôles
 * Keycloak {@code UBAX_ADMIN} et {@code UBAX_SUPER_ADMIN}.
 *
 * <pre>
 * ┌────────────────────┬──────────────────────────────────────────────────────────────────┐
 * │ Sous-rôle          │ Périmètre                                                        │
 * ├────────────────────┼──────────────────────────────────────────────────────────────────┤
 * │ DIRECTEUR_GENERAL  │ Vision globale — KPIs, revenus, partenaires, config système      │
 * │ SUPPORT_CLIENT     │ Tickets et réclamations partenaires et clients                   │
 * │ OPERATIONS         │ Onboarding partenaires, modération des annonces                  │
 * │ FINANCE            │ Abonnements, commissions, revenus, rapports financiers            │
 * │ COMMERCIAL         │ Acquisition partenaires, suivi commercial                        │
 * └────────────────────┴──────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see RoleScope#UBAX_INTERNAL
 * @see UserRole#ADMIN
 * @see UserRole#SUPER_ADMIN
 */
public enum UbaxAdminRole {

  /**
   * Directeur Général UBAX.
   *
   * <p>Accès complet au back-office : KPIs globaux, revenus de la plateforme, gestion des
   * partenaires, configuration système. Seul rôle admin hors {@code SUPER_ADMIN} ayant une vision
   * financière globale.
   */
  DIRECTEUR_GENERAL,

  /**
   * Support Client.
   *
   * <p>Traitement des tickets et réclamations remontés par les partenaires (agences, hôtels) et par
   * les clients. Escalade vers les équipes techniques si nécessaire.
   */
  SUPPORT_CLIENT,

  /**
   * Opérations.
   *
   * <p>Onboarding des partenaires (validation des dossiers d'adhésion, activation des comptes),
   * modération des annonces publiées sur la plateforme.
   */
  OPERATIONS,

  /**
   * Finance.
   *
   * <p>Suivi des abonnements partenaires, des commissions prélevées sur les transactions, des
   * revenus globaux et génération des rapports financiers.
   */
  FINANCE,

  /**
   * Commercial.
   *
   * <p>Acquisition de nouveaux partenaires, suivi du pipeline commercial, gestion des offres et
   * promotions pour les agences et hôtels.
   */
  COMMERCIAL
}
