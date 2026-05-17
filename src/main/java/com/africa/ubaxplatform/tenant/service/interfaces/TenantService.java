package com.africa.ubaxplatform.tenant.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.dto.TenantCreateRequest;
import com.africa.ubaxplatform.tenant.dto.TenantResponse;
import com.africa.ubaxplatform.tenant.dto.TenantUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Contrat du service de gestion des dossiers locataires. */
public interface TenantService {

  /**
   * Crée un dossier locataire pour l'utilisateur authentifié.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param request corps de la demande
   * @return le dossier créé
   */
  TenantResponse create(String keycloakId, TenantCreateRequest request) throws CustomException;

  /**
   * Retourne le dossier locataire de l'utilisateur authentifié.
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @return le dossier locataire
   */
  TenantResponse getMyProfile(String keycloakId) throws CustomException;

  /**
   * Met à jour le dossier locataire de l'utilisateur authentifié (champs non-null uniquement).
   *
   * @param keycloakId identifiant Keycloak de l'utilisateur
   * @param request champs à mettre à jour
   * @return le dossier mis à jour
   */
  TenantResponse updateMyProfile(String keycloakId, TenantUpdateRequest request)
      throws CustomException;

  /**
   * Récupère un dossier locataire par son identifiant (admin / agent).
   *
   * @param id identifiant du dossier
   * @return le dossier locataire
   */
  TenantResponse getById(String keycloakId, UUID id) throws CustomException;

  /**
   * Retourne une page de dossiers locataires, filtrable par statut et/ou bien.
   *
   * <ul>
   *   <li>ADMIN / SUPER_ADMIN : tous les dossiers non archivés.
   *   <li>PARTNER agence : uniquement les dossiers liés à leurs contrats.
   *   <li>PARTNER hôtel : accès refusé (les hôtels gèrent des réservations, pas des dossiers KYC).
   * </ul>
   *
   * @param keycloakId identifiant Keycloak de l'appelant
   * @param status filtre optionnel ; null = tous les statuts
   * @param propertyId filtre optionnel par bien ; null = tous les biens
   * @param pageable pagination
   * @return page de dossiers
   */
  /**
   * @param withoutContract si {@code true} et {@code propertyId} fourni : exclut les dossiers ayant
   *     déjà un contrat pour ce bien (candidatures en attente de décision)
   */
  Page<TenantResponse> list(
      String keycloakId,
      TenantStatus status,
      UUID propertyId,
      Boolean withoutContract,
      Pageable pageable)
      throws CustomException;

  /**
   * Qualifie un dossier locataire (passe son statut en {@code QUALIFIED}).
   *
   * @param id identifiant du dossier
   * @param reviewerKeycloakId identifiant Keycloak de l'agent/gestionnaire
   * @return le dossier mis à jour
   */
  TenantResponse qualify(UUID id, String reviewerKeycloakId) throws CustomException;

  /**
   * Rejette un dossier locataire avec un motif.
   *
   * @param id identifiant du dossier
   * @param reason motif du refus
   * @param callerKeycloakId identifiant Keycloak de l'agent/gestionnaire
   * @return le dossier mis à jour
   */
  TenantResponse reject(UUID id, String reason, String callerKeycloakId) throws CustomException;

  /**
   * Supprime logiquement un dossier locataire (soft delete).
   *
   * @param id identifiant du dossier
   */
  void delete(UUID id) throws CustomException;
}
