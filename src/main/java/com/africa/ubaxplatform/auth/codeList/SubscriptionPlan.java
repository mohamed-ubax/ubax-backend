package com.africa.ubaxplatform.auth.codeList;

/**
 * Offre d'abonnement souscrite par une agence immobilière sur la plateforme UBAX.
 * Détermine les fonctionnalités accessibles (nombre d'annonces, outils avancés...).
 */
public enum SubscriptionPlan {
    /** Accès limité : nombre d'annonces restreint, pas d'outils avancés. */
    BASIC,
    /** Annonces illimitées + statistiques avancées + CRM intégré. */
    PRO,
    /** Multi-utilisateurs + branding personnalisé + rapports analytiques avancés. */
    PREMIUM
}