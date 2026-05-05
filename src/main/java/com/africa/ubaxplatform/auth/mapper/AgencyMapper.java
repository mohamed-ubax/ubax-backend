package com.africa.ubaxplatform.auth.mapper;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AgencyMapper {

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

  public User toPartnerUser(
      PartnerApplication app, String keycloakId, Set<UserRole> roles, Agency agency) {
    User.UserBuilder<?, ?> builder =
        User.builder()
            .keycloakId(keycloakId)
            .firstName(app.getLegalRepFirstName())
            .lastName(app.getLegalRepLastName())
            .email(app.getEmail())
            .phone(app.getPhone())
            .roles(new HashSet<>(roles))
            .emailVerified(true)
            .country(app.getCountry())
            .city(app.getCity());
    if (agency != null) {
      builder.agency(agency);
    }
    return builder.build();
  }
}
