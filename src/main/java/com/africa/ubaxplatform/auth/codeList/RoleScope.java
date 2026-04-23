package com.africa.ubaxplatform.auth.codeList;

/**
 * Portée d'un sous-rôle dans la table {@code user_sub_roles}.
 *
 * <p>Règles de cohérence :
 *
 * <ul>
 *   <li>{@code UBAX_INTERNAL} — réservé aux utilisateurs {@code ADMIN} et {@code SUPER_ADMIN}.
 *       Valeurs autorisées : {@link UbaxAdminRole}.
 *   <li>{@code AGENCE} — réservé aux utilisateurs {@code PARTNER} rattachés à une agence. Valeurs
 *       autorisées : {@link AgenceRole}.
 *   <li>{@code HOTEL} — réservé aux utilisateurs {@code PARTNER} rattachés à un hôtel. Valeurs
 *       autorisées : {@link HotelRole}.
 * </ul>
 */
public enum RoleScope {

  /** Sous-rôles du back-office interne UBAX (admins). */
  UBAX_INTERNAL("ubax_internal"),

  /** Sous-rôles des membres d'une agence immobilière. */
  AGENCE("agence"),

  /** Sous-rôles des membres d'un hôtel. */
  HOTEL("hotel");

  private final String value;

  RoleScope(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static RoleScope fromValue(String value) {
    for (RoleScope scope : values()) {
      if (scope.value.equalsIgnoreCase(value)) return scope;
    }
    throw new IllegalArgumentException("Scope inconnu : " + value);
  }
}
