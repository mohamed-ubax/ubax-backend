package com.africa.ubaxplatform.visitappointment.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.visitappointment.dto.ConfigureVisitAvailabilityDto;
import com.africa.ubaxplatform.visitappointment.dto.ConfirmVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.RejectVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.UpdateBlackoutDatesDto;
import com.africa.ubaxplatform.visitappointment.dto.VisitAvailabilityResponse;
import com.africa.ubaxplatform.visitappointment.dto.VisitRequestResponse;
import com.africa.ubaxplatform.visitappointment.service.interfaces.PropertyVisitService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour les agences gérant les demandes de visite.
 *
 * <p>Endpoints authentifiés réservés aux partenaires (agences) pour configurer les créneaux et
 * traiter les demandes de visite.
 *
 * <ul>
 *   <li>{@code POST /v1/agency/property-visits/config} – configurer les créneaux d'un bien
 *   <li>{@code GET /v1/agency/property-visits/config/{propertyId}} – récupérer la configuration
 *   <li>{@code PUT /v1/agency/property-visits/config/{propertyId}/blackout-dates} – dates fermées
 *   <li>{@code GET /v1/agency/property-visits} – liste des demandes reçues
 *   <li>{@code PATCH /v1/agency/property-visits/{id}/confirm} – confirmer
 *   <li>{@code PATCH /v1/agency/property-visits/{id}/reject} – rejeter
 *   <li>{@code PATCH /v1/agency/property-visits/{id}/assign-agent/{agentId}} – assigner un agent
 * </ul>
 */
@RestController
@RequestMapping("/v1/agency/property-visits")
@RequiredArgsConstructor
@Tag(
    name = "Property Visits - Agency",
    description =
        "Gestion des demandes de visite côté agence. "
            + "L'agence configure les créneaux de visite par bien (jours ouvrables, horaires, capacité max) "
            + "et traite les demandes clients (confirmation, rejet, assignation d'agent). "
            + "Workflow : PENDING → CONFIRMED (agence confirme la date/créneau) ou REJECTED. "
            + "Seuls les biens appartenant à l'agence du PARTNER connecté sont accessibles.")
public class PropertyVisitAgencyController {

  private final PropertyVisitService visitService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Configuration ─────────────────────────────────────────────────────────

