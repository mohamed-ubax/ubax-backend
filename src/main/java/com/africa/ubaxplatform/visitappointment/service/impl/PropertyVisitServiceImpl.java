package com.africa.ubaxplatform.visitappointment.service.impl;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.visitappointment.codeList.VisitRequestStatus;
import com.africa.ubaxplatform.visitappointment.dto.ConfigureVisitAvailabilityDto;
import com.africa.ubaxplatform.visitappointment.dto.ConfirmVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.CreateVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.RejectVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.UpdateBlackoutDatesDto;
import com.africa.ubaxplatform.visitappointment.dto.VisitAvailabilityResponse;
import com.africa.ubaxplatform.visitappointment.dto.VisitAvailabilityResponse.AvailableSlot;
import com.africa.ubaxplatform.visitappointment.dto.VisitRequestResponse;
import com.africa.ubaxplatform.visitappointment.entity.AgencyVisitAvailability;
import com.africa.ubaxplatform.visitappointment.entity.PropertyVisitRequest;
import com.africa.ubaxplatform.visitappointment.mapper.VisitRequestMapper;
import com.africa.ubaxplatform.visitappointment.repository.AgencyVisitAvailabilityRepository;
import com.africa.ubaxplatform.visitappointment.repository.PropertyVisitRequestRepository;
import com.africa.ubaxplatform.visitappointment.service.interfaces.PropertyVisitService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyVisitServiceImpl implements PropertyVisitService {

  private final PropertyVisitRequestRepository visitRequestRepository;
  private final AgencyVisitAvailabilityRepository availabilityRepository;
  private final UserRepository userRepository;
  private final PropertyRepository propertyRepository;

  // ===== CLIENT ENDPOINTS =====

  @Override
  public VisitRequestResponse createVisitRequest(String keycloakId, CreateVisitRequestDto request)
      throws CustomException {

    // Vérifier que le client existe
    User client =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Client non trouvé"));

    // Vérifier que le bien existe et est publié
    UUID propertyId = UUID.fromString(request.getPropertyId());
    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Bien immobilier non trouvé"));

    // Vérifier que la date demandée est dans le futur (au moins demain)
    if (request.getRequestedDate().isBefore(LocalDate.now().plusDays(1))) {
      throw new CustomException("La date demandée doit être au minimum demain");
    }

    // Vérifier qu'une demande n'existe pas déjà pour ce bien/client
    if (visitRequestRepository
        .findByClientIdAndPropertyId(client.getId(), property.getId())
        .isPresent()) {
      throw new CustomException("Vous avez déjà une demande de visite pour ce bien");
    }

    // Vérifier la disponibilité côté agence
    AgencyVisitAvailability availability =
        availabilityRepository
            .findByPropertyId(property.getId())
            .orElseThrow(
                () -> new CustomException("Ce bien n'a pas encore de créneaux configurés"));

    if (!availability.getIsActive()) {
      throw new CustomException("Les visites sont actuellement fermées pour ce bien");
    }

    // Créer la demande
    PropertyVisitRequest visitRequest =
        PropertyVisitRequest.builder()
            .property(property)
            .client(client)
            .requestedDate(request.getRequestedDate())
            .requestedTimeSlot(request.getRequestedTimeSlot())
            .status(VisitRequestStatus.PENDING)
            .clientNotes(request.getClientNotes())
            .build();

    PropertyVisitRequest saved = visitRequestRepository.save(visitRequest);

    return VisitRequestMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<VisitRequestResponse> getMyVisitRequests(String keycloakId, Pageable pageable)
      throws CustomException {

    User client =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Client non trouvé"));

    return visitRequestRepository
        .findByClientIdOrderByCreatedAtDesc(client.getId(), pageable)
        .map(VisitRequestMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public VisitRequestResponse getVisitRequestDetail(String keycloakId, UUID visitRequestId)
      throws CustomException {

    User caller =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    PropertyVisitRequest visitRequest =
        visitRequestRepository
            .findById(visitRequestId)
            .orElseThrow(() -> new NotFoundException("Demande de visite non trouvée"));

    // Vérifier l'accès : client = propriétaire de la demande, agence = propriétaire du bien
    boolean isClient = visitRequest.getClient().getId().equals(caller.getId());
    boolean isAgency =
        visitRequest
            .getProperty()
            .getAgency()
            .getId()
            .equals(caller.getAgency() != null ? caller.getAgency().getId() : null);

    if (!isClient && !isAgency) {
      throw new CustomException("Accès refusé à cette demande de visite");
    }

    return VisitRequestMapper.toResponse(visitRequest);
  }

  @Override
  public void cancelVisitRequest(String keycloakId, UUID visitRequestId) throws CustomException {

    User caller =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    PropertyVisitRequest visitRequest =
        visitRequestRepository
            .findById(visitRequestId)
            .orElseThrow(() -> new NotFoundException("Demande de visite non trouvée"));

    // Vérifier que le client est propriétaire de la demande
    if (!visitRequest.getClient().getId().equals(caller.getId())) {
      throw new CustomException("Vous ne pouvez annuler que vos propres demandes");
    }

    // Vérifier que le statut est PENDING
    if (visitRequest.getStatus() != VisitRequestStatus.PENDING) {
      throw new CustomException("Seules les demandes en attente peuvent être annulées");
    }

    visitRequest.setStatus(VisitRequestStatus.CANCELLED);
    visitRequestRepository.save(visitRequest);
  }

  @Override
  @Transactional(readOnly = true)
  public VisitAvailabilityResponse getAvailableSlotsForProperty(UUID propertyId, int daysAhead)
      throws CustomException {

    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Bien immobilier non trouvé"));

    AgencyVisitAvailability availability =
        availabilityRepository
            .findByPropertyId(propertyId)
            .orElseThrow(() -> new CustomException("Ce bien n'a pas de créneaux configurés"));

    if (!availability.getIsActive()) {
      throw new CustomException("Les visites sont fermées pour ce bien");
    }

    List<LocalDate> availableDates = new ArrayList<>();
    Map<LocalDate, List<AvailableSlot>> slotsByDate = new HashMap<>();

    // Scanner les daysAhead prochains jours
    for (int i = 1; i <= daysAhead; i++) {
      LocalDate date = LocalDate.now().plusDays(i);

      // Vérifier : jour configuré + pas en blackout
      int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Java: 1=Lun...7=Dim → 0=Dim...6=Sam
      List<String> daySlots = availability.getTimeSlotsForDay(dayOfWeek);

      if (daySlots == null || daySlots.isEmpty()) {
        continue; // Jour fermé
      }

      if (availability.isDateBlackedOut(date)) {
        continue; // Date fermée
      }

      // Date disponible
      availableDates.add(date);

      // Calculer les créneaux disponibles
      List<AvailableSlot> slots = new ArrayList<>();
      for (String timeSlot : daySlots) {
        long bookings =
            visitRequestRepository.countConfirmedForDateAndSlot(propertyId, date, timeSlot);
        int available = (int) (availability.getMaxVisitsPerSlot() - bookings);

        if (available > 0) {
          slots.add(
              AvailableSlot.builder()
                  .slot(timeSlot)
                  .available(available)
                  .almostFull(available == 1)
                  .build());
        }
      }

      if (!slots.isEmpty()) {
        slotsByDate.put(date, slots);
      }
    }

    return VisitAvailabilityResponse.builder()
        .propertyId(propertyId.toString())
        .availableDates(availableDates)
        .slotsByDate(slotsByDate)
        .build();
  }

  // ===== AGENCY ENDPOINTS =====

  @Override
  public VisitAvailabilityResponse configureAvailability(
      String keycloakId, ConfigureVisitAvailabilityDto request) throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    UUID propertyId = UUID.fromString(request.getPropertyId());
    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Bien immobilier non trouvé"));

    // Vérifier que la propriété appartient à l'agence du user
    if (!property.getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Vous n'avez pas accès à ce bien");
    }

    // Chercher ou créer la configuration
    AgencyVisitAvailability availability =
        availabilityRepository
            .findByPropertyId(propertyId)
            .orElseGet(
                () ->
                    AgencyVisitAvailability.builder()
                        .property(property)
                        .agency(agency)
                        .timeSlotsConfig("{}")
                        .blackoutDates(new LocalDate[0])
                        .maxVisitsPerSlot(3)
                        .isActive(true)
                        .build());

    // Mettre à jour
    availability.setAllTimeSlots(request.getTimeSlots());
    if (request.getBlackoutDates() != null) {
      availability.setBlackoutDates(request.getBlackoutDates().toArray(new LocalDate[0]));
    }
    if (request.getMaxVisitsPerSlot() != null) {
      availability.setMaxVisitsPerSlot(request.getMaxVisitsPerSlot());
    }

    availabilityRepository.save(availability);

    return getAvailableSlotsForProperty(propertyId, 30);
  }

  @Override
  @Transactional(readOnly = true)
  public VisitAvailabilityResponse getConfiguredAvailability(String keycloakId, UUID propertyId)
      throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Bien immobilier non trouvé"));

    if (!property.getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Vous n'avez pas accès à ce bien");
    }

    return getAvailableSlotsForProperty(propertyId, 30);
  }

  @Override
  public void updateBlackoutDates(
      String keycloakId, UUID propertyId, UpdateBlackoutDatesDto request) throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    AgencyVisitAvailability availability =
        availabilityRepository
            .findByPropertyId(propertyId)
            .orElseThrow(() -> new NotFoundException("Configuration de visite non trouvée"));

    if (!availability.getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Vous n'avez pas accès à cette configuration");
    }

    availability.setBlackoutDates(request.getBlackoutDates().toArray(new LocalDate[0]));
    availabilityRepository.save(availability);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<VisitRequestResponse> getVisitRequestsForAgency(String keycloakId, Pageable pageable)
      throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    // Retourner d'abord les PENDING, puis les autres
    return visitRequestRepository
        .findPendingByAgencyId(agency.getId(), pageable)
        .map(VisitRequestMapper::toResponse);
  }

  @Override
  public VisitRequestResponse confirmVisitRequest(
      String keycloakId, UUID visitRequestId, ConfirmVisitRequestDto request)
      throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    PropertyVisitRequest visitRequest =
        visitRequestRepository
            .findById(visitRequestId)
            .orElseThrow(() -> new NotFoundException("Demande de visite non trouvée"));

    // Vérifier que la demande appartient à l'agence
    if (!visitRequest.getProperty().getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Vous n'avez pas accès à cette demande");
    }

    // Vérifier le statut
    if (visitRequest.getStatus() != VisitRequestStatus.PENDING) {
      throw new CustomException("Seules les demandes PENDING peuvent être confirmées");
    }

    // Vérifier que le créneau n'est pas surboké
    long bookings =
        visitRequestRepository.countConfirmedForDateAndSlot(
            visitRequest.getProperty().getId(),
            request.getConfirmedDate(),
            request.getConfirmedTimeSlot());

    AgencyVisitAvailability availability =
        availabilityRepository
            .findByPropertyId(visitRequest.getProperty().getId())
            .orElseThrow(() -> new NotFoundException("Configuration de visite non trouvée"));

    if (bookings >= availability.getMaxVisitsPerSlot()) {
      throw new CustomException("Ce créneau est complètement réservé");
    }

    // Confirmer
    visitRequest.setStatus(VisitRequestStatus.CONFIRMED);
    visitRequest.setConfirmedDate(request.getConfirmedDate());
    visitRequest.setConfirmedTimeSlot(request.getConfirmedTimeSlot());

    PropertyVisitRequest saved = visitRequestRepository.save(visitRequest);

    return VisitRequestMapper.toResponse(saved);
  }

  @Override
  public VisitRequestResponse rejectVisitRequest(
      String keycloakId, UUID visitRequestId, RejectVisitRequestDto request)
      throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    PropertyVisitRequest visitRequest =
        visitRequestRepository
            .findById(visitRequestId)
            .orElseThrow(() -> new NotFoundException("Demande de visite non trouvée"));

    if (!visitRequest.getProperty().getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Vous n'avez pas accès à cette demande");
    }

    if (visitRequest.getStatus() != VisitRequestStatus.PENDING) {
      throw new CustomException("Seules les demandes PENDING peuvent être rejetées");
    }

    visitRequest.setStatus(VisitRequestStatus.REJECTED);
    visitRequest.setRejectionReason(request.getReason());

    PropertyVisitRequest saved = visitRequestRepository.save(visitRequest);

    return VisitRequestMapper.toResponse(saved);
  }

  @Override
  public VisitRequestResponse assignAgent(String keycloakId, UUID visitRequestId, UUID agentId)
      throws CustomException {

    User agencyUser =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

    Agency agency = agencyUser.getAgency();
    if (agency == null) {
      throw new CustomException("Vous n'êtes pas membre d'une agence");
    }

    PropertyVisitRequest visitRequest =
        visitRequestRepository
            .findById(visitRequestId)
            .orElseThrow(() -> new NotFoundException("Demande de visite non trouvée"));

    if (!visitRequest.getProperty().getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Vous n'avez pas accès à cette demande");
    }

    User agent =
        userRepository
            .findById(agentId)
            .orElseThrow(() -> new NotFoundException("Agent non trouvé"));

    if (agent.getAgency() == null || !agent.getAgency().getId().equals(agency.getId())) {
      throw new CustomException("Cet agent ne fait pas partie de votre agence");
    }

    visitRequest.setAgent(agent);

    PropertyVisitRequest saved = visitRequestRepository.save(visitRequest);

    return VisitRequestMapper.toResponse(saved);
  }
}
