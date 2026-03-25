package com.africa.ubaxplatform.document.codeList;

public enum DocumentType {
  /** Facture émise pour un loyer, une commission ou un abonnement. */
  INVOICE,
  /** Reçu remis au payeur après validation du paiement. */
  RECEIPT,
  /** Contrat généré depuis un template (vente ou location). */
  CONTRACT,
  /** Bail de location spécifique avec clauses légales. */
  LEASE,
  /** État des lieux d'entrée ou de sortie. */
  INVENTORY,
  /** Rapport comptable, analytique ou d'activité. */
  REPORT,
  /** Relevé de compte ou historique de transactions. */
  STATEMENT,
  OTHER
}
