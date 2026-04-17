package com.africa.ubaxplatform.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriétés de bootstrap pour la création automatique du premier Super Administrateur.
 *
 * <p>À activer uniquement au premier démarrage sur un environnement vierge (aucun admin en base).
 * Une fois le compte créé, passer {@code ubax.bootstrap.enabled} à {@code false} (ou via la
 * variable d'environnement {@code BOOTSTRAP_ENABLED=false}) pour désactiver l'initialisation.
 *
 * <p>Le bootstrapper est idempotent : si un {@code SUPER_ADMIN} existe déjà en base, il ne fait
 * rien — même si {@code enabled = true}.
 *
 * <pre>
 * # application-local.yml (premier démarrage uniquement)
 * ubax:
 *   bootstrap:
 *     enabled: true
 *     email: admin@ubax.io
 *     first-name: Super
 *     last-name: Admin
 *     phone: +22600000000
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ubax.bootstrap")
@Getter
@Setter
public class SuperAdminBootstrapProperties {

  /** Active ou désactive le bootstrapper au démarrage. Par défaut {@code false}. */
  private boolean enabled = false;

  /** Adresse email du compte Super Administrateur initial. */
  private String email;

  /** Prénom du Super Administrateur initial. */
  private String firstName;

  /** Nom de famille du Super Administrateur initial. */
  private String lastName;

  /** Numéro de téléphone au format international (ex : {@code +22600000000}). */
  private String phone;
}
