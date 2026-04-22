package com.africa.ubaxplatform.auth.dto;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant un sous-rôle applicatif d'un utilisateur.
 *
 * @param id identifiant de l'entrée
 * @param userId identifiant de l'utilisateur
 * @param role valeur du sous-rôle (ex : FINANCE, DIRECTEUR_AGENCE)
 * @param scope portée du sous-rôle (UBAX_INTERNAL, AGENCE, HOTEL)
 * @param createdAt date d'assignation
 */
public record UserSubRoleResponse(
    UUID id, UUID userId, String role, RoleScope scope, LocalDateTime createdAt) {}
