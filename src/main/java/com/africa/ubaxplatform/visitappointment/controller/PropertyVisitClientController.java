package com.africa.ubaxplatform.visitappointment.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.visitappointment.dto.CreateVisitRequestDto;
import com.africa.ubaxplatform.visitappointment.dto.VisitAvailabilityResponse;
import com.africa.ubaxplatform.visitappointment.dto.VisitRequestResponse;
import com.africa.ubaxplatform.visitappointment.service.interfaces.PropertyVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour les clients demandant une visite immobilière.
 *
 * <p>Endpoints publics et authentifiés pour la réservation de visites de biens.
 */
@RestController
@RequestMapping("/v1/property-visits")
@RequiredArgsConstructor
@Tag(
    name = "Property Visits - Client",
    description =
        "Gestion des demandes de visite côté client. "
            + "Les clients peuvent voir les créneaux disponibles et demander une visite.")
public class PropertyVisitClientController {

  private final PropertyVisitService visitService;
  private final RequestHeaderParser requestHeaderParser;

  /** Récupère les créneaux disponibles pour un bien (PUBLIC - pas de JWT requis). */
  @GetMapping("/available-slots/{propertyId}")
  @Operation(
      summary = "Obtenir les créneaux disponibles pour un bien",
      description =
          "Retourne les dates et créneaux disponibles pour les visites "
              + "(prochains 30 jours par défaut)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Créneaux récupérés avec succès"),
    @ApiResponse(responseCode = "404", description = "Bien non trouvé")
  })
  public ResponseEntity<CustomResponse> getAvailableSlots(
      @Parameter(description = "ID du bien") @PathVariable("propertyId") UUID propertyId,
      @Parameter(description = "Nombre de jours à scanner (défaut: 30)")
          @RequestParam(defaultValue = "30")
          int daysAhead)
      throws CustomException {

    VisitAvailabilityResponse response =
        visitService.getAvailableSlotsForProperty(propertyId, daysAhead);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Créneaux disponibles récupérés",
            response));
  }

  /** Crée une nouvelle demande de visite. */
  @PostMapping
  @Operation(summary = "Demander une visite", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Demande créée avec succès"),
    @ApiResponse(responseCode = "400", description = "Erreur de validation ou métier"),
    @ApiResponse(responseCode = "404", description = "Bien ou client non trouvé")
  })
  public ResponseEntity<CustomResponse> createVisitRequest(
      @RequestBody @Valid CreateVisitRequestDto request, HttpServletRequest httpRequest)
      throws CustomException {

    var caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);

    VisitRequestResponse response = visitService.createVisitRequest(caller.getSub(), request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                "Demande de visite créée avec succès",
                response));
  }

  /** Récupère mes demandes de visite. */
  @GetMapping("/mine")
  @Operation(
      summary = "Mes demandes de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Demandes récupérées"),
    @ApiResponse(responseCode = "401", description = "Non authentifié")
  })
  public ResponseEntity<CustomResponse> getMyRequests(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);

    Page<VisitRequestResponse> response =
        visitService.getMyVisitRequests(caller.getSub(), pageable);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Mes demandes de visite",
            response));
  }

  /** Récupère le détail d'une demande de visite. */
  @GetMapping("/{visitRequestId}")
  @Operation(
      summary = "Détail d'une demande de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Demande récupérée"),
    @ApiResponse(responseCode = "403", description = "Accès refusé"),
    @ApiResponse(responseCode = "404", description = "Demande non trouvée")
  })
  public ResponseEntity<CustomResponse> getDetail(
      @Parameter(description = "ID de la demande") @PathVariable("visitRequestId")
          UUID visitRequestId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAuthenticated(requestHeaderParser, httpRequest);

    VisitRequestResponse response =
        visitService.getVisitRequestDetail(caller.getSub(), visitRequestId);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Demande de visite récupérée",
            response));
  }

  /** Annule une demande de visite (PENDING uniquement). */
  @DeleteMapping("/{visitRequestId}")
  @Operation(
      summary = "Annuler une demande de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Demande annulée"),
    @ApiResponse(responseCode = "400", description = "Erreur métier"),
    @ApiResponse(responseCode = "404", description = "Demande non trouvée")
  })
  public ResponseEntity<Void> cancelRequest(
      @Parameter(description = "ID de la demande") @PathVariable("visitRequestId")
          UUID visitRequestId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);

    visitService.cancelVisitRequest(caller.getSub(), visitRequestId);

    return ResponseEntity.noContent().build();
  }
}
