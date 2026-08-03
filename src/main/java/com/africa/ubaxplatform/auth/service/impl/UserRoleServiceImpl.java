package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.AgenceRole;
import com.africa.ubaxplatform.auth.codeList.HotelRole;
import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AddTeamMemberRequest;
import com.africa.ubaxplatform.auth.dto.UserResponse;
import com.africa.ubaxplatform.auth.dto.UserSubRoleResponse;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.entity.UserSubRole;
import com.africa.ubaxplatform.auth.mapper.UserMapper;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.repository.UserSubRoleRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.codelist.entity.LaCodeList;
import com.africa.ubaxplatform.common.codelist.repository.LaCodeListRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
  private final KeycloakAdminService keycloakAdminService;

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
    validateCallerCanManageTeam(caller, scope);
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
    validateCallerCanManageTeam(caller, scope);
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

  // ── Listes de membres ─────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getTeamMembers(String callerKeycloakId, RoleScope scope)
      throws CustomException {
    User caller = findByKeycloakId(callerKeycloakId);
    validatePartnerScope(scope);
    validateCallerIsPartner(caller);

    return switch (scope) {
      case AGENCE -> {
        if (caller.getAgency() == null) {
          throw new CustomException(
              new BadRequestException("Vous n'êtes rattaché à aucune agence"));
        }
        yield userRepo.findByAgencyIdAndDeletedAtIsNull(caller.getAgency().getId()).stream()
            .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
            .toList();
      }
      case HOTEL -> {
        if (caller.getHotel() == null) {
          throw new CustomException(new BadRequestException("Vous n'êtes rattaché à aucun hôtel"));
        }
        yield userRepo.findByHotelIdAndDeletedAtIsNull(caller.getHotel().getId()).stream()
            .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
            .toList();
      }
      default ->
          throw new CustomException(new BadRequestException("Scope non supporté : " + scope));
    };
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getInactiveTeamMembers(String callerKeycloakId, RoleScope scope)
      throws CustomException {
    User caller = findByKeycloakId(callerKeycloakId);
    validatePartnerScope(scope);
    validateCallerIsPartner(caller);

    return switch (scope) {
      case AGENCE -> {
        if (caller.getAgency() == null) {
          throw new CustomException(
              new BadRequestException("Vous n'êtes rattaché à aucune agence"));
        }
        yield userRepo.findByAgencyIdAndDeletedAtIsNotNull(caller.getAgency().getId()).stream()
            .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
            .toList();
      }
      case HOTEL -> {
        if (caller.getHotel() == null) {
          throw new CustomException(new BadRequestException("Vous n'êtes rattaché à aucun hôtel"));
        }
        yield userRepo.findByHotelIdAndDeletedAtIsNotNull(caller.getHotel().getId()).stream()
            .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
            .toList();
      }
      default ->
          throw new CustomException(new BadRequestException("Scope non supporté : " + scope));
    };
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getAgencyMembersForAdmin(UUID agencyId) {
    return userRepo.findByAgencyIdAndDeletedAtIsNull(agencyId).stream()
        .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getInactiveAgencyMembersForAdmin(UUID agencyId) {
    return userRepo.findByAgencyIdAndDeletedAtIsNotNull(agencyId).stream()
        .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getHotelMembersForAdmin(UUID hotelId) {
    return userRepo.findByHotelIdAndDeletedAtIsNull(hotelId).stream()
        .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getInactiveHotelMembersForAdmin(UUID hotelId) {
    return userRepo.findByHotelIdAndDeletedAtIsNotNull(hotelId).stream()
        .map(u -> UserMapper.toResponse(u, fetchSubRoles(u.getId())))
        .toList();
  }

  // ── Suppression d'un membre d'équipe ─────────────────────────────

  @Override
  @Transactional
  public void removeTeamMember(String callerKeycloakId, UUID targetUserId, RoleScope scope)
      throws CustomException {

    User caller = findByKeycloakId(callerKeycloakId);
    User target = findById(targetUserId);

    validatePartnerScope(scope);
    validateCallerIsPartner(caller);
    validateCallerCanManageTeam(caller, scope);
    validateSameStructure(caller, target, scope);

    if (caller.getId().equals(targetUserId)) {
      throw new CustomException(
          new BadRequestException("Vous ne pouvez pas vous supprimer vous-même"));
    }

    if (target.getRoles().contains(UserRole.PARTNER_ADMIN)) {
      throw new CustomException(
          new BadRequestException("Le fondateur de la structure ne peut pas être retiré"));
    }

    if (target.isDeleted()) {
      throw new CustomException(new BadRequestException("Ce membre est déjà supprimé"));
    }

    keycloakAdminService.disableUser(target.getKeycloakId());
    target.setDeletedAt(LocalDateTime.now());
    target.setActive(false);
    userRepo.save(target);

    log.info(
        "Membre retiré : caller={}, targetId={}, scope={}", callerKeycloakId, targetUserId, scope);
  }

  @Override
  @Transactional
  public void reactivateTeamMember(String callerKeycloakId, UUID targetUserId, RoleScope scope)
      throws CustomException {

    User caller = findByKeycloakId(callerKeycloakId);
    User target = findById(targetUserId);

    validatePartnerScope(scope);
    validateCallerIsPartner(caller);
    validateCallerCanManageTeam(caller, scope);
    validateSameStructure(caller, target, scope);

    if (!target.isDeleted()) {
      throw new CustomException(new BadRequestException("Ce membre est déjà actif"));
    }

    keycloakAdminService.enableUser(target.getKeycloakId());
    target.setDeletedAt(null);
    target.setActive(true);
    userRepo.save(target);

    log.info(
        "Membre réactivé : caller={}, targetId={}, scope={}",
        callerKeycloakId,
        targetUserId,
        scope);
  }

  // ── Ajout d'un membre d'équipe ────────────────────────────────────

  @Override
  @Transactional
  public UserResponse addTeamMember(
      String callerKeycloakId, AddTeamMemberRequest request, RoleScope scope)
      throws CustomException {

    User caller = findByKeycloakId(callerKeycloakId);
    validatePartnerScope(scope);
    validateCallerIsPartner(caller);
    validateCallerCanManageTeam(caller, scope);

    if (userRepo.existsByEmail(request.getEmail())) {
      throw new CustomException(
          new ConflictException("Un compte avec cet email existe déjà : " + request.getEmail()));
    }

    String keycloakId = null;
    try {
      keycloakId =
          keycloakAdminService.createAdminAccount(
              request.getEmail(),
              request.getFirstName(),
              request.getLastName(),
              request.getPhone());

      keycloakAdminService.assignRole(keycloakId, UserRole.PARTNER);
      keycloakAdminService.sendSetPasswordLink(keycloakId);

      User member =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(request.getFirstName())
              .lastName(request.getLastName())
              .email(request.getEmail())
              .phone(request.getPhone())
              .roles(new HashSet<>(Set.of(UserRole.PARTNER)))
              .agency(scope == RoleScope.AGENCE ? caller.getAgency() : null)
              .hotel(scope == RoleScope.HOTEL ? caller.getHotel() : null)
              .build();

      member = userRepo.save(member);

      if (request.getSubRoles() != null && !request.getSubRoles().isEmpty()) {
        validateRolesForScope(request.getSubRoles(), scope);
        for (String role : request.getSubRoles()) {
          subRoleRepo.save(UserSubRole.builder().user(member).role(role).scope(scope).build());
        }
      }

      log.info("Membre ajouté : caller={}, new={}, scope={}", callerKeycloakId, keycloakId, scope);
      return UserMapper.toResponse(member, fetchSubRoles(member.getId()));

    } catch (Exception e) {
      if (keycloakId != null) {
        keycloakAdminService.deleteUser(keycloakId);
      }
      if (e instanceof CustomException customException) {
        throw customException;
      }
      throw new CustomException(e, "Erreur lors de l'ajout du membre");
    }
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

  /**
   * Vérifie que le caller peut gérer l'équipe (inviter, assigner, révoquer des sous-rôles).
   *
   * <p>Autorisé si : rôle Keycloak {@code PARTNER_ADMIN} (fondateur) OU sous-rôle DG/GERANT actif
   * pour le scope concerné.
   */
  private void validateCallerCanManageTeam(User caller, RoleScope scope) throws CustomException {
    if (caller.getRoles().contains(UserRole.PARTNER_ADMIN)) {
      return;
    }
    String dgRole =
        scope == RoleScope.AGENCE
            ? AgenceRole.DIRECTEUR_AGENCE.name()
            : HotelRole.GERANT_HOTEL.name();
    if (subRoleRepo.existsByUserIdAndRoleAndScope(caller.getId(), dgRole, scope)) {
      return;
    }
    throw new CustomException(
        new UnAuthorizedException(
            "Seul le fondateur (PARTNER_ADMIN) ou le directeur (DG/GERANT) peut gérer l'équipe"));
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

  private List<String> fetchSubRoles(UUID userId) {
    return subRoleRepo.findByUserId(userId).stream().map(UserSubRole::getRole).toList();
  }

  private UserSubRoleResponse toResponse(UserSubRole sub) {
    return new UserSubRoleResponse(
        sub.getId(), sub.getUser().getId(), sub.getRole(), sub.getScope(), sub.getCreatedAt());
  }
}
