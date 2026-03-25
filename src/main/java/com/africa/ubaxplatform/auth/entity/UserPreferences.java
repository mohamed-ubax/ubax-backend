package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.auth.codeList.AlertFrequency;
import com.africa.ubaxplatform.auth.codeList.DisplayMode;
import com.africa.ubaxplatform.auth.codeList.NewsletterFrequency;
import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Préférences de recherche et de communication d'un utilisateur UBAX.
 *
 * <p>Ce profil de préférences est utilisé pour :
 *
 * <ul>
 *   <li><b>Alertes automatiques</b> : notifier l'utilisateur dès qu'un bien correspond à ses
 *       critères (type, budget, ville, surface...).
 *   <li><b>Recommandations personnalisées</b> : alimenter l'algorithme de suggestion de biens dans
 *       le feed et le dashboard.
 *   <li><b>Personnalisation de l'interface</b> : langue, devise, fréquence des newsletters.
 * </ul>
 *
 * <p><b>Relation :</b> 1-1 avec {@link User}. Une seule ligne par utilisateur, créée
 * automatiquement à l'inscription avec des valeurs par défaut.
 *
 * <p><b>Tableaux PostgreSQL :</b> les champs {@code preferredCities} et {@code amenities} utilisent
 * le type natif PostgreSQL {@code TEXT[]} via {@code hypersistence-utils} pour éviter une table de
 * jointure inutile.
 */
@Entity
@Table(
    name = "user_preferences",
    schema = "administrative",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_user_preferences_user_id", columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserPreferences extends BaseEntity {

  /**
   * Utilisateur propriétaire de ces préférences. Relation 1-1 : un seul enregistrement par
   * utilisateur.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_preferences_user"))
  private User user;

  /**
   * Type de bien immobilier recherché par défaut. Valeurs : {@code APARTMENT | VILLA | HOUSE | LAND
   * | OFFICE | WAREHOUSE | STORE}
   */
  @Column(name = "property_type", length = 50)
  private String propertyType;

  /**
   * Nature de la transaction souhaitée par défaut. Valeurs : {@code SALE | RENT | RENT_FURNISHED}
   */
  @Column(name = "transaction_type", length = 20)
  private String transactionType;

  /** Budget minimum souhaité en XOF. */
  @Column(name = "budget_min", precision = 15, scale = 2)
  private BigDecimal budgetMin;

  /** Budget maximum souhaité en XOF. */
  @Column(name = "budget_max", precision = 15, scale = 2)
  private BigDecimal budgetMax;

  /** Surface habitable minimale souhaitée (m²). */
  @Column(name = "surface_min", precision = 10, scale = 2)
  private BigDecimal surfaceMin;

  /** Surface habitable maximale souhaitée (m²). */
  @Column(name = "surface_max", precision = 10, scale = 2)
  private BigDecimal surfaceMax;

  /** Nombre minimum de pièces souhaité. */
  @Column(name = "rooms_min")
  private Integer roomsMin;

  /** Nombre minimum de chambres souhaité. */
  @Column(name = "bedrooms_min")
  private Integer bedroomsMin;

  /**
   * Liste des villes dans lesquelles l'utilisateur recherche un bien. Type PostgreSQL natif {@code
   * TEXT[]} via hypersistence-utils. Requête : {@code WHERE 'Dakar' = ANY(preferred_cities)}
   */
  @Column(name = "preferred_cities", columnDefinition = "TEXT[]")
  private List<String> preferredCities;

  /**
   * Équipements indispensables pour l'utilisateur. Type PostgreSQL natif {@code TEXT[]} via
   * hypersistence-utils. Valeurs possibles : {@code piscine | parking | gardiennage | climatisation
   * | groupe_electrogene | reservoir | ascenseur | jardin}
   */
  @Column(name = "amenities", columnDefinition = "TEXT[]")
  private List<String> amenities;

  /**
   * État du bien accepté par l'utilisateur. Valeurs : {@code NEW | GOOD | RENOVATE} {@code null} =
   * pas de préférence (tous états acceptés).
   */
  @Column(name = "preferred_condition", length = 20)
  private String preferredCondition;

  /**
   * Activer les alertes automatiques pour les nouvelles annonces correspondant aux critères de
   * recherche de l'utilisateur.
   */
  @Column(name = "alerts_enabled", nullable = false)
  @Builder.Default
  private boolean alertsEnabled = true;

  /**
   * Fréquence d'envoi des alertes de nouveaux biens. Valeurs : {@code REALTIME | DAILY | WEEKLY}
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "alert_frequency", length = 20)
  @Builder.Default
  private AlertFrequency alertFrequency = AlertFrequency.DAILY;

  /** Recevoir les alertes et notifications par email. */
  @Column(name = "notification_email", nullable = false)
  @Builder.Default
  private boolean notificationEmail = true;

  /**
   * Recevoir les alertes et rappels par SMS. Nécessite que {@code phone} soit renseigné et vérifié
   * dans {@link User}.
   */
  @Column(name = "notification_sms", nullable = false)
  @Builder.Default
  private boolean notificationSms = true;

  /**
   * Recevoir les notifications push sur l'application mobile. Nécessite que le token FCM soit
   * enregistré côté backend.
   */
  @Column(name = "notification_push", nullable = false)
  @Builder.Default
  private boolean notificationPush = true;

  /** Abonné à la newsletter de la plateforme (conseils, tendances marché, etc.). */
  @Column(name = "newsletter_subscribed", nullable = false)
  @Builder.Default
  private boolean newsletterSubscribed = false;

  /** Fréquence de réception de la newsletter. Valeurs : {@code WEEKLY | MONTHLY} */
  @Enumerated(EnumType.STRING)
  @Column(name = "newsletter_frequency", length = 20)
  @Builder.Default
  private NewsletterFrequency newsletterFrequency = NewsletterFrequency.WEEKLY;

  /**
   * Devise préférée pour l'affichage des prix. Défaut : {@code XOF} (Franc CFA). Valeurs supportées
   * : {@code XOF | EUR | USD}
   */
  @Column(name = "currency", length = 5)
  @Builder.Default
  private String currency = "XOF";

  /**
   * Mode d'affichage par défaut des résultats de recherche. Valeurs : {@code LIST | MAP | SPLIT}
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "display_mode", length = 10)
  @Builder.Default
  private DisplayMode displayMode = DisplayMode.SPLIT;

  /**
   * {@code true} si l'utilisateur a défini au moins un critère de recherche. Utilisé pour savoir si
   * les alertes peuvent être déclenchées.
   */
  public boolean hasSearchCriteria() {
    return propertyType != null
        || transactionType != null
        || budgetMax != null
        || (preferredCities != null && !preferredCities.isEmpty());
  }

  /** {@code true} si au moins un canal de notification est activé. */
  public boolean hasAnyNotificationEnabled() {
    return notificationEmail || notificationSms || notificationPush;
  }
}
