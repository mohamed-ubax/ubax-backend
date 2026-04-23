package com.africa.ubaxplatform.payment.codeList;

/**
 * Mode de règlement utilisé pour un paiement ou une dépense sur la plateforme UBAX.
 *
 * <p>Ce champ est optionnel à la création d'un paiement ({@code null} = mode non précisé à ce
 * stade) et peut être renseigné ou corrigé lors de la mise à jour du statut via {@code PATCH
 * /v1/payments/{id}/status}.
 *
 * <p>Les méthodes disponibles reflètent les usages courants en Afrique de l'Ouest où le Mobile
 * Money est dominant pour les règlements courants.
 */
public enum PaymentMethod {

  /**
   * Paiement en espèces.
   *
   * <p>Règlement physique remis directement à l'agence ou à l'agent. Nécessite l'émission d'un reçu
   * papier ou numérique. Mode courant pour les loyers inférieurs au seuil de traçabilité bancaire.
   */
  CASH,

  /**
   * Virement bancaire.
   *
   * <p>Transfert depuis le compte bancaire du locataire / acheteur vers le compte de l'agence. Mode
   * privilégié pour les montants importants (cautions, ventes). Un bordereau ou relevé bancaire
   * peut être joint comme justificatif ({@code receiptUrl}).
   */
  BANK_TRANSFER,

  /**
   * Paiement via Mobile Money (Orange Money, Wave, MTN MoMo, Free Money, etc.).
   *
   * <p>Mode de paiement dominant en Afrique de l'Ouest. L'opération s'effectue depuis l'application
   * mobile de l'opérateur. La référence de la transaction Mobile Money peut être enregistrée dans
   * le champ {@code reference} du paiement.
   */
  MOBILE_MONEY,

  /**
   * Paiement par chèque bancaire.
   *
   * <p>Chèque émis par le locataire à l'ordre de l'agence ou du propriétaire. L'encaissement n'est
   * effectif qu'après compensation bancaire. Le numéro de chèque peut être enregistré dans le champ
   * {@code reference} du paiement pour le suivi.
   */
  CHECK
}
