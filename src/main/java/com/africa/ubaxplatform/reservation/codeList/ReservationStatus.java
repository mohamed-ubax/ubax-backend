package com.africa.ubaxplatform.reservation.codeList;

public enum ReservationStatus {
  PENDING, // Soumise par le client, en attente de confirmation hôtel
  CONFIRMED, // Confirmée par l'hôtel
  CANCELLED, // Annulée (client ou hôtel)
  COMPLETED, // Séjour terminé
  NO_SHOW // Client ne s'est pas présenté
}
