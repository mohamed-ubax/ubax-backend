package com.africa.ubaxplatform.property.codeList;

public enum DocumentType {
    /** Titre foncier — preuve légale de propriété du bien. */
    TITLE_DEED,
    /** Permis de construire délivré par les autorités locales. */
    BUILDING_PERMIT,
    /** Diagnostics techniques obligatoires (DPE, amiante, plomb, électricité...). */
    DIAGNOSTIC,
    /** Plan cadastral de la parcelle. */
    CADASTRAL_PLAN,
    /** Attestation d'assurance habitation ou multirisque. */
    INSURANCE,
    /** Certificat de conformité de la construction. */
    CONFORMITY_CERTIFICATE,
    /** Tout autre document complémentaire jugé utile par le propriétaire. */
    OTHER
}