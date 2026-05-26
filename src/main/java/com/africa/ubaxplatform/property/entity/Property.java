package com.africa.ubaxplatform.property.entity;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Bien immobilier publié sur la plateforme UBAX.
 *
 * <p><b>Workflow de publication :</b> {@code DRAFT → PENDING → PUBLISHED → RESERVED → SOLD /
 * ARCHIVED} Un modérateur valide le passage de {@code PENDING} à {@code PUBLISHED}. En cas de
 * rejet, le statut passe à {@code REJECTED} avec un motif.
 *
 * <p><b>Médias :</b> photos, vidéos et plans sont gérés dans {@link PropertyMedia}, reliés par FK.
 * Les fichiers sont dans MinIO bucket {@code properties-media}.
 *
 * <p><b>Documents légaux :</b> titres fonciers, permis, diagnostics sont gérés dans {@link
 * PropertyDocument}, bucket MinIO {@code property-documents}.
 *
 * <p><b>Géolocalisation :</b> {@code latitude} et {@code longitude} alimentent l'affichage
 * cartographique (Google Maps / Mapbox) et les recherches géospatiales via l'index PostgreSQL GIST.
 */
@Entity
@Table(name = "properties", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Property extends BaseEntity {

  /**
   * Propriétaire ou agent ayant publié le bien. Peut avoir le rôle {@code OWNER}, {@code AGENT} ou
   * {@code AGENCY}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "owner_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_property_owner"))
  private User owner;

  /**
   * Agence immobilière gestionnaire du bien. {@code null} si le bien est publié par un particulier
   * (rôle {@code OWNER}).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agency_id", foreignKey = @ForeignKey(name = "fk_property_agency"))
  private Agency agency;

  /**
   * Hôtel propriétaire du bien (chambre/suite). {@code null} pour les biens immobiliers classiques.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hotel_id", foreignKey = @ForeignKey(name = "fk_property_hotel"))
  private Hotel hotel;

  /** Titre accrocheur de l'annonce affiché dans les résultats de recherche. */
  @Column(name = "title", nullable = false, length = 255)
  private String title;

  /** Description complète du bien. */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * Catégorie du bien immobilier. Valeurs : {@code APARTMENT | VILLA | HOUSE | LAND | OFFICE |
   * WAREHOUSE | STORE}
   */
  @Column(name = "property_type", nullable = false, length = 50)
  private String propertyType;

  /** Nature de la transaction proposée. Valeurs : {@code SALE | RENT | RENT_FURNISHED} */
  @Column(name = "transaction_type", nullable = false, length = 30)
  private String transactionType;

  /** Prix de vente ou loyer mensuel hors charges en XOF. */
  @Column(name = "price", nullable = false, precision = 15, scale = 2)
  private BigDecimal price;

  /** État général du bien. Valeurs : {@code NEW | GOOD | RENOVATE} */
  @Column(name = "condition", length = 20)
  private String condition;

  /** Année de construction du bien. */
  @Column(name = "year_built")
  private Integer yearBuilt;

  /** Surface totale du terrain ou de la parcelle (m²). */
  @Column(name = "surface_total", precision = 10, scale = 2)
  private BigDecimal surfaceTotal;

  /** Surface habitable nette (m²), hors garage et caves. */
  @Column(name = "surface_living", precision = 10, scale = 2)
  private BigDecimal surfaceLiving;

  /** Nombre total de pièces principales (salon + chambres). */
  @Column(name = "rooms")
  private Integer rooms;

  /** Nombre de chambres à coucher. */
  @Column(name = "bedrooms")
  private Integer bedrooms;

  /** Nombre de salles de bain et de douches. */
  @Column(name = "bathrooms")
  private Integer bathrooms;

  /** Nombre de balcons ou terrasses privatifs. */
  @Column(name = "balconies")
  @Builder.Default
  private Integer balconies = 0;

  /**
   * Étage du bien dans l'immeuble. {@code null} pour les villas, maisons individuelles et terrains.
   * {@code 0} = rez-de-chaussée.
   */
  @Column(name = "floor")
  private Integer floor;

  /** Nombre total d'étages de l'immeuble. */
  @Column(name = "total_floors")
  private Integer totalFloors;

  /** Adresse complète du bien (rue, numéro, résidence). */
  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  /** Ville où se situe le bien. Obligatoire pour la recherche géographique. */
  @Column(name = "city", nullable = false, length = 100)
  private String city;

  /** Quartier ou commune pour un filtrage plus fin. */
  @Column(name = "district", length = 100)
  private String district;

  /** Nom de la rue ou de l'avenue. */
  @Column(name = "street", length = 200)
  private String street;

  /** Latitude GPS pour l'affichage cartographique (Google Maps / Mapbox). */
  @Column(name = "latitude", precision = 10, scale = 8)
  private BigDecimal latitude;

  /** Longitude GPS pour l'affichage cartographique. */
  @Column(name = "longitude", precision = 11, scale = 8)
  private BigDecimal longitude;

  /** Commodités du bien : standard (référence PROPERTY_AMENITY) ou personnalisées. */
  @Builder.Default
  @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PropertyAmenity> amenities = new ArrayList<>();

  /**
   * {@code true} si l'annonce bénéficie d'une mise en avant payante (boost). Les annonces boostées
   * apparaissent en tête des résultats de recherche.
   */
  @Column(name = "is_boosted", nullable = false)
  @Builder.Default
  private boolean boosted = false;

  /**
   * Date d'expiration du boost payant. Après cette date, {@code boosted} repasse à {@code false}
   * via un job planifié.
   */
  @Column(name = "boost_expires_at")
  private LocalDateTime boostExpiresAt;

  /**
   * Statut de l'annonce dans son cycle de vie. Valeurs : {@code DRAFT | PENDING | PUBLISHED |
   * RESERVED | SOLD | ARCHIVED | REJECTED}
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private PropertyStatus status = PropertyStatus.DRAFT;

  /** Motif de refus renseigné par le modérateur en cas de rejet. */
  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  /**
   * Date et heure de publication effective. Valorisée lors du passage au statut {@code PUBLISHED}.
   */
  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  /**
   * Date d'expiration automatique de l'annonce publiée. Après cette date, l'annonce passe en {@code
   * ARCHIVED} via un job planifié.
   */
  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  /** {@code true} si l'annonce est visible publiquement. */
  public boolean isPublic() {
    return status == PropertyStatus.PUBLISHED || status == PropertyStatus.RESERVED;
  }

  /** {@code true} si le bien est encore disponible (pas vendu ni archivé). */
  public boolean isAvailable() {
    return status == PropertyStatus.PUBLISHED;
  }

  /** {@code true} si le boost est encore actif. */
  public boolean isBoostedActive() {
    return boosted && boostExpiresAt != null && boostExpiresAt.isAfter(LocalDateTime.now());
  }

  /** {@code true} si le bien est géolocalisé (coordonnées GPS renseignées). */
  public boolean isGeolocated() {
    return latitude != null && longitude != null;
  }

  /**
   * Type de lit principal de la chambre. Valeurs : {@code SINGLE | DOUBLE | TWIN | KING | QUEEN |
   * BUNK}
   */
  @Column(name = "bed_type", length = 20)
  private String bedType;

  /** Capacité maximale d'accueil (nombre de personnes). */
  @Column(name = "max_occupancy")
  private Integer maxOccupancy;

  /**
   * Formule repas incluse. Valeurs : {@code ROOM_ONLY | BREAKFAST | HALF_BOARD | FULL_BOARD |
   * ALL_INCLUSIVE}
   */
  @Column(name = "meal_plan", length = 30)
  private String mealPlan;

  /** Fréquence de facturation du prix. Valeurs : {@code NIGHTLY | WEEKLY | MONTHLY} */
  @Column(name = "payment_frequency", length = 20)
  private String paymentFrequency;

  /**
   * Nombre d'unités disponibles pour ce type de bien hôtelier.
   *
   * <p>Permet de modéliser un pool de chambres identiques (même configuration, même prix) sans
   * créer une entrée distincte par chambre. Exemple : 5 chambres Standard disponibles → {@code
   * unitCount = 5}.
   *
   * <p>La disponibilité est accordée tant que le nombre de réservations CONFIRMED qui se
   * chevauchent est strictement inférieur à ce seuil.
   *
   * <p>Pour les biens immobiliers classiques, cette valeur est toujours {@code 1}.
   */
  @Column(name = "unit_count", nullable = false)
  @Builder.Default
  private Integer unitCount = 1;

  /** {@code true} si le bien est géré par une agence. */
  public boolean isManagedByAgency() {
    return agency != null;
  }
}
