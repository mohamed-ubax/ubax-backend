package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AdminUserResponse;
import com.africa.ubaxplatform.auth.dto.CreateAdminRequest;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.impl.AdminManagementServiceImpl;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminManagementServiceImpl – tests unitaires")
class AdminManagementServiceImplTest {

  @Mock private UserRepository userRepo;
  @Mock private KeycloakAdminService keycloakAdminService;

  @InjectMocks private AdminManagementServiceImpl service;

  private static final String KC_ID = "kc-admin-uuid";

  private CreateAdminRequest buildCreateRequest(UserRole role) {
    CreateAdminRequest req = new CreateAdminRequest();
    req.setEmail("admin@ubax.africa");
    req.setFirstName("Admin");
    req.setLastName("UBAX");
    req.setPhone("+2250700000099");
    req.setRole(role);
    return req;
  }

  private User buildSavedAdmin(UUID userId, UserRole role) {
    User user =
        User.builder()
            .keycloakId(KC_ID)
            .firstName("Admin")
            .lastName("UBAX")
            .email("admin@ubax.africa")
            .phone("+2250700000099")
            .roles(new HashSet<>(Set.of(role)))
            .build();
    SharedTestFixtures.injectId(user, userId);
    return user;
  }

  @Nested
  @DisplayName("createAdmin()")
  class CreateAdmin {

    @Test
    @DisplayName("Succès – admin ADMIN créé en Keycloak et persisté")
    void createAdmin_adminRole_success() throws CustomException {
      UUID userId = UUID.randomUUID();
      CreateAdminRequest req = buildCreateRequest(UserRole.ADMIN);
      User saved = buildSavedAdmin(userId, UserRole.ADMIN);

      when(userRepo.existsByEmail(req.getEmail())).thenReturn(false);
      when(keycloakAdminService.createAdminAccount(
              req.getEmail(), req.getFirstName(), req.getLastName(), req.getPhone()))
          .thenReturn(KC_ID);
      doNothing().when(keycloakAdminService).assignRole(KC_ID, UserRole.ADMIN);
      when(userRepo.save(any(User.class))).thenReturn(saved);
      doNothing().when(keycloakAdminService).sendSetPasswordLink(KC_ID);

      AdminUserResponse result = service.createAdmin(req);

      assertThat(result.getEmail()).isEqualTo("admin@ubax.africa");
      assertThat(result.getRoles()).contains(UserRole.ADMIN);
      verify(keycloakAdminService).createAdminAccount(any(), any(), any(), any());
      verify(keycloakAdminService).assignRole(KC_ID, UserRole.ADMIN);
      verify(keycloakAdminService).sendSetPasswordLink(KC_ID);
    }

