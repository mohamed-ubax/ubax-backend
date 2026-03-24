package com.africa.ubaxplatform.document.codeList;

public enum RefType {
    /** Document lié à un paiement (facture, reçu). */
    PAYMENT,
    /** Document lié à un contrat (bail, acte de vente). */
    CONTRACT,
    /** Document lié à un ticket SAV (rapport d'intervention). */
    TICKET,
    /** Document lié à un bien immobilier (fiche descriptive). */
    PROPERTY,
    /** Document lié à une offre (pré-contrat, accord). */
    OFFER
}