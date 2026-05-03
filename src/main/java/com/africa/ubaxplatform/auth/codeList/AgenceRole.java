package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôles internes des membres d'une agence immobilière partenaire UBAX.
 *
 * <p>Ces rôles sont persistés dans {@code user_sub_roles} (scope {@code agence}) et non dans
 * Keycloak. Ils affinent les droits d'accès au portail agence au-delà du rôle Keycloak {@code
 * UBAX_PARTNER}.
 *
 * <pre>
 * ┌──────────────────────┬──────────────────────────────────────────────────────────────┐
 * │ Rôle                 │ Périmètre d'accès                                            │
 * ├──────────────────────┼──────────────────────────────────────────────────────────────┤
 * │ DIRECTEUR_AGENCE     │ Accès complet : dashboard global, biens, finances, équipe    │
 * │ COMMERCIAL           │ Prospects, rendez-vous, dossiers, ajout biens                │
 * │ COMPTABLE_AGENCE     │ Finances uniquement (solde masqué pour non-directeur)        │
 * │ AGENT_SAV            │ Tickets, techniciens, support client                         │
 * └──────────────────────┴──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see RoleScope#AGENCE
 */
public enum AgenceRole {

  /** Directeur d'agence — accès complet incluant finances et gestion de l'équipe. */
  DIRECTEUR_AGENCE,

  /** Commercial — prospection, rendez-vous, dossiers. Pas d'accès aux finances. */
  COMMERCIAL,

  /** Comptable agence — finances uniquement. Pas d'accès aux biens ni réservations. */
  COMPTABLE_AGENCE,

  /** Agent SAV — tickets de maintenance et support. Pas d'accès aux finances. */
  AGENT_SAV,

  /** Agent immobilier — gestion des biens, visites et dossiers locataires/acheteurs. */
  AGENT_IMMOBILIER
}
