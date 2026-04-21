package com.africa.ubaxplatform.auth.mapper;

import com.africa.ubaxplatform.auth.dto.AgencyTeamMemberResponse;
import com.africa.ubaxplatform.auth.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper entre l'entité {@link User} et le DTO {@link AgencyTeamMemberResponse}.
 *
 * <p>Usage :
 *
 * <pre>{@code
 * AgencyTeamMemberResponse dto = AgencyTeamMapper.toResponse(user);
 * }</pre>
 */
@Component
public class AgencyTeamMapper {

  private AgencyTeamMapper() {}

  /**
   * Convertit un {@link User} membre d'une agence en {@link AgencyTeamMemberResponse}.
   *
   * @param user entité utilisateur (non null, doit être rattaché à une agence)
   * @return DTO de réponse avec identité et rôle partenaire
   */
  public static AgencyTeamMemberResponse toResponse(User user) {
    return new AgencyTeamMemberResponse(
        user.getId(),
        user.getKeycloakId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getPhone(),
        user.getAvatarUrl(),
        user.getPartnerRole(),
        user.isActive(),
        user.getLastLoginAt(),
        user.getCreatedAt());
  }
}
