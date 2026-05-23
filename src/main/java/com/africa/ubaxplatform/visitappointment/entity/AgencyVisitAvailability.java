package com.africa.ubaxplatform.visitappointment.entity;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.property.entity.Property;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Configuration des créneaux disponibles pour les visites d'un bien immobilier.
 *
 * <p>Permet à une agence de définir :
 *
 * <ul>
 *   <li>Les jours de la semaine disponibles
 *   <li>Les créneaux horaires par jour (ex: "10:00-14:00", "14:00-18:00")
 *   <li>Les dates fermées (vacances, congés)
 *   <li>Le nombre maximum de visites par créneau
 * </ul>
 */
@Entity
@Table(name = "agency_visit_availabilities", schema = "administrative")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AgencyVisitAvailability extends BaseEntity {

  /** Bien immobilier associé (relation 1:1) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "property_id",
      nullable = false,
      unique = true,
      foreignKey = @ForeignKey(name = "fk_availability_property"))
  private Property property;

  /** Agence propriétaire du bien */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "agency_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_availability_agency"))
  private Agency agency;

  /**
   * Configuration des créneaux par jour de semaine (JSON).
   *
   * <p>Format : {@code {"1": ["10:00-14:00", "14:00-18:00"], "2": ["14:00-22:00"]}}
   *
   * <p>Jours : 0=Dimanche, 1=Lundi, ..., 6=Samedi
   */
  @Column(name = "time_slots_config", columnDefinition = "TEXT", nullable = false)
  private String timeSlotsConfig;

  /** Dates fermées (array PostgreSQL converti en List JSON). Format ISO YYYY-MM-DD. */
  @Column(name = "blackout_dates", columnDefinition = "DATE[]", nullable = false)
  private LocalDate[] blackoutDates;

  /** Limite maximum de visites par créneau */
  @Column(name = "max_visits_per_slot", nullable = false)
  private Integer maxVisitsPerSlot = 3;

  /** Configuration active ou non */
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  // ===== Helpers =====

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Récupère les créneaux du jour de semaine donnée.
   *
   * @param dayOfWeek 0=Dimanche, 1=Lundi, ..., 6=Samedi
   * @return Liste des créneaux (ex: ["10:00-14:00", "14:00-18:00"]) ou null si jour fermé
   */
  public List<String> getTimeSlotsForDay(int dayOfWeek) {
    try {
      Map<String, List<String>> config =
          objectMapper.readValue(timeSlotsConfig, new TypeReference<>() {});
      return config.get(String.valueOf(dayOfWeek));
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Définit les créneaux pour un jour de semaine.
   *
   * @param dayOfWeek 0=Dimanche, 1=Lundi, ..., 6=Samedi
   * @param timeSlots Liste des créneaux (ex: ["10:00-14:00", "14:00-18:00"]) ou null pour fermer le
   *     jour
   */
  public void setTimeSlotsForDay(int dayOfWeek, List<String> timeSlots) {
    try {
      Map<String, List<String>> config =
          objectMapper.readValue(timeSlotsConfig, new TypeReference<>() {});
      if (timeSlots == null || timeSlots.isEmpty()) {
        config.remove(String.valueOf(dayOfWeek));
      } else {
        config.put(String.valueOf(dayOfWeek), timeSlots);
      }
      timeSlotsConfig = objectMapper.writeValueAsString(config);
    } catch (IOException e) {
      // Fallback : initialise une nouvelle config
      Map<String, List<String>> config = new HashMap<>();
      if (timeSlots != null && !timeSlots.isEmpty()) {
        config.put(String.valueOf(dayOfWeek), timeSlots);
      }
      try {
        timeSlotsConfig = objectMapper.writeValueAsString(config);
      } catch (IOException ex) {
        timeSlotsConfig = "{}";
      }
    }
  }

  /**
   * Récupère l'intégralité de la configuration des créneaux.
   *
   * @return Map day_of_week → List[timeSlots]
   */
  public Map<String, List<String>> getAllTimeSlots() {
    try {
      return objectMapper.readValue(timeSlotsConfig, new TypeReference<>() {});
    } catch (IOException e) {
      return new HashMap<>();
    }
  }

  /**
   * Définit l'intégralité de la configuration des créneaux.
   *
   * @param allTimeSlots Map day_of_week → List[timeSlots]
   */
  public void setAllTimeSlots(Map<String, List<String>> allTimeSlots) {
    try {
      timeSlotsConfig = objectMapper.writeValueAsString(allTimeSlots);
    } catch (IOException e) {
      timeSlotsConfig = "{}";
    }
  }

  /**
   * Vérifie si une date est fermée (en blackout).
   *
   * @param date Date à vérifier
   * @return true si date est en blackout
   */
  public boolean isDateBlackedOut(LocalDate date) {
    if (blackoutDates == null) {
      return false;
    }
    for (LocalDate blackout : blackoutDates) {
      if (blackout.equals(date)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Ajoute une date au blackout.
   *
   * @param date Date à ajouter
   */
  public void addBlackoutDate(LocalDate date) {
    if (blackoutDates == null) {
      blackoutDates = new LocalDate[] {date};
    } else {
      LocalDate[] newArray = new LocalDate[blackoutDates.length + 1];
      System.arraycopy(blackoutDates, 0, newArray, 0, blackoutDates.length);
      newArray[blackoutDates.length] = date;
      blackoutDates = newArray;
    }
  }

  /**
   * Supprime une date du blackout.
   *
   * @param date Date à supprimer
   */
  public void removeBlackoutDate(LocalDate date) {
    if (blackoutDates == null || blackoutDates.length == 0) {
      return;
    }
    List<LocalDate> list = new ArrayList<>();
    for (LocalDate d : blackoutDates) {
      if (!d.equals(date)) {
        list.add(d);
      }
    }
    blackoutDates = list.toArray(new LocalDate[0]);
  }
}
