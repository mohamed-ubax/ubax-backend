package com.africa.ubaxplatform.payment.codeList;

/**
 * Statut d'un paiement tout au long de son cycle de vie sur la plateforme UBAX.
 *
 * <p>Le statut initial est calculé automatiquement à la création selon les données fournies :
 *
 * <pre>
 *  paidDate + amountPaid ≥ amount  →  PAID
 *  paidDate + amountPaid &lt; amount  →  PARTIAL
 *  dueDate &lt; aujourd'hui           →  LATE
 *  sinon                           →  PENDING
 * </pre>
 *
 * <p>Transitions autorisées via {@code PATCH /v1/payments/{id}/status} :
 *
 * <pre>
 *  PENDING ──────────────────────────────► PAID
 *    │                                     │
 *    ├──► PARTIAL ──────────────────────► PAID  (auto si amountPaid ≥ amount)
 *    │
 *    ├──► LATE (passage automatique par le scheduler si dueDate dépassée)
 *    │
 *    └──► CANCELLED  (annulation avant tout encaissement)
 * </pre>
 *
 * <p><b>Règle de protection :</b> un paiement {@code PAID} ne peut pas être supprimé. Un paiement
 * {@code CANCELLED} ne peut plus être modifié.
 *
 * @see com.africa.ubaxplatform.payment.entity.Payment
 * @see com.africa.ubaxplatform.payment.service.interfaces.PaymentService
 */
public enum PaymentStatus {

  /**
   * Paiement attendu – échéance à venir ou non encore encaissé.
   *
   * <p>Statut par défaut lorsqu'aucun encaissement n'a été enregistré et que la date d'échéance
   * n'est pas encore dépassée. Les paiements en {@code PENDING} apparaissent dans le calendrier des
   * échéances du tableau de bord de l'agence.
   */
  PENDING,

  /**
   * Paiement intégralement encaissé.
   *
   * <p>Le montant payé ({@code amountPaid}) est supérieur ou égal au montant dû ({@code amount}) et
   * une date d'encaissement ({@code paidDate}) est renseignée. Un reçu PDF peut être généré
   * automatiquement et envoyé au locataire. Cet état est <b>définitif</b> : un paiement encaissé ne
   * peut ni être modifié ni supprimé.
   */
  PAID,

  /**
   * Paiement en retard – échéance dépassée sans encaissement complet.
   *
   * <p>La date d'échéance ({@code dueDate}) est passée et le paiement est toujours {@code PENDING}
   * ou {@code PARTIAL}. Passage automatique géré par le {@code PaymentSchedulerJob}. Les paiements
   * en retard sont listés sur le tableau de bord de l'agence avec les coordonnées du locataire
   * concerné pour relance.
   */
  LATE,

  /**
   * Paiement partiellement encaissé.
   *
   * <p>Un acompte a été versé mais le montant payé ({@code amountPaid}) est inférieur au montant
   * total dû ({@code amount}). Le solde restant est toujours attendu. Si un versement
   * complémentaire porte {@code amountPaid ≥ amount}, le statut est automatiquement corrigé en
   * {@code PAID} lors de la mise à jour.
   */
  PARTIAL,

  /**
   * Paiement annulé avant tout encaissement.
   *
   * <p>Applicable uniquement aux paiements en statut {@code PENDING}, {@code PARTIAL} ou {@code
   * LATE}. Un paiement annulé est définitivement figé : aucune modification ultérieure n'est
   * possible. Utilisé pour supprimer logiquement un doublon ou une erreur de saisie tout en
   * conservant la traçabilité.
   */
  CANCELLED
}
