package com.africa.ubaxplatform.ticketing.codeList;

/**
 * Cycle de vie d'un ticket de maintenance ou de SAV sur la plateforme UBAX.
 *
 * <p>Un ticket est créé par un locataire dans le cadre d'un contrat actif pour signaler un
 * incident (fuite, panne électrique, problème de serrure, etc.). Il transite par les statuts
 * suivants jusqu'à sa clôture :
 *
 * <pre>
 *  OPEN
 *   │  (prise en charge par l'agent SAV)
 *   ▼
 *  IN_ANALYSIS
 *   │  (prestataire mandaté)
 *   ▼
 *  TECHNICIAN_SENT ──────────────────────► CANCELLED  (fausse alerte / résolu seul)
 *   │  (intervention effectuée)
 *   ▼
 *  RESOLVED
 *   │  (confirmation locataire ou délai dépassé)
 *   ▼
 *  CLOSED
 * </pre>
 *
 * <p><b>Notifications :</b> chaque transition de statut déclenche une notification au locataire
 * via le module {@code notification} (email + SMS).
 *
 * <p><b>Indicateurs de performance (SLA) :</b> le délai entre {@code OPEN} et {@code RESOLVED}
 * est mesuré pour alimenter les KPIs du tableau de bord SAV de l'agence.
 *
 * @see com.africa.ubaxplatform.ticketing.entity.Ticket
 */
public enum TicketStatus {

  /**
   * Ticket ouvert – incident signalé par le locataire.
   *
   * <p>Le locataire a déclaré l'incident avec titre, description, catégorie et niveau d'urgence.
   * Des photos ou vidéos peuvent être jointes. Le ticket n'est pas encore pris en charge par
   * l'équipe SAV. Apparaît dans la file d'attente de l'agent SAV.
   */
  OPEN,

  /**
   * En cours d'analyse par l'agent SAV.
   *
   * <p>Un agent SAV a pris en charge le ticket et est en train d'évaluer la nature et la gravité
   * de l'incident. Il peut poser des questions au locataire via la messagerie interne, consulter
   * l'historique du bien et décider si un prestataire doit intervenir.
   */
  IN_ANALYSIS,

  /**
   * Technicien mandaté et envoyé sur site.
   *
   * <p>Un prestataire technique externe a été désigné. Son nom, numéro de téléphone et la date
   * d'intervention planifiée sont renseignés sur le ticket. Le locataire est notifié pour être
   * présent. L'agent SAV suit l'avancement de l'intervention.
   */
  TECHNICIAN_SENT,

  /**
   * Incident résolu – en attente de confirmation du locataire.
   *
   * <p>Le technicien a effectué l'intervention et l'agent SAV a marqué le ticket comme résolu.
   * Le coût de réparation et la note de résolution sont renseignés. Le locataire est notifié et
   * invité à confirmer la résolution. Si aucune réponse dans le délai imparti, le ticket passe
   * automatiquement en {@code CLOSED}.
   */
  RESOLVED,

  /**
   * Ticket clôturé définitivement.
   *
   * <p>Le locataire a confirmé que l'incident est résolu, ou le délai de confirmation automatique
   * est écoulé. Le locataire peut laisser une évaluation (note de 1 à 5 et commentaire). Les
   * données du ticket sont conservées pour les statistiques de maintenance et les rapports
   * comptables (imputation des coûts).
   */
  CLOSED,

  /**
   * Ticket annulé.
   *
   * <p>Applicable lorsque le locataire a déclaré une fausse alerte, a résolu le problème lui-même,
   * ou lorsque l'agent SAV a constaté que l'incident n'est pas de la responsabilité de l'agence.
   * Un ticket {@code CANCELLED} n'est plus traité et ne génère pas de coût.
   */
  CANCELLED
}