  @PostMapping("/config")
  @Operation(
      summary = "Configurer les créneaux de visite d'un bien",
      description =
          "Crée ou remplace la configuration de disponibilités de visite pour un bien de l'agence. "
              + "Les créneaux sont définis par jour de semaine (0=Dimanche … 6=Samedi) avec des plages horaires. "
              + "Un jour absent de `timeSlots` est considéré fermé. "
              + "`maxVisitsPerSlot` (défaut 3) limite le nombre de demandes simultanées par créneau. "
              + "L'appel est idempotent : si une configuration existe déjà pour ce bien, elle est remplacée.\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Configuration créée ou mise à jour avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitAvailabilityResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Données de configuration invalides (ex. créneau mal formaté)"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant — PARTNER requis, ou bien n'appartient pas à votre agence"),
    @ApiResponse(responseCode = "404", description = "Bien introuvable")
  })
  public ResponseEntity<CustomResponse> configureAvailability(
      @RequestBody @Valid ConfigureVisitAvailabilityDto request, HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitAvailabilityResponse response =
        visitService.configureAvailability(caller.getSub(), request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.VISIT_AVAILABILITY_CONFIGURE_SUCCESS,
                response));
  }

  @GetMapping("/config/{propertyId}")
  @Operation(
      summary = "Récupérer la configuration de disponibilité d'un bien",
      description =
          "Retourne la configuration de créneaux de visite actuellement en place pour un bien "
              + "de l'agence (jours ouvrables, créneaux horaires, dates fermées, capacité max).\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Configuration retournée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitAvailabilityResponse.class))),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou bien n'appartient pas à votre agence"),
    @ApiResponse(
        responseCode = "404",
        description = "Bien introuvable ou aucune configuration définie pour ce bien")
  })
  public ResponseEntity<CustomResponse> getConfiguration(
      @Parameter(description = "ID du bien immobilier") @PathVariable("propertyId") UUID propertyId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitAvailabilityResponse response =
        visitService.getConfiguredAvailability(caller.getSub(), propertyId);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_AVAILABILITY_GET_SUCCESS,
            response));
  }

  @PutMapping("/config/{propertyId}/blackout-dates")
  @Operation(
      summary = "Mettre à jour les dates d'indisponibilité (blackout)",
      description =
          "Remplace la liste des dates fermées pour un bien (vacances, congés, fermetures exceptionnelles). "
              + "Les dates existantes sont intégralement remplacées par celles fournies dans le body. "
              + "Envoyer un tableau vide `[]` pour supprimer toutes les dates fermées.\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dates d'indisponibilité mises à jour"),
    @ApiResponse(responseCode = "400", description = "Format de date invalide"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou bien n'appartient pas à votre agence"),
    @ApiResponse(
        responseCode = "404",
        description = "Bien introuvable ou aucune configuration définie pour ce bien")
  })
  public ResponseEntity<CustomResponse> updateBlackoutDates(
      @Parameter(description = "ID du bien immobilier") @PathVariable("propertyId") UUID propertyId,
      @RequestBody @Valid UpdateBlackoutDatesDto request,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    visitService.updateBlackoutDates(caller.getSub(), propertyId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_AVAILABILITY_UPDATE_SUCCESS,
            null));
  }

  // ── Demandes ──────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Demandes de visite de mon agence",
      description =
          "Retourne la liste paginée des demandes de visite reçues pour tous les biens "
              + "de l'agence du PARTNER connecté. "
              + "Triées par date de création ascendante (les plus anciennes en priorité). "
              + "Pagination par défaut : 20 éléments.\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant — PARTNER requis")
  })
  public ResponseEntity<CustomResponse> getRequests(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    Page<VisitRequestResponse> response =
        visitService.getVisitRequestsForAgency(caller.getSub(), pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_REQUEST_GET_LIST_SUCCESS,
            response));
  }

  @PatchMapping("/{visitRequestId}/confirm")
  @Operation(
      summary = "Confirmer une demande de visite",
      description =
          "L'agence confirme une demande en statut **PENDING**. "
              + "Le statut passe à **CONFIRMED**. "
              + "L'agence peut confirmer à la date/créneau demandés ou proposer une alternative "
              + "(les champs `confirmedDate` et `confirmedTimeSlot` peuvent différer de la demande). "
              + "Le backend vérifie que le créneau confirmé n'est pas déjà complet.\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Demande confirmée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitRequestResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description =
            "Erreur métier — créneau complet, statut invalide (doit être PENDING), ou données invalides"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou demande n'appartient pas à votre agence"),
    @ApiResponse(responseCode = "404", description = "Demande introuvable")
  })
  public ResponseEntity<CustomResponse> confirmRequest(
      @Parameter(description = "ID de la demande de visite") @PathVariable("visitRequestId")
          UUID visitRequestId,
      @RequestBody @Valid ConfirmVisitRequestDto request,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitRequestResponse response =
        visitService.confirmVisitRequest(caller.getSub(), visitRequestId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_REQUEST_CONFIRM_SUCCESS,
            response));
  }

  @PatchMapping("/{visitRequestId}/reject")
  @Operation(
      summary = "Rejeter une demande de visite",
      description =
          "L'agence rejette une demande en statut **PENDING**. "
              + "Le statut passe à **REJECTED**. "
              + "Un motif de rejet (`reason`) est obligatoire. "
              + "Le créneau occupancy est automatiquement libéré.\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Demande rejetée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitRequestResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Statut invalide — la demande doit être en statut PENDING"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou demande n'appartient pas à votre agence"),
    @ApiResponse(responseCode = "404", description = "Demande introuvable")
  })
  public ResponseEntity<CustomResponse> rejectRequest(
      @Parameter(description = "ID de la demande de visite") @PathVariable("visitRequestId")
          UUID visitRequestId,
      @RequestBody @Valid RejectVisitRequestDto request,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitRequestResponse response =
        visitService.rejectVisitRequest(caller.getSub(), visitRequestId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_REQUEST_REJECT_SUCCESS,
            response));
  }

  @PatchMapping("/{visitRequestId}/assign-agent/{agentId}")
  @Operation(
      summary = "Assigner un agent immobilier à une visite",
      description =
          "Assigne un membre de l'agence (agent immobilier) à une demande de visite confirmée ou en attente. "
              + "`agentId` est l'UUID **base de données** de l'agent (`user.id`), "
              + "récupérable via `GET /v1/agency/team`. "
              + "L'assignation ne change pas le statut de la demande.\n\n"
              + "**Rôles autorisés :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Agent assigné avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitRequestResponse.class))),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou demande n'appartient pas à votre agence"),
    @ApiResponse(
        responseCode = "404",
        description = "Demande introuvable ou agent introuvable dans l'équipe de l'agence")
  })
  public ResponseEntity<CustomResponse> assignAgent(
      @Parameter(description = "ID de la demande de visite") @PathVariable("visitRequestId")
          UUID visitRequestId,
      @Parameter(description = "UUID base de données de l'agent (user.id — pas le Keycloak ID)")
          @PathVariable("agentId")
          UUID agentId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitRequestResponse response =
        visitService.assignAgent(caller.getSub(), visitRequestId, agentId);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_REQUEST_ASSIGN_SUCCESS,
            response));
  }
}
