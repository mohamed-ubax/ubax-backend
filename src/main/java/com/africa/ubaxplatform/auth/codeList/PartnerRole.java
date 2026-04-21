package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôles internes des partenaires UBAX pour le pilotage de leur propre structure.
 *
 * <p>Ces rôles s'appliquent aux membres de l'équipe d'une agence immobilière ou d'un hôtel
 * partenaire. Ils gouvernent les accès au portail partenaire UBAX, distinct du back-office
 * d'administration de la plateforme ({@link UbaxAdminRole}).
 *
 * <p>Les rôles agence et hôtel sont regroupés dans ce même enum car un partenaire peut gérer les
 * deux types de structures. La distinction agence / hôtel est portée par le contexte applicatif
 * (tenant ou type de partenaire), non par le rôle lui-même.
 *
 * <pre>
 * ╔══════════════════════════════════════════════════════════════════════════════════════╗
 * ║  AGENCE IMMOBILIÈRE                                                                  ║
 * ╠══════════════════════════╦═══════════════════════════════════════════════════════════╣
 * ║ Rôle                     ║ Périmètre d'accès                                        ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ DIRECTEUR_AGENCE         ║ Accès complet : dashboard global, biens, réservations,   ║
 * ║                          ║ finances (solde visible), employés, archives             ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ COMMERCIAL               ║ Prospection, rendez-vous, dossiers conclus, ajout biens  ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ COMPTABLE_AGENCE         ║ Finances uniquement : revenus, loyers, dépenses,         ║
 * ║                          ║ transactions (solde masqué •••)                          ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ AGENT_SAV                ║ Support : tickets ouverts/en cours/résolus, techniciens  ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ╔══════════════════════════════════════════════════════════════════════════════════════╗
 * ║  HÔTEL                                                                               ║
 * ╠══════════════════════════╦═══════════════════════════════════════════════════════════╣
 * ║ Rôle                     ║ Périmètre d'accès                                        ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ GERANT_HOTEL             ║ Accès complet : dashboard, réservations, espaces,        ║
 * ║                          ║ clients, employés, facturation                           ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ RECEPTIONNISTE           ║ Réservations uniquement : arrivées, départs, check-in    ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ COMPTABLE_HOTEL          ║ Facturation et revenus hôtel                             ║
 * ╠══════════════════════════╬═══════════════════════════════════════════════════════════╣
 * ║ RESPONSABLE_HEBERGEMENT  ║ Gestion des espaces / chambres                           ║
 * ╚══════════════════════════╩═══════════════════════════════════════════════════════════╝
 * </pre>
 */
public enum PartnerRole {

  // ══════════════════════════════════════════════════════════════
  //  AGENCE IMMOBILIÈRE
  // ══════════════════════════════════════════════════════════════

  /**
   * Directeur d'Agence (Directeur Général).
   *
   * <p>Accès complet au portail agence : tableau de bord global (CA, loyers, vacance), gestion des
   * biens immobiliers, réservations, finances avec solde en clair, gestion des employés et accès
   * aux archives. Peut ajouter, modifier et archiver tous les éléments.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  DIRECTEUR_AGENCE,

  /**
   * Commercial.
   *
   * <p>Dashboard orienté prospection : suivi des prospects, gestion des rendez-vous, dossiers
   * conclus, demandes clientèles. Peut ajouter et modifier des biens. N'a pas accès aux finances ni
   * aux archives.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  COMMERCIAL,

  /**
   * Comptable Agence.
   *
   * <p>Accès restreint aux finances de l'agence : suivi des revenus locatifs, historique des
   * transactions, liste des loyers en retard, ajout de dépenses. Le solde global est masqué ({@code
   * •••}) — visible uniquement pour le Directeur. Pas d'accès aux biens ni aux réservations.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  COMPTABLE_AGENCE,

  /**
   * Agent SAV (Service Après-Vente).
   *
   * <p>Dashboard support : tickets ouverts, en cours, résolus et urgents. Gestion des techniciens
   * d'intervention. Accès aux demandes clientèles pour traitement. Pas d'accès aux biens ni aux
   * finances.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  AGENT_SAV,

  // ══════════════════════════════════════════════════════════════
  //  HÔTEL
  // ══════════════════════════════════════════════════════════════

  /**
   * Gérant d'Hôtel.
   *
   * <p>Accès complet au portail hôtel : tableau de bord (taux d'occupation, revenus), gestion des
   * réservations, espaces / chambres, clients, employés et facturation. Équivalent du {@link
   * #DIRECTEUR_AGENCE} pour le contexte hôtelier.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  GERANT_HOTEL,

  /**
   * Réceptionniste.
   *
   * <p>Gestion opérationnelle des réservations : check-in, check-out, arrivées du jour, départs
   * prévus. Accès en consultation aux fiches clients. Pas d'accès aux données financières ni à la
   * gestion des employés.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  RECEPTIONNISTE,

  /**
   * Comptable Hôtel.
   *
   * <p>Suivi de la facturation, des revenus par période et des rapports financiers de l'hôtel. Pas
   * d'accès aux réservations en cours ni à la gestion des espaces.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  COMPTABLE_HOTEL,

  /**
   * Responsable Hébergement.
   *
   * <p>Gestion des espaces et chambres : création, modification, tarification, disponibilités.
   * Accès en lecture aux réservations pour vérifier l'occupation. Pas d'accès aux finances ni à la
   * gestion des employés.
   *
   * <p>Keycloak realm role parent : {@code UBAX_PARTNER}
   */
  RESPONSABLE_HEBERGEMENT
}
