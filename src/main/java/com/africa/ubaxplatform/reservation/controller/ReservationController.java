package com.africa.ubaxplatform.reservation.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.reservation.codeList.ReservationStatus;
import com.africa.ubaxplatform.reservation.dto.CancelReservationRequest;
import com.africa.ubaxplatform.reservation.dto.CreateReservationRequest;
import com.africa.ubaxplatform.reservation.dto.ReservationResponse;
import com.africa.ubaxplatform.reservation.service.interfaces.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur de gestion des réservations hôtelières.
 *
 * <p>Endpoints client (rôle {@code CLIENT} requis) :
 *
 * <ul>
 *   <li>{@code POST /v1/reservations} – soumettre une réservation
 *   <li>{@code GET /v1/reservations/mine} – mes réservations
 *   <li>{@code GET /v1/reservations/{id}} – détail
 *   <li>{@code DELETE /v1/reservations/{id}} – annuler (PENDING uniquement)
 * </ul>
 *
 * <p>Endpoints hôtel ({@code PARTNER} requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/reservations/hotel} – réservations de mon hôtel
 *   <li>{@code PATCH /v1/reservations/{id}/confirm} – confirmer
 *   <li>{@code PATCH /v1/reservations/{id}/cancel} – annuler côté hôtel
 *   <li>{@code PATCH /v1/reservations/{id}/complete} – marquer séjour terminé
 *   <li>{@code PATCH /v1/reservations/{id}/no-show} – marquer no-show
 * </ul>
 *
 * <p>Endpoints admin ({@code ADMIN} ou {@code SUPER_ADMIN} requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/reservations} – toutes les réservations
 * </ul>
 */
@RestController
@RequestMapping("/v1/reservations")
@RequiredArgsConstructor
@Tag(
    name = "Reservation",
    description =
        "Gestion des réservations hôtelières. "
            + "Les clients mobiles réservent un bien ; l'hôtel confirme, complète ou annule.")
public class ReservationController {

