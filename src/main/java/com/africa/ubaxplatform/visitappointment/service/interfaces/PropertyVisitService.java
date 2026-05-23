package com.africa.ubaxplatform.visitappointment.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.visitappointment.dto.ConfigureVisitAvailabilityDto;
import com.africa.ubaxplatform.visitappointment.dto.ConfirmVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.CreateVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.RejectVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.UpdateBlackoutDatesDto;
import com.africa.ubaxplatform.visitappointment.dto.VisitAvailabilityResponse;
import com.africa.ubaxplatform.visitappointment.dto.VisitRequestResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service métier pour les demandes de visite immobilière.
 *
 * <p>Gère la création, la confirmation et le rejet des demandes de visite côté client et agence.
 */
public interface PropertyVisitService {

  // ===== CLIENT ENDPOINTS =====

  /**
   * Crée une nouvelle demande de visite.
   *
   * @param keycloakId ID Keycloak du client
   * @param request DTO CreateVisitRequestDto
   * @return DTO VisitRequestResponse
   * @throws CustomException Si erreur métier
   */
  VisitRequestResponse createVisitRequest(String keycloakId, CreateVisitRequestDto request)
      throws CustomException;

  /**
   * Récupère les demandes de visite du client.
   *
   * @param keycloakId ID Keycloak du client
   * @param pageable Pagination
   * @return Page de demandes
   * @throws CustomException Si erreur métier
   */
  Page<VisitRequestResponse> getMyVisitRequests(String keycloakId, Pageable pageable)
      throws CustomException;

  /**
   * Récupère le détail d'une demande de visite.
   *
   * @param keycloakId ID Keycloak du demandeur (pour vérification droits)
   * @param visitRequestId ID de la demande
   * @return DTO VisitRequestResponse
   * @throws CustomException Si erreur métier ou accès refusé
   */
  VisitRequestResponse getVisitRequestDetail(String keycloakId, UUID visitRequestId)
      throws CustomException;

  /**
   * Annule une demande de visite (PENDING uniquement).
   *
   * @param keycloakId ID Keycloak du demandeur
   * @param visitRequestId ID de la demande
   * @throws CustomException Si erreur métier
   */
  void cancelVisitRequest(String keycloakId, UUID visitRequestId) throws CustomException;

  /**
   * Récupère les créneaux disponibles pour un bien (accès public pour les clients).
   *
   * @param propertyId ID du bien
   * @param daysAhead Nombre de jours à scanner (ex: 30)
   * @return DTO VisitAvailabilityResponse
   * @throws CustomException Si bien n'existe pas
   */
  VisitAvailabilityResponse getAvailableSlotsForProperty(UUID propertyId, int daysAhead)
      throws CustomException;

  // ===== AGENCY ENDPOINTS =====

  /**
   * Configure les créneaux disponibles pour un bien immobilier.
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param request DTO ConfigureVisitAvailabilityDto
   * @return DTO VisitAvailabilityResponse (réponse confirmation)
   * @throws CustomException Si erreur métier
   */
  VisitAvailabilityResponse configureAvailability(
      String keycloakId, ConfigureVisitAvailabilityDto request) throws CustomException;

  /**
   * Récupère la configuration actuelle d'un bien.
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param propertyId ID du bien
   * @return DTO VisitAvailabilityResponse
   * @throws CustomException Si erreur métier
   */
  VisitAvailabilityResponse getConfiguredAvailability(String keycloakId, UUID propertyId)
      throws CustomException;

  /**
   * Met à jour les dates fermées (blackout) d'un bien.
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param propertyId ID du bien
   * @param request DTO UpdateBlackoutDatesDto
   * @throws CustomException Si erreur métier
   */
  void updateBlackoutDates(String keycloakId, UUID propertyId, UpdateBlackoutDatesDto request)
      throws CustomException;

  /**
   * Récupère les demandes de visite d'une agence.
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param pageable Pagination
   * @return Page de demandes (en attente prioritairement)
   * @throws CustomException Si erreur métier
   */
  Page<VisitRequestResponse> getVisitRequestsForAgency(String keycloakId, Pageable pageable)
      throws CustomException;

  /**
   * Confirme une demande de visite (PENDING → CONFIRMED).
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param visitRequestId ID de la demande
   * @param request DTO ConfirmVisitRequestDto
   * @return DTO VisitRequestResponse
   * @throws CustomException Si erreur métier
   */
  VisitRequestResponse confirmVisitRequest(
      String keycloakId, UUID visitRequestId, ConfirmVisitRequestDto request)
      throws CustomException;

  /**
   * Rejette une demande de visite (PENDING → REJECTED).
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param visitRequestId ID de la demande
   * @param request DTO RejectVisitRequestDto
   * @return DTO VisitRequestResponse
   * @throws CustomException Si erreur métier
   */
  VisitRequestResponse rejectVisitRequest(
      String keycloakId, UUID visitRequestId, RejectVisitRequestDto request) throws CustomException;

  /**
   * Assigne un agent à une demande de visite.
   *
   * @param keycloakId ID Keycloak de l'agence
   * @param visitRequestId ID de la demande
   * @param agentId ID de l'agent à assigner
   * @return DTO VisitRequestResponse
   * @throws CustomException Si erreur métier
   */
  VisitRequestResponse assignAgent(String keycloakId, UUID visitRequestId, UUID agentId)
      throws CustomException;
}
