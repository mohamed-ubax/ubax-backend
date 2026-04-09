package com.africa.ubaxplatform.partner.codeList;

/** Cycle de vie d'une demande d'adhésion partenaire. */
public enum ApplicationStatus {

  /** Demande soumise, en attente de traitement par un administrateur. */
  PENDING,

  /** Un administrateur a démarré l'examen de la demande. */
  UNDER_REVIEW,

  /** Dossier incomplet – le partenaire doit fournir des informations ou documents manquants. */
  INCOMPLETE,

  /** Demande approuvée – le partenaire est intégré à la plateforme. */
  APPROVED,

  /** Demande rejetée définitivement. */
  REJECTED
}
