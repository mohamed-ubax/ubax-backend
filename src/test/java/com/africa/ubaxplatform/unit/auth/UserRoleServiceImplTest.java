package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.AgenceRole;
import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UbaxAdminRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.UserSubRoleResponse;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.entity.UserSubRole;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.repository.UserSubRoleRepository;
import com.africa.ubaxplatform.auth.service.impl.UserRoleServiceImpl;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.util.List;
import java.util.Optional;
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
@DisplayName("UserRoleServiceImpl – tests unitaires")
class UserRoleServiceImplTest {

  @Mock private UserSubRoleRepository subRoleRepo;
  @Mock private UserRepository userRepo;

  @InjectMocks private UserRoleServiceImpl service;

  private User buildPartnerUser() {
    return SharedTestFixtures.buildPartnerUser();
  }

  private User buildAdminUser() {
    return SharedTestFixtures.buildAdminUser(
        SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID, UserRole.ADMIN);
  }

  @Nested
  @DisplayName("assignSubRoles()")
  class AssignSubRoles {

    @Test
    @DisplayName("Succès – sous-rôle AGENCE assigné à un PARTNER")
    void assignSubRoles_agenceScope_partnerUser_returnsAssigned() throws CustomException {
      User partner = buildPartnerUser();
      UUID userId = partner.getId();
      String role = AgenceRole.DIRECTEUR_AGENCE.name();
      List<String> roles = List.of(role);

      UserSubRole saved = SharedTestFixtures.buildSubRole(partner, role, RoleScope.AGENCE);

      when(userRepo.findById(userId)).thenReturn(Optional.of(partner));
      when(subRoleRepo.existsByUserIdAndRoleAndScope(userId, role, RoleScope.AGENCE))
          .thenReturn(false);
      when(subRoleRepo.save(any(UserSubRole.class))).thenReturn(saved);

      List<UserSubRoleResponse> result = service.assignSubRoles(userId, roles, RoleScope.AGENCE);

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().role()).isEqualTo(role);
      assertThat(result.getFirst().scope()).isEqualTo(RoleScope.AGENCE);

      ArgumentCaptor<UserSubRole> captor = ArgumentCaptor.forClass(UserSubRole.class);
      verify(subRoleRepo).save(captor.capture());
      assertThat(captor.getValue().getRole()).isEqualTo(role);
      assertThat(captor.getValue().getScope()).isEqualTo(RoleScope.AGENCE);
    }

