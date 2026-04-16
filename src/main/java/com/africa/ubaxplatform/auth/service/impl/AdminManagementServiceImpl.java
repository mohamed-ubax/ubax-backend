package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AdminUserResponse;
import com.africa.ubaxplatform.auth.dto.CreateAdminRequest;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.AdminManagementService;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des administrateurs internes UBAX.
 *
 * <p>Orchestre la création en Keycloak, l'attribution du rôle, la synchronisation en base et
 * l'envoi de l'email de définition du mot de passe. Un rollback Keycloak est déclenché en cas
 * d'échec de la persistance en base.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminManagementServiceImpl implements AdminManagementService {

  private static final Set<UserRole> ADMIN_ROLES = Set.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);

  private final UserRepository userRepo;
  private final KeycloakAdminService keycloakAdminService;

  @Override
  @Transactional
  public AdminUserResponse createAdmin(CreateAdminRequest request) throws CustomException {

    if (!ADMIN_ROLES.contains(request.getRole())) {
      throw new CustomException(
          new IllegalArgumentException(
              "Rôle invalide pour un administrateur UBAX : "
                  + request.getRole()
                  + ". Valeurs autorisées : ADMIN, SUPER_ADMIN"),
          ResponseMessageConstants.USER_CREATE_FAILURE);
    }

    if (userRepo.existsByEmail(request.getEmail())) {
      throw new CustomException(
          new IllegalArgumentException("Cet email est déjà utilisé : " + request.getEmail()),
          ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
    }

    String keycloakId =
        keycloakAdminService.createAdminAccount(
            request.getEmail(), request.getFirstName(), request.getLastName(), request.getPhone());
    log.info(
        "Compte administrateur créé dans Keycloak: keycloakId={}, email={}",
        keycloakId,
        request.getEmail());

    try {
      keycloakAdminService.assignRole(keycloakId, request.getRole());

      User user =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(request.getFirstName())
              .lastName(request.getLastName())
              .email(request.getEmail())
              .phone(request.getPhone())
              .roles(new HashSet<>(Set.of(request.getRole())))
              .phoneVerified(false)
              .emailVerified(true)
              .build();

      user = userRepo.save(user);
      log.info("Administrateur persisté en base: userId={}", user.getId());

      keycloakAdminService.sendSetPasswordLink(keycloakId);
      log.info("Email de définition du mot de passe envoyé à {}", request.getEmail());

      return toResponse(user);

    } catch (Exception e) {
      log.error(
          "Échec création administrateur pour keycloakId={}. Rollback Keycloak en cours...",
          keycloakId);
      keycloakAdminService.deleteUser(keycloakId);
      throw e;
    }
  }

  @Override
  public List<AdminUserResponse> listAdmins() {
    return userRepo.findAdminUsers(ADMIN_ROLES).stream()
        .filter(u -> !u.isDeleted())
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public AdminUserResponse assignAdminRole(UUID userId, UserRole newRole) throws CustomException {

    if (!ADMIN_ROLES.contains(newRole)) {
      throw new CustomException(
          new IllegalArgumentException(
              "Rôle invalide : " + newRole + ". Valeurs autorisées : ADMIN, SUPER_ADMIN"),
          ResponseMessageConstants.USER_UPDATE_FAILURE);
    }

    User user =
        userRepo
            .findById(userId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new IllegalArgumentException("Administrateur introuvable : " + userId),
                        ResponseMessageConstants.USER_GET_FAILURE_NOT_FOUND));

    if (user.isDeleted()) {
      throw new CustomException(
          new IllegalArgumentException("Ce compte administrateur est supprimé"),
          ResponseMessageConstants.USER_UPDATE_FAILURE);
    }

    if (user.getRoles().stream().noneMatch(ADMIN_ROLES::contains)) {
      throw new CustomException(
          new IllegalArgumentException("L'utilisateur n'est pas un administrateur UBAX"),
          ResponseMessageConstants.USER_UPDATE_FAILURE);
    }

    // Retirer les anciens rôles admin Keycloak
    for (UserRole oldRole : user.getRoles()) {
      if (ADMIN_ROLES.contains(oldRole)) {
        keycloakAdminService.removeRole(user.getKeycloakId(), oldRole);
      }
    }

    // Attribuer le nouveau rôle
    keycloakAdminService.assignRole(user.getKeycloakId(), newRole);

    // Mettre à jour en base
    user.getRoles().removeAll(ADMIN_ROLES);
    user.getRoles().add(newRole);
    user = userRepo.save(user);

    log.info("Rôle administrateur mis à jour : userId={}, newRole={}", userId, newRole);

    return toResponse(user);
  }

  @Override
  @Transactional
  public void deleteAdmin(UUID userId) throws CustomException {
    User user =
        userRepo
            .findById(userId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new IllegalArgumentException("Administrateur introuvable : " + userId),
                        ResponseMessageConstants.USER_GET_FAILURE_NOT_FOUND));

    if (!ADMIN_ROLES.containsAll(user.getRoles())
        && user.getRoles().stream().noneMatch(ADMIN_ROLES::contains)) {
      throw new CustomException(
          new IllegalArgumentException("L'utilisateur n'est pas un administrateur UBAX"),
          ResponseMessageConstants.USER_DELETE_FAILURE);
    }

    if (user.isDeleted()) {
      throw new CustomException(
          new IllegalArgumentException("Ce compte administrateur est déjà supprimé"),
          ResponseMessageConstants.USER_DELETE_FAILURE);
    }

    keycloakAdminService.disableUser(user.getKeycloakId());

    user.setDeletedAt(LocalDateTime.now());
    user.setActive(false);
    userRepo.save(user);

    log.info("Administrateur supprimé logiquement : userId={}, email={}", userId, user.getEmail());
  }

  private AdminUserResponse toResponse(User user) {
    return AdminUserResponse.builder()
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
