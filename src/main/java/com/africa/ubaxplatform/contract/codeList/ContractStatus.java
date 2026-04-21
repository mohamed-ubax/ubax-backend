package com.africa.ubaxplatform.contract.codeList;

/**
 * Cycle de vie d'un contrat de location ou de vente sur la plateforme UBAX.
 *
 * <p>Un contrat transite par les statuts suivants depuis sa création jusqu'à sa clôture :
 *
 * <pre>
 *  DRAFT
 *    │
 *    ▼
 *  PENDING_SIGNATURE ──────────────────► CANCELLED
 *    │
 *    ▼
 *  ACTIVE ──────────────────────────────► TERMINATED (résiliation anticipée)
 *    │
 *    ▼
 *  EXPIRED  (fin de bail sans renouvellement)
 * </pre>
 *
 * <p><b>Règles métier :</b>
 * <ul>
 *   <li>Un contrat {@code ACTIVE} génère automatiquement les paiements {@code PENDING} mensuels
 *       via le {@code PaymentSchedulerJob}.
 *   <li>Seul un contrat {@code ACTIVE} peut être lié à des tickets de maintenance.
 *   <li>Un contrat {@code TERMINATED} ou {@code EXPIRED} libère le bien (statut {@code PUBLISHED}).
 * </ul>
 */
public enum ContractStatus {

  /**
   * Brouillon – contrat en cours de rédaction.
   *
   * <p>Le contrat a été créé mais n'est pas encore soumis à la signature. Les clauses et les
   * informations peuvent encore être modifiées librement. Aucun paiement n'est généré à ce stade.
   */
  DRAFT,

  /**
   * En attente de signature par les parties.
   *
   * <p>Le contrat est finalisé et envoyé aux signataires (locataire et propriétaire / agence).
   * Aucune modification des clauses n'est possible. Le bien est réservé ({@code RESERVED}) mais
   * pas encore loué. Si aucune signature n'intervient dans le délai imparti, le contrat peut
   * passer en {@code CANCELLED}.
   */
  PENDING_SIGNATURE,

  /**
   * Contrat actif – bail en cours d'exécution.
   *
   * <p>Toutes les parties ont signé. Le locataire occupe le bien et les loyers sont générés
   * mensuellement. C'est l'état nominal d'un bail en cours. Le bien est marqué {@code RESERVED}
   * dans ce statut.
   */
  ACTIVE,

  /**
   * Résilié avant terme à l'initiative de l'une des parties.
   *
   * <p>La résiliation anticipée peut être demandée par le locataire (préavis) ou par le
   * propriétaire / l'agence (motif légal). La date de résiliation effective et le motif sont
   * enregistrés sur le contrat. Le bien repasse en statut {@code PUBLISHED} ou {@code ARCHIVED}
   * selon la décision du propriétaire.
   */
  TERMINATED,

  /**
   * Contrat arrivé à échéance naturelle.
   *
   * <p>La durée contractuelle est écoulée et le bail n'a pas été reconduit. Aucune action n'est
   * requise de la part des parties. Le bien est libéré automatiquement et peut être republié.
   */
  EXPIRED,

  /**
   * Contrat annulé avant toute prise d'effet.
   *
   * <p>Applicable uniquement aux contrats en statut {@code DRAFT} ou {@code PENDING_SIGNATURE}.
   * L'annulation peut être effectuée par l'agence, le propriétaire ou un administrateur. Un
   * contrat {@code ACTIVE} ne peut pas être annulé directement — il doit passer par
   * {@code TERMINATED}.
   */
  CANCELLED
}
