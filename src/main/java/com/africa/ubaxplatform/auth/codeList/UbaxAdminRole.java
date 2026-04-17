package com.africa.ubaxplatform.auth.codeList;

/**
 * Sous-rôles opérationnels du back-office d'administration de la plateforme UBAX.
 *
 * <p><b>Distinction avec {@link UserRole} :</b>
 *
 * <ul>
 *   <li>{@link UserRole#ADMIN} et {@link UserRole#SUPER_ADMIN} sont les rôles techniques persistés
 *       en base et synchronisés avec Keycloak ({@code UBAX_ADMIN}, {@code UBAX_SUPER_ADMIN}). Ils
 *       gouvernent l'accès au back-office en général.
 *   <li>Les valeurs de cet enum ({@code FINANCE_MANAGER}, etc.) sont des <em>sous-rôles
 *       opérationnels</em> qui affinent les permissions à l'intérieur du back-office. Ils ne
 *       correspondent pas à des rôles realm Keycloak distincts et ne sont pas persistés dans {@code
 *       User.roles} — ils sont portés par un attribut applicatif complémentaire.
 * </ul>
 *
 * <pre>
 * ┌─────────────────────┬────────────────────────────────────────────────────────────────┐
 * │ Rôle (UserRole)     │ Périmètre                                                      │
 * ├─────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ SUPER_ADMIN         │ Accès total : config système, gestion des admins, logs, audit  │
 * │ ADMIN               │ Opérations courantes : partenaires, utilisateurs, signalements  │
 * ├─────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ Sous-rôle           │ Périmètre affiné (attribué en complément d'ADMIN)              │
 * ├─────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ FINANCE_MANAGER     │ Abonnements, commissions, revenus et rapports financiers        │
 * │ SUPPORT_MANAGER     │ Tickets et réclamations des partenaires et clients              │
 * │ PARTNER_MANAGER     │ Onboarding, activation et suspension des agences & hôtels       │
 * │ CONTENT_MODERATOR   │ Validation et modération des annonces publiées                  │
 * └─────────────────────┴────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see UserRole#ADMIN
 * @see UserRole#SUPER_ADMIN
 */
public enum UbaxAdminRole {

  /**
   * Responsable Finances Plateforme.
   *
   * <p>Suivi des abonnements partenaires, des commissions prélevées sur les transactions, des
   * revenus globaux et génération des rapports financiers. Accès en lecture aux données de
   * facturation des partenaires.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  FINANCE_MANAGER,

  /**
   * Responsable Support Plateforme.
   *
   * <p>Traitement des tickets et réclamations remontés par les partenaires (agences, hôtels) et par
   * les clients. Escalade vers les équipes techniques si nécessaire. Pas d'accès aux données
   * financières.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  SUPPORT_MANAGER,

  /**
   * Gestionnaire Partenaires.
   *
   * <p>Suivi du cycle de vie des partenaires : onboarding (validation des dossiers d'adhésion),
   * activation des comptes, suspension en cas de non-conformité, et suivi de la performance des
   * agences et hôtels inscrits sur la plateforme.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  PARTNER_MANAGER,

  /**
   * Modérateur de Contenu.
   *
   * <p>Validation et modération des annonces (biens immobiliers, offres hôtelières) publiées par
   * les partenaires. Peut masquer ou rejeter un contenu non conforme à la charte UBAX. Pas d'accès
   * aux données financières ni à la gestion des comptes utilisateurs.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  CONTENT_MODERATOR
}
