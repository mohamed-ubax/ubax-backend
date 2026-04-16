package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôles internes du back-office d'administration de la plateforme UBAX.
 *
 * <p>Ces rôles s'appliquent aux membres de l'équipe UBAX qui gèrent la plateforme en tant que
 * super-structure (distinct de la gestion interne des agences et hôtels partenaires).
 *
 * <p>Correspondance Keycloak realm : {@code UBAX_SUPER_ADMIN} et {@code UBAX_ADMIN} couvrent les
 * accès au back-office. Les rôles plus fins ({@code FINANCE_MANAGER}, etc.) sont des sous-rôles
 * opérationnels attribués en complément.
 *
 * <pre>
 * ┌─────────────────────┬────────────────────────────────────────────────────────────────┐
 * │ Rôle                │ Périmètre d'accès                                              │
 * ├─────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ SUPER_ADMIN         │ Accès total : config système, gestion des admins, logs, audit  │
 * │ ADMIN               │ Opérations courantes : partenaires, utilisateurs, signalements  │
 * │ FINANCE_MANAGER     │ Abonnements, commissions, revenus et rapports financiers        │
 * │ SUPPORT_MANAGER     │ Tickets et réclamations des partenaires et clients              │
 * │ PARTNER_MANAGER     │ Onboarding, activation et suspension des agences & hôtels       │
 * │ CONTENT_MODERATOR   │ Validation et modération des annonces publiées                  │
 * └─────────────────────┴────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public enum UbaxAdminRole {

  /**
   * Super Administrateur UBAX.
   *
   * <p>Accès intégral à toutes les fonctionnalités du back-office : configuration système,
   * paramètres du realm Keycloak, gestion des comptes administrateurs, consultation des logs
   * d'audit et des métriques globales de la plateforme.
   *
   * <p>Keycloak realm role : {@code UBAX_SUPER_ADMIN}
   */
  SUPER_ADMIN,

  /**
   * Administrateur Plateforme.
   *
   * <p>Gestion opérationnelle courante : consultation et modération des partenaires, gestion des
   * utilisateurs signalés, traitement des demandes d'inscription. Pas d'accès à la configuration
   * système ni à la gestion des autres administrateurs.
   *
   * <p>Keycloak realm role : {@code UBAX_ADMIN}
   */
  ADMIN,

  /**
   * Responsable Finances Plateforme.
   *
   * <p>Suivi des abonnements partenaires, des commissions prélevées sur les transactions, des
   * revenus globaux et génération des rapports financiers. Accès en lecture aux données de
   * facturation des partenaires.
   *
   * <p>Keycloak realm role : {@code UBAX_ADMIN} (sous-rôle opérationnel)
   */
  FINANCE_MANAGER,

  /**
   * Responsable Support Plateforme.
   *
   * <p>Traitement des tickets et réclamations remontés par les partenaires (agences, hôtels) et par
   * les clients. Escalade vers les équipes techniques si nécessaire. Pas d'accès aux données
   * financières.
   *
   * <p>Keycloak realm role : {@code UBAX_ADMIN} (sous-rôle opérationnel)
   */
  SUPPORT_MANAGER,

  /**
   * Gestionnaire Partenaires.
   *
   * <p>Suivi du cycle de vie des partenaires : onboarding (validation des dossiers d'adhésion),
   * activation des comptes, suspension en cas de non-conformité, et suivi de la performance des
   * agences et hôtels inscrits sur la plateforme.
   *
   * <p>Keycloak realm role : {@code UBAX_ADMIN} (sous-rôle opérationnel)
   */
  PARTNER_MANAGER,

  /**
   * Modérateur de Contenu.
   *
   * <p>Validation et modération des annonces (biens immobiliers, offres hôtelières) publiées par
   * les partenaires. Peut masquer ou rejeter un contenu non conforme à la charte UBAX. Pas d'accès
   * aux données financières ni à la gestion des comptes utilisateurs.
   *
   * <p>Keycloak realm role : {@code UBAX_ADMIN} (sous-rôle opérationnel)
   */
  CONTENT_MODERATOR
}
