package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.common.constants.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Hôtel partenaire de la plateforme UBAX.
 *
 * <p>Symétrique avec {@link Agency} pour les structures hôtelières. Un hôtel peut gérer ses
 * espaces, ses réservations et son équipe interne via les membres rattachés ({@link User} avec rôle
 * {@code PARTNER} et {@code partnerRole} hôtelier).
 *
 * <p><b>Logo :</b> uploadé dans le bucket MinIO {@code agencies-logos}. Seule l'URL est persistée
 * dans {@code logoUrl}.
 *
 * <p><b>Soft delete :</b> un hôtel n'est jamais supprimé physiquement. {@code deletedAt} non null =
 * hôtel désactivé.
 */
@Entity
@Table(name = "hotels", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Hotel extends BaseEntity {

  /** Raison sociale ou nom commercial de l'hôtel. Affiché sur les annonces et la fiche publique. */
  @Column(name = "name", nullable = false, length = 150)
  private String name;

  /** Description de l'hôtel, de ses services et de son positionnement. */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Numéro RCCM ou d'immatriculation légale de l'hôtel. */
  @Column(name = "registration_number", length = 100)
  private String registrationNumber;

  /**
   * URL du logo stocké dans le bucket MinIO {@code agencies-logos}.
   *
   * <p>Format : {@code http://minio:9000/agencies-logos/{hotelId}.webp}
   */
  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  /** Adresse du siège social de l'hôtel. */
  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  /** Ville d'implantation principale. */
  @Column(name = "city", length = 100)
  private String city;

  /** Code pays ISO 3166-1 alpha-2. Défaut : {@code SN} pour Sénégal. */
  @Column(name = "country", nullable = false, length = 5)
  @Builder.Default
  private String country = "SN";

  /** Numéro de téléphone professionnel au format international. */
  @Column(name = "phone", length = 20)
  private String phone;

  /** Adresse email professionnelle de l'hôtel. */
  @Column(name = "email", length = 150)
  private String email;

  /** Site web officiel de l'hôtel. */
  @Column(name = "website", length = 300)
  private String website;

  /** Offre d'abonnement souscrite sur la plateforme. Valeurs : {@code BASIC | PRO | PREMIUM}. */
  @Column(name = "subscription_plan", nullable = false, length = 20)
  @Builder.Default
  private String subscriptionPlan = Constants.CodeList.SubscriptionPlan.BASIC;

  /** Date d'expiration de l'abonnement en cours. {@code null} ou passée = plan BASIC. */
  @Column(name = "subscription_expires_at")
  private LocalDateTime subscriptionExpiresAt;

  /** {@code true} si l'hôtel a été vérifié et validé par un administrateur UBAX. */
  @Column(name = "is_verified", nullable = false)
  @Builder.Default
  private boolean verified = false;

  /** Date de vérification par l'administrateur. */
  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  /** {@code false} = compte suspendu par un administrateur. */
  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  /** Date et heure de suppression logique (soft delete). {@code null} = hôtel actif. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** {@code true} si l'abonnement est encore valide. */
  public boolean isSubscriptionActive() {
    return subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(LocalDateTime.now());
  }

  /** {@code true} si le compte a été supprimé logiquement. */
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