  private final ReservationService reservationService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Client ────────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Soumettre une réservation",
      description =
          "Le client crée une demande de réservation pour un bien hôtelier publié. "
              + "Le bien doit être en statut **PUBLISHED**.\n\n"
              + "**Disponibilité :** une réservation est acceptée si le nombre de séjours `CONFIRMED`"
              + " se chevauchant est **strictement inférieur** au `unitCount` du bien"
              + " (défaut `1`). Pour un bien avec `unitCount = 5`, jusqu'à 5 réservations simultanées"
              + " sur les mêmes dates sont possibles.\n\n"
              + "La réservation est créée en statut **PENDING**, en attente de confirmation hôtel.\n\n"
              + "**Rôles autorisés :** `CLIENT`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Réservation créée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ReservationResponse.class))),
    @ApiResponse(responseCode = "400", description = "Dates invalides ou bien non disponible"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant — CLIENT requis"),
    @ApiResponse(responseCode = "404", description = "Bien introuvable")
  })
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateReservationRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT);
    ReservationResponse response = reservationService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.RESERVATION_CREATE_SUCCESS,
                response));
  }

  @GetMapping("/mine")
  @Operation(
      summary = "Mes réservations",
      description =
          "Retourne la liste paginée des réservations du client connecté.\n\n"
              + "**Rôles autorisés :** `CLIENT`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant — CLIENT requis")
  })
  public ResponseEntity<CustomResponse> getMyReservations(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT);
    Page<ReservationResponse> page =
        reservationService.getMyReservations(caller.getSub(), pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_GET_LIST_SUCCESS,
            page));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'une réservation",
      description =
          "Retourne le détail d'une réservation. Accessible par le client propriétaire de la "
              + "réservation ou par un partenaire hôtelier dont le bien est concerné.\n\n"
              + "**Rôles autorisés :** `CLIENT`, `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Réservation retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Accès non autorisé"),
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.PARTNER);
    ReservationResponse response = reservationService.getById(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_GET_SUCCESS,
            response));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Annuler ma réservation",
      description =
          "Le client annule sa réservation. Seules les réservations en statut **PENDING** peuvent être annulées.\n\n"
              + "**Rôles autorisés :** `CLIENT`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Réservation annulée avec succès"),
    @ApiResponse(
        responseCode = "400",
        description = "La réservation n'est pas annulable (statut invalide)"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Cette réservation ne vous appartient pas"),
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
  })
  public ResponseEntity<CustomResponse> cancelByClient(
      @PathVariable UUID id,
      @RequestBody @Valid CancelReservationRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT);
    ReservationResponse response = reservationService.cancelByClient(caller.getSub(), id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_UPDATE_SUCCESS,
            response));
  }

  // ── Hôtel ─────────────────────────────────────────────────────────────────

  @GetMapping("/hotel")
  @Operation(
      summary = "Réservations de mon hôtel",
      description =
          "Retourne la liste paginée des réservations pour tous les biens de l'hôtel du partenaire connecté. "
              + "Filtrable par statut.\n\n"
              + "**Rôles autorisés :** `PARTNER` (hôtelier uniquement)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant ou partenaire non hôtelier")
  })
  public ResponseEntity<CustomResponse> getHotelReservations(
      @Parameter(
              description = "Filtre par statut. Omis = tous les statuts.",
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {
                        "PENDING",
                        "CONFIRMED",
                        "CANCELLED",
                        "COMPLETED",
                        "NO_SHOW"
                      }))
          @RequestParam(required = false)
          ReservationStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    Page<ReservationResponse> page =
        reservationService.getHotelReservations(caller.getSub(), status, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_GET_LIST_SUCCESS,
            page));
  }

  @PatchMapping("/{id}/confirm")
  @Operation(
      summary = "Confirmer une réservation",
      description =
          "L'hôtel confirme une réservation en statut **PENDING**. Le statut passe à **CONFIRMED**.\n\n"
              + "**Rôles autorisés :** `PARTNER` (hôtelier)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Réservation confirmée"),
    @ApiResponse(responseCode = "400", description = "Statut invalide pour cette transition"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Non autorisé"),
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
  })
  public ResponseEntity<CustomResponse> confirm(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    ReservationResponse response = reservationService.confirm(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/cancel")
  @Operation(
      summary = "Annuler une réservation (hôtel)",
      description =
          "L'hôtel annule une réservation **PENDING** ou **CONFIRMED**. Le motif est obligatoire.\n\n"
              + "**Rôles autorisés :** `PARTNER` (hôtelier)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Réservation annulée"),
    @ApiResponse(responseCode = "400", description = "Statut invalide pour cette transition"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Non autorisé"),
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
  })
  public ResponseEntity<CustomResponse> cancelByHotel(
      @PathVariable UUID id,
      @RequestBody @Valid CancelReservationRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    ReservationResponse response = reservationService.cancelByHotel(caller.getSub(), id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/complete")
  @Operation(
      summary = "Marquer le séjour comme terminé",
      description =
          "L'hôtel marque une réservation **CONFIRMED** comme **COMPLETED** après la fin du séjour.\n\n"
              + "**Rôles autorisés :** `PARTNER` (hôtelier)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Séjour marqué terminé"),
    @ApiResponse(responseCode = "400", description = "Statut invalide"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Non autorisé"),
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
  })
  public ResponseEntity<CustomResponse> complete(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    ReservationResponse response = reservationService.complete(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/no-show")
  @Operation(
      summary = "Marquer le client comme no-show",
      description =
          "L'hôtel signale que le client ne s'est pas présenté pour une réservation **CONFIRMED**.\n\n"
              + "**Rôles autorisés :** `PARTNER` (hôtelier)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "No-show enregistré"),
    @ApiResponse(responseCode = "400", description = "Statut invalide"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Non autorisé"),
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
  })
  public ResponseEntity<CustomResponse> noShow(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    ReservationResponse response = reservationService.noShow(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_UPDATE_SUCCESS,
            response));
  }

  // ── Admin ─────────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Toutes les réservations (admin)",
      description =
          "Retourne toutes les réservations de la plateforme, filtrables par statut. "
              + "Pagination par défaut : 20 éléments triés par `createdAt` décroissant.\n\n"
              + "**Rôles autorisés :** `ADMIN`, `SUPER_ADMIN`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — ADMIN ou SUPER_ADMIN requis")
  })
  public ResponseEntity<CustomResponse> listAll(
      @Parameter(
              description = "Filtre par statut. Omis = tous les statuts.",
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {
                        "PENDING",
                        "CONFIRMED",
                        "CANCELLED",
                        "COMPLETED",
                        "NO_SHOW"
                      }))
          @RequestParam(required = false)
          ReservationStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    Page<ReservationResponse> page = reservationService.listAll(status, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.RESERVATION_GET_LIST_SUCCESS,
            page));
  }
}
