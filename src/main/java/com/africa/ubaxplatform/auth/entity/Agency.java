package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.auth.codeList.SubscriptionPlan;
import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Agence immobilière regroupant plusieurs agents sur la plateforme UBAX.
 *
 * <p>Une agence peut gérer plusieurs biens via ses agents rattachés ({@link User} avec rôle {@code
 * AGENT} ou {@code AGENCY}). Elle dispose d'un branding personnalisé (logo, couleurs) et d'un
 * abonnement premium donnant accès aux outils avancés.
 *
 * <p><b>Logo :</b> uploadé dans le bucket MinIO {@code agencies-logos}. Seule l'URL est persistée
 * dans {@code logoUrl}.
 *
 * <p><b>Soft delete :</b> une agence n'est jamais supprimée physiquement. {@code deletedAt} non
 * null = agence désactivée.
 */
@Entity
@Table(
    name = "agencies",
    schema = "administrative",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_agencies_email", columnNames = "email"),
      @UniqueConstraint(name = "uq_agencies_phone", columnNames = "phone")
    },
    indexes = {
      @Index(name = "idx_agencies_verified", columnList = "is_verified"),
      @Index(name = "idx_agencies_active", columnList = "is_active"),
      @Index(name = "idx_agencies_deleted_at", columnList = "deleted_at"),
      @Index(name = "idx_agencies_city", columnList = "city")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Agency extends BaseEntity {

  /**
   * Raison sociale ou nom commercial de l'agence. Affiché sur les annonces publiées par ses agents.
   */
  @Column(name = "name", nullable = false, length = 150)
  private String name;

  /**
   * Description de l'agence, de ses services et de son positionnement. Affichée sur la page
   * publique de l'agence.
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * Numéro RCCM ou d'immatriculation légale de l'agence. Utilisé pour la vérification
   * administrative avant validation.
   */
  @Column(name = "registration_number", length = 100)
  private String registrationNumber;

  /**
   * URL du logo de l'agence stocké dans le bucket MinIO {@code agencies-logos}.
   *
   * <p><b>Cycle d'upload :</b>
   *
   * <ol>
   *   <li>Admin de l'agence demande une URL présignée PUT : {@code GET
   *       /api/storage/presign?bucket=agencies-logos&key={agencyId}.webp}
   *   <li>Upload direct vers MinIO via l'URL présignée.
   *   <li>Notification backend : {@code PATCH /api/agencies/{id}/logo}
   *   <li>Backend met à jour ce champ.
   * </ol>
   *
   * Format : {@code http://minio:9000/agencies-logos/{agencyId}.webp}
   */
  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  /** Adresse du siège social de l'agence. Affichée sur la fiche agence et les documents légaux. */
  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  /**
   * Ville d'implantation principale de l'agence. Utilisée pour le filtrage géographique des
   * agences.
   */
  @Column(name = "city", length = 100)
  private String city;

  /** Code pays ISO 3166-1 alpha-2. Défaut : {@code SN} pour Sénégal. */
  @Column(name = "country", length = 5)
  @Builder.Default
  private String country = "SN";

  /** Numéro de téléphone professionnel de l'agence au format international. */
  @Column(name = "phone", length = 20)
  private String phone;

  /**
   * Adresse email professionnelle de l'agence. Utilisée pour les notifications et les documents
   * contractuels.
   */
  @Column(name = "email", length = 150)
  private String email;

  /** Site web officiel de l'agence. */
  @Column(name = "website", length = 300)
  private String website;

  /**
   * Offre d'abonnement souscrite par l'agence sur la plateforme. Détermine les fonctionnalités
   * accessibles (nombre d'annonces, outils avancés...). Valeurs : {@code BASIC | PRO | PREMIUM}
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "subscription_plan", length = 20)
  @Builder.Default
  private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BASIC;

  /**
   * Date d'expiration de l'abonnement en cours. Si {@code null} ou passée, l'agence est rétrogradée
   * au plan BASIC.
   */
  @Column(name = "subscription_expires_at")
  private LocalDateTime subscriptionExpiresAt;

  /**
   * {@code true} si l'agence a été vérifiée et validée par un administrateur. Seules les agences
   * vérifiées peuvent publier des annonces avec badge "Agence certifiée" et accéder aux outils
   * premium.
   */
  @Column(name = "is_verified", nullable = false)
  @Builder.Default
  private boolean verified = false;

  /** Date de vérification de l'agence par l'administrateur. */
  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  /**
   * Compte agence actif sur la plateforme. {@code false} = compte suspendu par un administrateur.
   */
  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  /**
   * Date et heure de suppression logique de l'agence (soft delete). {@code null} = agence
   * existante. Non null = agence supprimée.
   */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** {@code true} si l'abonnement de l'agence est encore valide. */
  public boolean isSubscriptionActive() {
    return subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(LocalDateTime.now());
  }

  /** {@code true} si le compte a été supprimé logiquement. */
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
