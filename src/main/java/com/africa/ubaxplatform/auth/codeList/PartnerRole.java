package com.africa.ubaxplatform.auth.codeList;

/**
 * @deprecated Remplacé par {@link AgenceRole} (scope agence) et {@link HotelRole} (scope hotel).
 *     Utiliser {@link AgenceRole} ou {@link HotelRole} selon le contexte partenaire.
 */
@Deprecated(since = "2.0", forRemoval = true)
public enum PartnerRole {
  DIRECTEUR_AGENCE,
  COMMERCIAL,
  COMPTABLE_AGENCE,
  AGENT_SAV,
  GERANT_HOTEL,
  RECEPTIONNISTE,
  COMPTABLE_HOTEL,
  RESPONSABLE_HEBERGEMENT
}
