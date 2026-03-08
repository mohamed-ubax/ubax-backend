package com.africa.ubaxplatform.auth.codeList;

/**
 * Rôle fonctionnel d'un utilisateur sur la plateforme UBAX.
 * Synchronisé depuis le realm Keycloak et dupliqué en base
 * pour les requêtes métier sans appel supplémentaire à Keycloak.
 */
public enum UserRole {
    /** Acheteur ou locataire cherchant un bien. */
    CLIENT,
    /** Propriétaire particulier gérant ses propres biens. */
    OWNER,
    /** Agent immobilier rattaché à une agence. */
    AGENT,
    /** Compte principal d'une agence immobilière. */
    AGENCY,
    /** Administrateur de la plateforme. */
    ADMIN
}