    @Test
    @DisplayName("Échec – rôle CLIENT invalide → USER_CREATE_FAILURE")
    void createAdmin_invalidRole_throwsCustomException() throws CustomException {
      CreateAdminRequest req = buildCreateRequest(UserRole.CLIENT);

      assertThatThrownBy(() -> service.createAdmin(req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_CREATE_FAILURE);
      verify(userRepo, never()).save(any());
      verify(keycloakAdminService, never()).createAdminAccount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Échec – email déjà utilisé → USER_CREATE_FAILURE_ALREADY_EXISTS")
    void createAdmin_emailAlreadyExists_throwsConflict() throws CustomException {
      CreateAdminRequest req = buildCreateRequest(UserRole.ADMIN);
      when(userRepo.existsByEmail(req.getEmail())).thenReturn(true);

      assertThatThrownBy(() -> service.createAdmin(req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
      verify(keycloakAdminService, never()).createAdminAccount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Échec – erreur persistance → rollback Keycloak et rethrow")
    void createAdmin_persistenceFails_rollsBackKeycloak() throws CustomException {
      CreateAdminRequest req = buildCreateRequest(UserRole.ADMIN);

      when(userRepo.existsByEmail(req.getEmail())).thenReturn(false);
      when(keycloakAdminService.createAdminAccount(any(), any(), any(), any())).thenReturn(KC_ID);
      doNothing().when(keycloakAdminService).assignRole(any(), any());
      when(userRepo.save(any())).thenThrow(new RuntimeException("DB down"));
      doNothing().when(keycloakAdminService).deleteUser(KC_ID);

      assertThatThrownBy(() -> service.createAdmin(req)).isInstanceOf(RuntimeException.class);

      verify(keycloakAdminService).deleteUser(KC_ID);
    }
  }

  @Nested
  @DisplayName("listAdmins()")
  class ListAdmins {

    @Test
    @DisplayName("Succès – retourne uniquement les admins non supprimés")
    void listAdmins_returnsNonDeletedAdmins() {
      User active = buildSavedAdmin(UUID.randomUUID(), UserRole.ADMIN);
      User deleted = buildSavedAdmin(UUID.randomUUID(), UserRole.SUPER_ADMIN);
      deleted.setDeletedAt(LocalDateTime.now());

      when(userRepo.findAdminUsers(any())).thenReturn(List.of(active, deleted));

      List<AdminUserResponse> result = service.listAdmins();

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().getEmail()).isEqualTo("admin@ubax.africa");
    }
  }

  @Nested
  @DisplayName("assignAdminRole()")
  class AssignAdminRole {

    @Test
    @DisplayName("Succès – rôle mis à jour en Keycloak et en base")
    void assignAdminRole_success_updatesRole() throws CustomException {
      UUID userId = UUID.randomUUID();
      User user = buildSavedAdmin(userId, UserRole.ADMIN);

      when(userRepo.findById(userId)).thenReturn(Optional.of(user));
      when(userRepo.save(any(User.class))).thenReturn(user);
      doNothing().when(keycloakAdminService).removeRole(any(), any());
      doNothing().when(keycloakAdminService).assignRole(any(), any());

      AdminUserResponse result = service.assignAdminRole(userId, UserRole.SUPER_ADMIN);

      assertThat(result).isNotNull();
      verify(keycloakAdminService).removeRole(KC_ID, UserRole.ADMIN);
      verify(keycloakAdminService).assignRole(KC_ID, UserRole.SUPER_ADMIN);
      verify(userRepo).save(any());
    }

    @Test
    @DisplayName("Échec – rôle CLIENT invalide → USER_UPDATE_FAILURE")
    void assignAdminRole_invalidRole_throwsCustomException() {
      UUID userId = UUID.randomUUID();

      assertThatThrownBy(() -> service.assignAdminRole(userId, UserRole.CLIENT))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UPDATE_FAILURE);
      verify(userRepo, never()).findById(any());
    }

    @Test
    @DisplayName("Échec – admin introuvable → USER_GET_FAILURE_NOT_FOUND")
    void assignAdminRole_notFound_throwsNotFoundException() {
      UUID userId = UUID.randomUUID();
      when(userRepo.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignAdminRole(userId, UserRole.ADMIN))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – admin supprimé → USER_UPDATE_FAILURE")
    void assignAdminRole_deletedUser_throwsCustomException() {
      UUID userId = UUID.randomUUID();
      User deleted = buildSavedAdmin(userId, UserRole.ADMIN);
      deleted.setDeletedAt(LocalDateTime.now());

      when(userRepo.findById(userId)).thenReturn(Optional.of(deleted));

      assertThatThrownBy(() -> service.assignAdminRole(userId, UserRole.SUPER_ADMIN))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UPDATE_FAILURE);
    }

    @Test
    @DisplayName("Échec – l'utilisateur n'est pas un admin → USER_UPDATE_FAILURE")
    void assignAdminRole_notAdminUser_throwsCustomException() {
      UUID userId = UUID.randomUUID();
      User clientUser =
          User.builder()
              .keycloakId(KC_ID)
              .firstName("Client")
              .lastName("Test")
              .email("client@test.ci")
              .roles(new HashSet<>(Set.of(UserRole.CLIENT)))
              .build();
      SharedTestFixtures.injectId(clientUser, userId);

      when(userRepo.findById(userId)).thenReturn(Optional.of(clientUser));

      assertThatThrownBy(() -> service.assignAdminRole(userId, UserRole.ADMIN))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_UPDATE_FAILURE);
    }
  }

  @Nested
  @DisplayName("deleteAdmin()")
  class DeleteAdmin {

    @Test
    @DisplayName("Succès – admin désactivé et soft-deleted")
    void deleteAdmin_success_disablesAndSoftDeletes() throws CustomException {
      UUID userId = UUID.randomUUID();
      User user = buildSavedAdmin(userId, UserRole.ADMIN);

      when(userRepo.findById(userId)).thenReturn(Optional.of(user));
      doNothing().when(keycloakAdminService).disableUser(KC_ID);
      when(userRepo.save(any(User.class))).thenReturn(user);

      service.deleteAdmin(userId);

      ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
      verify(userRepo).save(captor.capture());
      assertThat(captor.getValue().getDeletedAt()).isNotNull();
      assertThat(captor.getValue().isActive()).isFalse();
      verify(keycloakAdminService).disableUser(KC_ID);
    }

    @Test
    @DisplayName("Échec – admin introuvable → USER_GET_FAILURE_NOT_FOUND")
    void deleteAdmin_notFound_throwsNotFoundException() {
      UUID userId = UUID.randomUUID();
      when(userRepo.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deleteAdmin(userId))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_GET_FAILURE_NOT_FOUND);
      verify(keycloakAdminService, never()).disableUser(any());
    }

    @Test
    @DisplayName("Échec – admin déjà supprimé → USER_DELETE_FAILURE")
    void deleteAdmin_alreadyDeleted_throwsCustomException() {
      UUID userId = UUID.randomUUID();
      User deleted = buildSavedAdmin(userId, UserRole.ADMIN);
      deleted.setDeletedAt(LocalDateTime.now());

      when(userRepo.findById(userId)).thenReturn(Optional.of(deleted));

      assertThatThrownBy(() -> service.deleteAdmin(userId))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_DELETE_FAILURE);
      verify(keycloakAdminService, never()).disableUser(any());
    }
  }
}