    @Test
    @DisplayName("Succès – sous-rôle déjà existant ignoré (idempotent)")
    void assignSubRoles_alreadyExists_skipsAndReturnsEmpty() throws CustomException {
      User partner = buildPartnerUser();
      UUID userId = partner.getId();
      String role = AgenceRole.COMMERCIAL.name();

      when(userRepo.findById(userId)).thenReturn(Optional.of(partner));
      when(subRoleRepo.existsByUserIdAndRoleAndScope(userId, role, RoleScope.AGENCE))
          .thenReturn(true);

      List<UserSubRoleResponse> result =
          service.assignSubRoles(userId, List.of(role), RoleScope.AGENCE);

      assertThat(result).isEmpty();
      verify(subRoleRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – utilisateur introuvable → USER_NOT_FOUND")
    void assignSubRoles_userNotFound_throwsNotFoundException() {
      UUID unknownId = UUID.randomUUID();
      when(userRepo.findById(unknownId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.assignSubRoles(
                      unknownId, List.of(AgenceRole.COMMERCIAL.name()), RoleScope.AGENCE))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
      verify(subRoleRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – scope UBAX_INTERNAL attribué à un PARTNER → BadRequest")
    void assignSubRoles_internalScopeOnPartner_throwsBadRequest() {
      User partner = buildPartnerUser();
      when(userRepo.findById(partner.getId())).thenReturn(Optional.of(partner));

      assertThatThrownBy(
              () ->
                  service.assignSubRoles(
                      partner.getId(),
                      List.of(UbaxAdminRole.FINANCE.name()),
                      RoleScope.UBAX_INTERNAL))
          .isInstanceOf(CustomException.class);
      verify(subRoleRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – scope AGENCE attribué à un ADMIN → BadRequest")
    void assignSubRoles_agenceScopeOnAdmin_throwsBadRequest() {
      User admin = buildAdminUser();
      when(userRepo.findById(admin.getId())).thenReturn(Optional.of(admin));

      assertThatThrownBy(
              () ->
                  service.assignSubRoles(
                      admin.getId(), List.of(AgenceRole.COMMERCIAL.name()), RoleScope.AGENCE))
          .isInstanceOf(CustomException.class);
      verify(subRoleRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – rôle invalide pour le scope → BadRequest")
    void assignSubRoles_invalidRoleForScope_throwsBadRequest() {
      User partner = buildPartnerUser();
      when(userRepo.findById(partner.getId())).thenReturn(Optional.of(partner));

      assertThatThrownBy(
              () ->
                  service.assignSubRoles(
                      partner.getId(), List.of("INVALID_ROLE"), RoleScope.AGENCE))
          .isInstanceOf(CustomException.class);
      verify(subRoleRepo, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getSubRoles()")
  class GetSubRoles {

    @Test
    @DisplayName("Succès – retourne les sous-rôles filtrés par scope")
    void getSubRoles_withScope_returnsFiltered() {
      User partner = buildPartnerUser();
      UserSubRole sub =
          SharedTestFixtures.buildSubRole(partner, AgenceRole.COMMERCIAL.name(), RoleScope.AGENCE);
      when(subRoleRepo.findByUserIdAndScope(partner.getId(), RoleScope.AGENCE))
          .thenReturn(List.of(sub));

      List<UserSubRoleResponse> result = service.getSubRoles(partner.getId(), RoleScope.AGENCE);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).role()).isEqualTo(AgenceRole.COMMERCIAL.name());
    }

    @Test
    @DisplayName("Succès – scope null retourne tous les sous-rôles")
    void getSubRoles_noScope_returnsAll() {
      User partner = buildPartnerUser();
      UserSubRole sub =
          SharedTestFixtures.buildSubRole(
              partner, AgenceRole.COMPTABLE_AGENCE.name(), RoleScope.AGENCE);
      when(subRoleRepo.findByUserId(partner.getId())).thenReturn(List.of(sub));

      List<UserSubRoleResponse> result = service.getSubRoles(partner.getId(), null);

      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Succès – retourne liste vide si aucun sous-rôle")
    void getSubRoles_noRoles_returnsEmpty() {
      UUID userId = UUID.randomUUID();
      when(subRoleRepo.findByUserId(userId)).thenReturn(List.of());

      List<UserSubRoleResponse> result = service.getSubRoles(userId, null);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("revokeSubRole()")
  class RevokeSubRole {

    @Test
    @DisplayName("Succès – sous-rôle révoqué")
    void revokeSubRole_exists_deletesSuccessfully() throws CustomException {
      UUID userId = SharedTestFixtures.USER_ID;
      String role = AgenceRole.AGENT_SAV.name();

      when(subRoleRepo.existsByUserIdAndRoleAndScope(userId, role, RoleScope.AGENCE))
          .thenReturn(true);

      service.revokeSubRole(userId, role, RoleScope.AGENCE);

      verify(subRoleRepo).deleteByUserIdAndRoleAndScope(userId, role, RoleScope.AGENCE);
    }

    @Test
    @DisplayName("Échec – sous-rôle introuvable → NotFoundException")
    void revokeSubRole_notFound_throwsNotFoundException() {
      UUID userId = UUID.randomUUID();
      when(subRoleRepo.existsByUserIdAndRoleAndScope(userId, "MISSING_ROLE", RoleScope.AGENCE))
          .thenReturn(false);

      assertThatThrownBy(() -> service.revokeSubRole(userId, "MISSING_ROLE", RoleScope.AGENCE))
          .isInstanceOf(CustomException.class);
      verify(subRoleRepo, never()).deleteByUserIdAndRoleAndScope(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("hasSubRole()")
  class HasSubRole {

    @Test
    @DisplayName("Retourne true si le sous-rôle existe")
    void hasSubRole_exists_returnsTrue() {
      UUID userId = SharedTestFixtures.USER_ID;
      when(subRoleRepo.existsByUserIdAndRoleAndScope(
              userId, AgenceRole.DIRECTEUR_AGENCE.name(), RoleScope.AGENCE))
          .thenReturn(true);

      boolean result =
          service.hasSubRole(userId, AgenceRole.DIRECTEUR_AGENCE.name(), RoleScope.AGENCE);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Retourne false si le sous-rôle n'existe pas")
    void hasSubRole_notExists_returnsFalse() {
      UUID userId = UUID.randomUUID();
      when(subRoleRepo.existsByUserIdAndRoleAndScope(userId, "ANY_ROLE", RoleScope.AGENCE))
          .thenReturn(false);

      boolean result = service.hasSubRole(userId, "ANY_ROLE", RoleScope.AGENCE);

      assertThat(result).isFalse();
    }
  }
}
