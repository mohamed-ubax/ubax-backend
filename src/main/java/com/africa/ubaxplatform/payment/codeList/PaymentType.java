package com.africa.ubaxplatform.payment.codeList;

/**
 * Nature d'un paiement enregistré sur la plateforme UBAX.
 *
 * <p>Chaque paiement est catégorisé selon sa nature afin de permettre un suivi comptable précis et
 * d'alimenter les tableaux de bord financiers (revenus locatifs, commissions perçues, transactions
 * immobilières).
 *
 * <p>La référence générée automatiquement intègre ce type : {@code PAY-{ANNÉE}-{TYPE}-{UUID_8}}.
 * Exemple : {@code PAY-2025-RENT-A3F2B19C}.
 *
 * @see com.africa.ubaxplatform.payment.entity.Payment
 * @see com.africa.ubaxplatform.payment.service.interfaces.PaymentService
 */
public enum PaymentType {

  /**
   * Loyer mensuel dû par le locataire.
   *
   * <p>Paiement récurrent généré automatiquement chaque mois par le {@code PaymentSchedulerJob}
   * pour tout contrat en statut {@code ACTIVE}. Le montant correspond au loyer hors charges défini
   * dans le contrat. Le statut initial est {@code PENDING} jusqu'à encaissement.
   */
  RENT,

  /**
   * Caution versée par le locataire à l'entrée dans le logement.
   *
   * <p>Montant garantissant les dégradations éventuelles en fin de bail. Généralement équivalent à
   * 1 ou 2 mois de loyer selon les termes du contrat. La caution est restituée (totalement ou
   * partiellement) en fin de contrat après état des lieux de sortie.
   */
  DEPOSIT,

  /**
   * Charges locatives refacturées au locataire.
   *
   * <p>Couvrent les charges communes, la consommation d'eau, d'électricité ou d'autres services
   * mutualisés inclus dans le contrat. Peuvent être forfaitaires ou provisionnées avec
   * régularisation annuelle.
   */
  CHARGES,

  /**
   * Commission prélevée par l'agence sur une transaction.
   *
   * <p>Honoraires facturés par l'agence lors de la mise en relation entre propriétaire et locataire
   * (location) ou entre vendeur et acheteur (vente). Le taux de commission est défini dans le
   * mandat signé avec le propriétaire. Ces revenus alimentent directement le dashboard financier de
   * l'agence.
   */
  COMMISSION,

  /**
   * Paiement lié à une transaction de vente immobilière.
   *
   * <p>Acompte, solde ou paiement unique dans le cadre d'un acte de vente. Le bien passe en statut
   * {@code SOLD} une fois la transaction finalisée. Déclenche la génération automatique du document
   * {@code RECEIPT} de type {@code SALE}.
   */
  SALE
}
