package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôles internes des membres d'un hôtel partenaire UBAX.
 *
 * <p>Ces rôles sont persistés dans {@code user_sub_roles} (scope {@code hotel}) et non dans
 * Keycloak. Ils affinent les droits d'accès au portail hôtel au-delà du rôle Keycloak {@code
 * UBAX_PARTNER}.
 *
 * <pre>
 * ┌──────────────────────────┬────────────────────────────────────────────────────────────┐
 * │ Rôle                     │ Périmètre d'accès                                          │
 * ├──────────────────────────┼────────────────────────────────────────────────────────────┤
 * │ GERANT_HOTEL             │ Accès complet : dashboard, réservations, espaces, équipe   │
 * │ RECEPTIONNISTE           │ Réservations : check-in/out, arrivées, départs             │
 * │ COMPTABLE_HOTEL          │ Facturation et revenus hôtel                               │
 * │ RESPONSABLE_HEBERGEMENT  │ Gestion des espaces et chambres                            │
 * └──────────────────────────┴────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see RoleScope#HOTEL
 */
public enum HotelRole {

  /** Gérant d'hôtel — accès complet, équivalent du DIRECTEUR_AGENCE pour les hôtels. */
  GERANT_HOTEL,

  /** Réceptionniste — gestion opérationnelle des réservations et check-in/out. */
  RECEPTIONNISTE,

  /** Comptable hôtel — facturation et revenus. Pas d'accès aux réservations en cours. */
  COMPTABLE_HOTEL,

  /** Responsable hébergement — espaces, chambres, tarification, disponibilités. */
  RESPONSABLE_HEBERGEMENT
}
