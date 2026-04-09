package com.africa.ubaxplatform.auth.mapper;

import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.auth.dto.UserResponse;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper statique entre l'entité {@link User} et ses DTOs de sortie.
 *
 * <p>Centralise toutes les conversions {@code User → DTO} pour éviter la duplication dans les
 * services et controllers.
 *
 * <p>Usage :
 *
 * <pre>{@code
 * UserResponse response = UserMapper.toResponse(user);
 * RegisterResponse reg  = UserMapper.toRegisterResponse(user);
 * }</pre>
 */
@Component
public class UserMapper {

  private UserMapper() {}

  /**
   * Convertit un {@link User} en {@link UserResponse} complet.
   *
   * <p>Si l'utilisateur est rattaché à une agence, {@code agencyId} et {@code agencyName} sont
   * renseignés. Sinon ils sont {@code null}.
   *
   * @param user entité à convertir (non null)
   * @return DTO de réponse
   */
  public static UserResponse toResponse(User user) {
    Agency agency = user.getAgency();
    return new UserResponse(
        user.getId(),
        user.getKeycloakId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhone(),
        user.getDateOfBirth(),
        user.getAddress(),
        user.getCity(),
        user.getCountry(),
        user.getLanguage(),
        user.getAvatarUrl(),
        user.getRoles(),
        agency != null ? agency.getId() : null,
        agency != null ? agency.getName() : null,
        user.isEmailVerified(),
        user.isPhoneVerified(),
        user.isIdentityVerified(),
        user.isActive(),
        user.getLastLoginAt(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  /**
   * Convertit un {@link User} en {@link RegisterResponse} (vue post-inscription, sans données
   * sensibles).
   *
   * @param user entité à convertir (non null)
   * @return DTO d'inscription
   */
  public static RegisterResponse toRegisterResponse(User user) {
    return RegisterResponse.builder()
        .userId(user.getId())
        .keycloakId(user.getKeycloakId())
        .email(user.getEmail())
        .phone(user.getPhone())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .roles(user.getRoles())
        .build();
  }
}
