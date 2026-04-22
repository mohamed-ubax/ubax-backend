package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.UserSubRoleResponse;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.entity.UserSubRole;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.repository.UserSubRoleRepository;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import com.africa.ubaxplatform.common.codelist.repository.LaCodeListRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import java.util.ArrayList;
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

  private final UserSubRoleRepository subRoleRepo;
  private final UserRepository userRepo;
  private final LaCodeListRepository codeListRepo;

  @Override
  @Transactional
  public List<UserSubRoleResponse> assignSubRoles(UUID userId, List<String> roles, RoleScope scope)
      throws CustomException {

    User user =
        userRepo
            .findById(userId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND)));

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
      throw new CustomException(
          new NotFoundException(
              "Sous-rôle introuvable : userId=" + userId + ", role=" + role + ", scope=" + scope));
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

  private void validateScopeCompatibility(User user, RoleScope scope) throws CustomException {
    boolean isAdmin =
        user.getRoles().contains(UserRole.ADMIN) || user.getRoles().contains(UserRole.SUPER_ADMIN);
    boolean isPartner = user.getRoles().contains(UserRole.PARTNER);

    switch (scope) {
      case UBAX_INTERNAL -> {
        if (!isAdmin) {
          throw new CustomException(
              (new BadRequestException(
                  "Le scope UBAX_INTERNAL est réservé aux utilisateurs ADMIN et SUPER_ADMIN")));
        }
      }
      case AGENCE -> {
        if (!isPartner) {
          throw new CustomException(
              (new BadRequestException("Le scope AGENCE est réservé aux utilisateurs PARTNER")));
        }
      }
      case HOTEL -> {
        if (!isPartner) {
          throw new CustomException(
              (new BadRequestException("Le scope HOTEL est réservé aux utilisateurs PARTNER")));
        }
      }
    }
  }

  private void validateRolesForScope(List<String> roles, RoleScope scope) throws CustomException {
    String codeListType =
        switch (scope) {
          case UBAX_INTERNAL -> "ROLE_UBAX_INTERNAL";
          case AGENCE -> "ROLE_AGENCE";
          case HOTEL -> "ROLE_HOTEL";
        };

    Set<String> allowed =
        codeListRepo.findAllByType(codeListType).stream()
            .map(LaCodeList::getValue)
            .collect(Collectors.toSet());

    List<String> invalid = roles.stream().filter(r -> !allowed.contains(r)).toList();
    if (!invalid.isEmpty()) {
      throw new CustomException(
          new BadRequestException(
              "Rôles invalides pour le scope "
                  + scope
                  + " : "
                  + invalid
                  + ". Valeurs autorisées : "
                  + allowed));
    }
  }

  // ── Opérations PARTNER (même structure) ──────────────────────────

  @Override
  @Transactional
  public List<UserSubRoleResponse> assignPartnerSubRoles(
      String callerKeycloakId, UUID targetUserId, List<String> roles, RoleScope scope)
      throws CustomException {

    User caller = findByKeycloakId(callerKeycloakId);
    User target = findById(targetUserId);

    validatePartnerScope(scope);
    validateCallerIsPartner(caller);
    validateSameStructure(caller, target, scope);
    validateRolesForScope(roles, scope);

    List<UserSubRole> assigned = new ArrayList<>();
    for (String role : roles) {
      if (!subRoleRepo.existsByUserIdAndRoleAndScope(targetUserId, role, scope)) {
        assigned.add(
            subRoleRepo.save(UserSubRole.builder().user(target).role(role).scope(scope).build()));
        log.info(
            "Sous-rôle partner assigné : callerId={}, targetId={}, role={}, scope={}",
            caller.getId(),
            targetUserId,
            role,
            scope);
      }
    }
    return assigned.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserSubRoleResponse> getPartnerSubRoles(
      String callerKeycloakId, UUID targetUserId, RoleScope scope) throws CustomException {

    User caller = findByKeycloakId(callerKeycloakId);
    User target = findById(targetUserId);

    validatePartnerScope(scope);
    validateCallerIsPartner(caller);
    validateSameStructure(caller, target, scope);

    return subRoleRepo.findByUserIdAndScope(targetUserId, scope).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void revokePartnerSubRole(
      String callerKeycloakId, UUID targetUserId, String role, RoleScope scope)
      throws CustomException {

    User caller = findByKeycloakId(callerKeycloakId);
    User target = findById(targetUserId);

    validatePartnerScope(scope);
    validateCallerIsPartner(caller);
    validateSameStructure(caller, target, scope);

    if (!subRoleRepo.existsByUserIdAndRoleAndScope(targetUserId, role, scope)) {
      throw new CustomException(
          new NotFoundException("Sous-rôle introuvable : " + role + " [" + scope + "]"));
    }
    subRoleRepo.deleteByUserIdAndRoleAndScope(targetUserId, role, scope);
    log.info(
        "Sous-rôle partner révoqué : callerId={}, targetId={}, role={}, scope={}",
        caller.getId(),
        targetUserId,
        role,
        scope);
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private User findByKeycloakId(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND)));
  }

  private User findById(UUID userId) throws CustomException {
    return userRepo
        .findById(userId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException(ResponseMessageConstants.USER_NOT_FOUND)));
  }

  private void validateCallerIsPartner(User caller) throws CustomException {
    if (!caller.getRoles().contains(UserRole.PARTNER)) {
      throw new CustomException(
          new UnAuthorizedException("Seul un PARTNER peut gérer les sous-rôles de sa structure"));
    }
  }

  private void validatePartnerScope(RoleScope scope) throws CustomException {
    if (scope == RoleScope.UBAX_INTERNAL) {
      throw new CustomException(
          new BadRequestException(
              "Le scope UBAX_INTERNAL est réservé aux administrateurs système"));
    }
  }

  private void validateSameStructure(User caller, User target, RoleScope scope)
      throws CustomException {
    switch (scope) {
      case AGENCE -> {
        if (caller.getAgency() == null
            || target.getAgency() == null
            || !caller.getAgency().getId().equals(target.getAgency().getId())) {
          throw new CustomException(
              new UnAuthorizedException(
                  "Vous ne pouvez gérer que les membres de votre propre agence"));
        }
      }
      case HOTEL -> {
        if (caller.getHotel() == null
            || target.getHotel() == null
            || !caller.getHotel().getId().equals(target.getHotel().getId())) {
          throw new CustomException(
              new UnAuthorizedException(
                  "Vous ne pouvez gérer que les membres de votre propre hôtel"));
        }
      }
      default -> {}
    }
  }

  private UserSubRoleResponse toResponse(UserSubRole sub) {
    return new UserSubRoleResponse(
        sub.getId(), sub.getUser().getId(), sub.getRole(), sub.getScope(), sub.getCreatedAt());
  }
}
