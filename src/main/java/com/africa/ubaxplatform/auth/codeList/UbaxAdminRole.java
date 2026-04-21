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
 *   <li>Les valeurs de cet enum sont des <em>sous-rôles opérationnels</em> qui reflètent
 *       l'organisation interne UBAX. Ils ne correspondent pas à des rôles realm Keycloak distincts
 *       et ne sont pas persistés dans {@code User.roles} — ils sont portés par un attribut
 *       applicatif complémentaire.
 * </ul>
 *
 * <pre>
 * ┌──────────────────────┬────────────────────────────────────────────────────────────────┐
 * │ Rôle (UserRole)      │ Périmètre                                                      │
 * ├──────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ SUPER_ADMIN          │ Accès total : config système, gestion des admins, logs, audit  │
 * │ ADMIN                │ Opérations courantes : partenaires, utilisateurs, signalements  │
 * ├──────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ Sous-rôle            │ Périmètre affiné (attribué en complément d'ADMIN)              │
 * ├──────────────────────┼────────────────────────────────────────────────────────────────┤
 * │ DIRECTEUR_GENERAL    │ Vision globale plateforme, accès total back-office              │
 * │ SUPPORT_CLIENT       │ Tickets, réclamations, relation partenaires et clients          │
 * │ OPERATIONS           │ Onboarding partenaires, modération, suivi opérationnel          │
 * │ FINANCE              │ Abonnements, commissions, revenus, rapports financiers          │
 * │ COMMERCIAL           │ Acquisition partenaires, suivi commercial, performance          │
 * └──────────────────────┴────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see UserRole#ADMIN
 * @see UserRole#SUPER_ADMIN
 */
public enum UbaxAdminRole {

  /**
   * Directeur Général UBAX.
   *
   * <p>Vision globale de la plateforme : accès complet au back-office, tableaux de bord consolidés
   * (revenus, partenaires, clients, opérations), gestion des admins et audit des actions.
   * Équivalent fonctionnel de {@link UserRole#SUPER_ADMIN} dans le contexte métier.
   *
   * <p>Requiert {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  DIRECTEUR_GENERAL,

  /**
   * Support Client UBAX.
   *
   * <p>Traitement des tickets et réclamations remontés par les partenaires (agences, hôtels) et par
   * les clients. Escalade vers les équipes techniques si nécessaire. Pas d'accès aux données
   * financières ni à la configuration système.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  SUPPORT_CLIENT,

  /**
   * Opérations UBAX.
   *
   * <p>Suivi du cycle de vie opérationnel : onboarding et validation des dossiers d'adhésion
   * partenaires, activation/suspension des comptes, modération des annonces publiées, et suivi de
   * la conformité des contenus à la charte UBAX.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  OPERATIONS,

  /**
   * Finance UBAX.
   *
   * <p>Suivi des abonnements partenaires, des commissions prélevées sur les transactions, des
   * revenus globaux et génération des rapports financiers. Accès en lecture aux données de
   * facturation des partenaires.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  FINANCE,

  /**
   * Commercial UBAX.
   *
   * <p>Acquisition et suivi des partenaires : prospection des agences et hôtels, relances,
   * performance commerciale, suivi des conversions de candidatures en comptes actifs.
   *
   * <p>Requiert {@link UserRole#ADMIN} ou {@link UserRole#SUPER_ADMIN} en rôle de base.
   */
  COMMERCIAL
}
