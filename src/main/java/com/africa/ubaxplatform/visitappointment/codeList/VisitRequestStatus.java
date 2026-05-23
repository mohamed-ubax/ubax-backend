package com.africa.ubaxplatform.visitappointment.codeList;

/**
 * Énumération des statuts d'une demande de visite.
 *
 * <p>Workflow : PENDING → CONFIRMED / REJECTED | CANCELLED
 */
public enum VisitRequestStatus {
  /** En attente de réponse de l'agence */
  PENDING,

  /** Visite confirmée par l'agence */
  CONFIRMED,

  /** Visite rejetée par l'agence */
  REJECTED,

  /** Visite annulée par le client */
  CANCELLED,

  /** Visite effectuée (archive) */
  COMPLETED
}
