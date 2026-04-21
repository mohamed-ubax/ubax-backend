package com.africa.ubaxplatform.auth.repository;

import com.africa.ubaxplatform.auth.entity.Hotel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository JPA pour les hôtels partenaires. */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {

  /**
   * Recherche un hôtel par son adresse email professionnelle.
   *
   * @param email adresse email de l'hôtel
   * @return l'hôtel correspondant, ou vide si absent
   */
  Optional<Hotel> findByEmail(String email);

  /**
   * Vérifie si un hôtel existe déjà avec cet email.
   *
   * @param email adresse email à vérifier
   * @return {@code true} si un hôtel avec cet email existe déjà
   */
  boolean existsByEmail(String email);
}
