package com.africa.ubaxplatform.auth.mapper;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Mapper entre {@link PartnerApplication} et les entités de structure partenaire ({@link Agency},
 * {@link Hotel}) ainsi que l'entité {@link User} du compte principal.
 *
 * <p>Les deux méthodes {@link #toAgency} et {@link #toHotel} sont symétriques. La méthode {@link
 * #toPartnerUser} gère les deux cas et initialise automatiquement le {@link PartnerRole} initial du
 * dirigeant ({@code DIRECTEUR_AGENCE} ou {@code GERANT_HOTEL}).
 */
@Component
public class AgencyMapper {

  /**
   * Construit l'entité {@link Agency} à partir d'une demande d'adhésion approuvée.
   *
   * @param app demande approuvée de type {@code AGENCE_IMMOBILIERE}
   * @return entité agence prête à persister
   */
  public Agency toAgency(PartnerApplication app) {
    return Agency.builder()
        .name(app.getCompanyName())
        .registrationNumber(app.getRegistrationNumber())
        .logoUrl(app.getLogoUrl())
        .address(app.getPostalAddress())
        .city(app.getCity())
        .country(app.getCountry())
        .phone(app.getPhone())
        .email(app.getEmail())
        .build();
  }

  /**
   * Construit l'entité {@link Hotel} à partir d'une demande d'adhésion approuvée.
   *
   * @param app demande approuvée de type {@code HOTEL}
   * @return entité hôtel prête à persister
   */
  public Hotel toHotel(PartnerApplication app) {
    return Hotel.builder()
        .name(app.getCompanyName())
        .registrationNumber(app.getRegistrationNumber())
        .logoUrl(app.getLogoUrl())
        .address(app.getPostalAddress())
        .city(app.getCity())
        .country(app.getCountry())
        .phone(app.getPhone())
        .email(app.getEmail())
        .build();
  }

  /**
   * Construit l'entité {@link User} du compte principal du partenaire après approbation.
   *
   * <p>Le {@link PartnerRole} initial est assigné automatiquement :
   *
   * <ul>
   *   <li>{@code AGENCE_IMMOBILIERE} → {@link PartnerRole#DIRECTEUR_AGENCE}
   *   <li>{@code HOTEL} → {@link PartnerRole#GERANT_HOTEL}
   * </ul>
   *
   * <p>L'un des paramètres {@code agency} ou {@code hotel} doit être non-null selon le type. Ils
   * sont mutuellement exclusifs.
   *
   * @param app demande d'adhésion approuvée
   * @param keycloakId identifiant Keycloak nouvellement créé
   * @param assignedRole rôle Keycloak ({@code AGENCY} ou {@code PARTNER})
   * @param agency entité agence créée, ou {@code null} si type hôtel
   * @param hotel entité hôtel créée, ou {@code null} si type agence
   * @return entité utilisateur prête à persister
   */
  public User toPartnerUser(
      PartnerApplication app,
      String keycloakId,
      UserRole assignedRole,
      Agency agency,
      Hotel hotel) {

    boolean isAgency =
        Constants.CodeList.PartnerType.AGENCE_IMMOBILIERE.equals(app.getPartnerType());

    PartnerRole initialRole = isAgency ? PartnerRole.DIRECTEUR_AGENCE : PartnerRole.GERANT_HOTEL;

    User.UserBuilder<?, ?> builder =
        User.builder()
            .keycloakId(keycloakId)
            .firstName(app.getCompanyName())
            .lastName(app.getLegalRepresentative())
            .email(app.getEmail())
            .phone(app.getPhone())
            .roles(new HashSet<>(Set.of(assignedRole)))
            .partnerRole(initialRole)
            .emailVerified(true)
            .country(app.getCountry())
            .city(app.getCity());

    if (agency != null) builder.agency(agency);
    if (hotel != null) builder.hotel(hotel);

    return builder.build();
  }
}
