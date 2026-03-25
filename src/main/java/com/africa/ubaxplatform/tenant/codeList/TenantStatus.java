package com.africa.ubaxplatform.tenant.codeList;

public enum TenantStatus {
  /** Dossier en cours de constitution, documents manquants. */
  INCOMPLETE,
  /** Dossier complet soumis, en attente de vérification. */
  PENDING_REVIEW,
  /** Dossier validé, locataire autorisé à signer un bail. */
  QUALIFIED,
  /** Dossier refusé par le gestionnaire. */
  REJECTED,
  /** Locataire banni suite à des impayés ou incidents graves. */
  BLACKLISTED
}
