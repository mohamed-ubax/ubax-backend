package com.africa.ubaxplatform.auth.mapper;

import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.auth.dto.UserResponse;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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

  private static String minioPublicEndpoint = "";

  @Value("${minio.public-endpoint}")
  public void setMinioPublicEndpoint(String value) {
    UserMapper.minioPublicEndpoint = value;
  }

  private UserMapper() {}

  /**
   * Normalise l'avatarUrl stockée en base : gère l'ancien format relatif (/bucket/file ou
   * bucket/file) et le nouveau format URL complète (http://...).
   */
  public static String resolveAvatarUrl(String avatarUrl) {
    if (avatarUrl == null) return null;
    if (avatarUrl.startsWith("http")) return avatarUrl;
    String path = avatarUrl.startsWith("/") ? avatarUrl.substring(1) : avatarUrl;
    return minioPublicEndpoint + "/" + path;
  }

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
    return toResponse(user, List.of());
  }

  public static UserResponse toResponse(User user, List<String> subRoles) {
    Agency agency = user.getAgency();
    var hotel = user.getHotel();
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
        resolveAvatarUrl(user.getAvatarUrl()),
        user.getRoles(),
        agency != null ? agency.getId() : null,
        agency != null ? agency.getName() : null,
        hotel != null ? hotel.getId() : null,
        hotel != null ? hotel.getName() : null,
        agency != null ? "AGENCE" : (hotel != null ? "HOTEL" : null),
        subRoles,
        user.isEmailVerified(),
        user.isPhoneVerified(),
        user.isIdentityVerified(),
        user.isActive(),
        user.getLastLoginAt(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  /** Convertit un {@link User} en {@link ClientUserResponse} (vue légère pour les listings). */
  public static ClientUserResponse toClientResponse(User user) {
    return new ClientUserResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhone(),
        user.getCity(),
        resolveAvatarUrl(user.getAvatarUrl()),
        user.isEmailVerified(),
        user.isPhoneVerified(),
        user.isIdentityVerified(),
        user.isActive(),
        user.getLastLoginAt(),
        user.getCreatedAt());
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
