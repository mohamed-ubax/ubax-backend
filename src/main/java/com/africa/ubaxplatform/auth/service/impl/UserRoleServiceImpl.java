package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.AgenceRole;
import com.africa.ubaxplatform.auth.codeList.HotelRole;
import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UbaxAdminRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.UserSubRoleResponse;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.entity.UserSubRole;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.repository.UserSubRoleRepository;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleServiceImpl implements UserRoleService {

  private static final Set<String> UBAX_INTERNAL_ROLES =
      Arrays.stream(UbaxAdminRole.values()).map(Enum::name).collect(Collectors.toSet());

  private static final Set<String> AGENCE_ROLES =
      Arrays.stream(AgenceRole.values()).map(Enum::name).collect(Collectors.toSet());

  private static final Set<String> HOTEL_ROLES =
      Arrays.stream(HotelRole.values()).map(Enum::name).collect(Collectors.toSet());

  private final UserSubRoleRepository subRoleRepo;
  private final UserRepository userRepo;

  @Override
  @Transactional
  public List<UserSubRoleResponse> assignSubRoles(UUID userId, List<String> roles, RoleScope scope)
      throws CustomException {

    User user =
        userRepo
            .findById(userId)
            .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND));

    validateScopeCompatibility(user, scope);
    validateRolesForScope(roles, scope);

    List<UserSubRole> assigned = new ArrayList<>();
    for (String role : roles) {
      if (!subRoleRepo.existsByUserIdAndRoleAndScope(userId, role, scope)) {
        UserSubRole sub = UserSubRole.builder().user(user).role(role).scope(scope).build();
        assigned.add(subRoleRepo.save(sub));
        log.info("Sous-rôle assigné : userId={}, role={}, scope={}", userId, role, scope);
      }
    }

    return assigned.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserSubRoleResponse> getSubRoles(UUID userId, RoleScope scope) {
    List<UserSubRole> results =
        scope != null
            ? subRoleRepo.findByUserIdAndScope(userId, scope)
            : subRoleRepo.findByUserId(userId);
    return results.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public void revokeSubRole(UUID userId, String role, RoleScope scope) throws CustomException {
    if (!subRoleRepo.existsByUserIdAndRoleAndScope(userId, role, scope)) {
      throw new NotFoundException(
          "Sous-rôle introuvable : userId=" + userId + ", role=" + role + ", scope=" + scope);
    }
    subRoleRepo.deleteByUserIdAndRoleAndScope(userId, role, scope);
    log.info("Sous-rôle révoqué : userId={}, role={}, scope={}", userId, role, scope);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasSubRole(UUID userId, String role, RoleScope scope) {
    return subRoleRepo.existsByUserIdAndRoleAndScope(userId, role, scope);
  }

  // ── Validation ────────────────────────────────────────────────────

  private void validateScopeCompatibility(User user, RoleScope scope) {
    boolean isAdmin =
        user.getRoles().contains(UserRole.ADMIN) || user.getRoles().contains(UserRole.SUPER_ADMIN);
    boolean isPartner = user.getRoles().contains(UserRole.PARTNER);

    switch (scope) {
      case UBAX_INTERNAL -> {
        if (!isAdmin) {
          throw new BadRequestException(
              "Le scope UBAX_INTERNAL est réservé aux utilisateurs ADMIN et SUPER_ADMIN");
        }
      }
      case AGENCE -> {
        if (!isPartner) {
          throw new BadRequestException("Le scope AGENCE est réservé aux utilisateurs PARTNER");
        }
      }
      case HOTEL -> {
        if (!isPartner) {
          throw new BadRequestException("Le scope HOTEL est réservé aux utilisateurs PARTNER");
        }
      }
    }
  }

  private void validateRolesForScope(List<String> roles, RoleScope scope) {
    Set<String> allowed =
        switch (scope) {
          case UBAX_INTERNAL -> UBAX_INTERNAL_ROLES;
          case AGENCE -> AGENCE_ROLES;
          case HOTEL -> HOTEL_ROLES;
        };

    List<String> invalid = roles.stream().filter(r -> !allowed.contains(r)).toList();
    if (!invalid.isEmpty()) {
      throw new BadRequestException(
          "Rôles invalides pour le scope "
              + scope
              + " : "
              + invalid
              + ". Valeurs autorisées : "
              + allowed);
    }
  }

  private UserSubRoleResponse toResponse(UserSubRole sub) {
    return new UserSubRoleResponse(
        sub.getId(), sub.getUser().getId(), sub.getRole(), sub.getScope(), sub.getCreatedAt());
  }
}
