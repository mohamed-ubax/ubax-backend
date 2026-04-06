package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Réponse retournée après une inscription complète et réussie. */
@Getter
@Builder
public class RegisterResponse {

  private UUID userId;
  private String keycloakId;
  private String email;
  private String phone;
  private String firstName;
  private String lastName;
  private Set<UserRole> roles;
}
