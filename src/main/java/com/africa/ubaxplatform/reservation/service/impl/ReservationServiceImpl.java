package com.africa.ubaxplatform.reservation.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import com.africa.ubaxplatform.reservation.dto.CancelReservationRequest;
import com.africa.ubaxplatform.reservation.dto.CreateReservationRequest;
import com.africa.ubaxplatform.reservation.dto.ReservationResponse;
import com.africa.ubaxplatform.reservation.entity.Reservation;
import com.africa.ubaxplatform.reservation.mapper.ReservationMapper;
import com.africa.ubaxplatform.reservation.repository.ReservationRepository;
import com.africa.ubaxplatform.reservation.service.interfaces.ReservationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

  private final ReservationRepository reservationRepo;
  private final UserRepository userRepo;
  private final PropertyRepository propertyRepo;

  // ── Helpers ───────────────────────────────────────────────────────────────

  private User resolveUser(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakIdWithHotel(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private Reservation resolveReservation(UUID id) throws CustomException {
    return reservationRepo
        .findByIdWithDetails(id)
        .filter(r -> r.getDeletedAt() == null)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Réservation introuvable"),
                    ResponseMessageConstants.RESERVATION_GET_FAILURE_NOT_FOUND));
  }

  private void requireHotelOwnership(User caller, Reservation reservation) throws CustomException {
    if (caller.getHotel() == null) {
      throw new CustomException(
          new UnAuthorizedException("Vous n'êtes pas un partenaire hôtelier"),
          ResponseMessageConstants.USER_UNAUTHORIZED);
    }
    UUID hotelId = caller.getHotel().getId();

    // Logique OR identique à ReservationRepository.findByHotelId :
    //   - chemin direct : property.hotel_id = hotelId
    //   - chemin owner  : property.owner.hotel_id = hotelId  (anciens biens pré-V044)
    // Les deux LEFT JOIN FETCH dans findByIdWithDetails garantissent que les deux sont chargés.
    var property = reservation.getProperty();
    boolean directMatch =
        property.getHotel() != null && hotelId.equals(property.getHotel().getId());
    boolean ownerMatch =
        property.getOwner() != null
            && property.getOwner().getHotel() != null
            && hotelId.equals(property.getOwner().getHotel().getId());

    if (!directMatch && !ownerMatch) {
      throw new CustomException(
          new UnAuthorizedException("Cette réservation n'appartient pas à votre hôtel"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  // ── Client ────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public ReservationResponse create(String keycloakId, CreateReservationRequest request)
      throws CustomException {

    if (!request.checkOutDate().isAfter(request.checkInDate())) {
      throw new CustomException(
          new BadRequestException("La date de départ doit être après la date d'arrivée"),
          ResponseMessageConstants.RESERVATION_CREATE_FAILURE_BAD_REQUEST);
    }

    User client = resolveUser(keycloakId);

    Property property =
        propertyRepo
            .findById(request.propertyId())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Bien introuvable"),
                        ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND));

    if (property.getStatus() != PropertyStatus.PUBLISHED) {
      throw new CustomException(
          new BadRequestException("Ce bien n'est pas disponible à la réservation"),
          ResponseMessageConstants.RESERVATION_CREATE_FAILURE_BAD_REQUEST);
    }

    long overlaps =
        reservationRepo.countOverlappingConfirmed(
            property.getId(), request.checkInDate(), request.checkOutDate());
    int capacity = property.getUnitCount() != null ? property.getUnitCount() : 1;
    if (overlaps >= capacity) {
      throw new CustomException(
          new BadRequestException(
              capacity == 1
                  ? "Le bien est déjà réservé pour ces dates"
                  : "Toutes les unités disponibles sont déjà réservées pour ces dates ("
                      + overlaps
                      + "/"
                      + capacity
                      + ")"),
          ResponseMessageConstants.RESERVATION_CREATE_FAILURE_UNAVAILABLE);
    }

    if (property.getMaxOccupancy() != null && request.guestCount() > property.getMaxOccupancy()) {
      throw new CustomException(
          new BadRequestException(
              "Capacité maximale du bien dépassée : "
                  + property.getMaxOccupancy()
                  + " personne(s) autorisée(s), "
                  + request.guestCount()
                  + " demandée(s)"),
          ResponseMessageConstants.RESERVATION_CREATE_FAILURE_BAD_REQUEST);
    }

    int nights = (int) ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
    BigDecimal unitPrice = property.getPrice();

    // Calcul du montant total selon la fréquence de facturation du bien
    BigDecimal total =
        switch (property.getPaymentFrequency() != null
            ? property.getPaymentFrequency()
            : "NIGHTLY") {
          case "WEEKLY" ->
              // Prix à la semaine : proratisé (ex: 10 nuits = 10/7 semaines)
              unitPrice
                  .multiply(BigDecimal.valueOf(nights))
                  .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
          case "MONTHLY" ->
              // Prix au mois : proratisé sur 30 jours (ex: 45 nuits = 45/30 mois)
              unitPrice
                  .multiply(BigDecimal.valueOf(nights))
                  .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
          default ->
              // NIGHTLY : prix × nombre de nuits
              unitPrice.multiply(BigDecimal.valueOf(nights));
        };

    Reservation reservation =
        Reservation.builder()
            .property(property)
            .client(client)
            .checkInDate(request.checkInDate())
            .checkOutDate(request.checkOutDate())
            .numberOfNights(nights)
            .guestCount(request.guestCount())
            .pricePerNight(unitPrice)
            .totalAmount(total)
            .status(ReservationStatus.PENDING)
            .notes(request.notes())
            .build();

    return ReservationMapper.toResponse(reservationRepo.save(reservation));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReservationResponse> getMyReservations(String keycloakId, Pageable pageable)
      throws CustomException {
    User client = resolveUser(keycloakId);
    return reservationRepo
        .findByClientIdAndDeletedAtIsNull(client.getId(), pageable)
        .map(ReservationMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ReservationResponse getById(String keycloakId, UUID id) throws CustomException {
    User caller = resolveUser(keycloakId);
    Reservation reservation = resolveReservation(id);

    boolean isClient = reservation.getClient().getId().equals(caller.getId());
    var propertyHotel = reservation.getProperty().getOwner().getHotel(); // ← corrigé

    boolean isHotelOwner =
        caller.getHotel() != null
            && propertyHotel != null
            && caller.getHotel().getId().equals(propertyHotel.getId());

    if (!isClient && !isHotelOwner) {
      throw new CustomException(
          new UnAuthorizedException("Accès non autorisé à cette réservation"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }

    return ReservationMapper.toResponse(reservation);
  }

  @Override
  @Transactional
  public ReservationResponse cancelByClient(
      String keycloakId, UUID id, CancelReservationRequest request) throws CustomException {
    User client = resolveUser(keycloakId);
    Reservation reservation = resolveReservation(id);

    if (!reservation.getClient().getId().equals(client.getId())) {
      throw new CustomException(
          new UnAuthorizedException("Cette réservation ne vous appartient pas"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
    if (reservation.getStatus() != ReservationStatus.PENDING) {
      throw new CustomException(
          new BadRequestException(
              "Seules les réservations PENDING peuvent être annulées par le client"),
          ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }

    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setCancellationReason(request.reason());
    reservation.setCancelledBy(client);
    reservation.setCancelledAt(LocalDateTime.now());

    return ReservationMapper.toResponse(reservationRepo.save(reservation));
  }

  // ── Hôtel ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<ReservationResponse> getHotelReservations(
      String keycloakId, ReservationStatus status, Pageable pageable) throws CustomException {
    User caller = resolveUser(keycloakId);
    if (caller.getHotel() == null) {
      throw new CustomException(
          new UnAuthorizedException("Vous n'êtes pas un partenaire hôtelier"),
          ResponseMessageConstants.USER_UNAUTHORIZED);
    }
    UUID hotelId = caller.getHotel().getId();
    if (status != null) {
      return reservationRepo
          .findByHotelIdAndStatus(hotelId, status, pageable)
          .map(ReservationMapper::toResponse);
    }
    return reservationRepo.findByHotelId(hotelId, pageable).map(ReservationMapper::toResponse);
  }

  @Override
  @Transactional
  public ReservationResponse confirm(String keycloakId, UUID id) throws CustomException {
    User caller = resolveUser(keycloakId);
    Reservation reservation = resolveReservation(id);
    requireHotelOwnership(caller, reservation);

    if (reservation.getStatus() != ReservationStatus.PENDING) {
      throw new CustomException(
          new BadRequestException("Seules les réservations PENDING peuvent être confirmées"),
          ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }

    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservation.setConfirmedAt(LocalDateTime.now());
    return ReservationMapper.toResponse(reservationRepo.save(reservation));
  }

  @Override
  @Transactional
  public ReservationResponse cancelByHotel(
      String keycloakId, UUID id, CancelReservationRequest request) throws CustomException {
    User caller = resolveUser(keycloakId);
    Reservation reservation = resolveReservation(id);
    requireHotelOwnership(caller, reservation);

    if (reservation.getStatus() != ReservationStatus.PENDING
        && reservation.getStatus() != ReservationStatus.CONFIRMED) {
      throw new CustomException(
          new BadRequestException(
              "Seules les réservations PENDING ou CONFIRMED peuvent être annulées"),
          ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }

    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setCancellationReason(request.reason());
    reservation.setCancelledBy(caller);
    reservation.setCancelledAt(LocalDateTime.now());
    return ReservationMapper.toResponse(reservationRepo.save(reservation));
  }

  @Override
  @Transactional
  public ReservationResponse complete(String keycloakId, UUID id) throws CustomException {
    User caller = resolveUser(keycloakId);
    Reservation reservation = resolveReservation(id);
    requireHotelOwnership(caller, reservation);

    if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
      throw new CustomException(
          new BadRequestException("Seules les réservations CONFIRMED peuvent être complétées"),
          ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }

    reservation.setStatus(ReservationStatus.COMPLETED);
    reservation.setCompletedAt(LocalDateTime.now());
    return ReservationMapper.toResponse(reservationRepo.save(reservation));
  }

  @Override
  @Transactional
  public ReservationResponse noShow(String keycloakId, UUID id) throws CustomException {
    User caller = resolveUser(keycloakId);
    Reservation reservation = resolveReservation(id);
    requireHotelOwnership(caller, reservation);

    if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
      throw new CustomException(
          new BadRequestException(
              "Seules les réservations CONFIRMED peuvent être marquées NO_SHOW"),
          ResponseMessageConstants.RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION);
    }

    reservation.setStatus(ReservationStatus.NO_SHOW);
    return ReservationMapper.toResponse(reservationRepo.save(reservation));
  }

  // ── Admin ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public Page<ReservationResponse> listAll(ReservationStatus status, Pageable pageable) {
    if (status != null) {
      return reservationRepo
          .findByStatusAndDeletedAtIsNull(status, pageable)
          .map(ReservationMapper::toResponse);
    }
    return reservationRepo.findByDeletedAtIsNull(pageable).map(ReservationMapper::toResponse);
  }
}
