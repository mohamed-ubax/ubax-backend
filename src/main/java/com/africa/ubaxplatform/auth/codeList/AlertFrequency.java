package com.africa.ubaxplatform.auth.codeList;

/**
 * Fréquence d'envoi des alertes de nouveaux biens correspondant
 * aux critères de recherche de l'utilisateur.
 */
public enum AlertFrequency {
    /** Notification immédiate dès la publication du bien. */
    REALTIME,
    /** Récapitulatif quotidien des nouveaux biens correspondants. */
    DAILY,
    /** Récapitulatif hebdomadaire des nouveaux biens correspondants. */
    WEEKLY
}