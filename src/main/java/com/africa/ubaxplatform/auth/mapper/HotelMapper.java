package com.africa.ubaxplatform.auth.mapper;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

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
        .description(app.getDescription())
        .build();
  }

  public User toPartnerUser(
      PartnerApplication app, String keycloakId, Set<UserRole> roles, Hotel hotel) {
    User.UserBuilder<?, ?> builder =
        User.builder()
            .keycloakId(keycloakId)
            .firstName(app.getCompanyName())
            .lastName(app.getLegalRepresentative())
            .email(app.getEmail())
            .phone(app.getPhone())
            .roles(new HashSet<>(roles))
            .emailVerified(true)
            .country(app.getCountry())
            .city(app.getCity());
    if (hotel != null) {
      builder.hotel(hotel);
    }
    return builder.build();
  }
}
