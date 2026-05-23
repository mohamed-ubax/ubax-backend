package com.africa.ubaxplatform.visitappointment.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
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
 */
@RestController
@RequestMapping("/v1/agency/property-visits")
@RequiredArgsConstructor
@Tag(
    name = "Property Visits - Agency",
    description =
        "Gestion des demandes de visite côté agence. "
            + "Les agences configurent les créneaux disponibles et traitent les demandes.")
public class PropertyVisitAgencyController {

  private final PropertyVisitService visitService;
  private final RequestHeaderParser requestHeaderParser;

  /** Configure les créneaux disponibles pour un bien. */
  @PostMapping("/config")
  @Operation(
      summary = "Configurer les créneaux disponibles d'un bien",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Configuration créée/mise à jour"),
    @ApiResponse(responseCode = "400", description = "Erreur validation"),
    @ApiResponse(responseCode = "403", description = "Bien n'appartient pas à votre agence"),
    @ApiResponse(responseCode = "404", description = "Bien non trouvé")
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
                "Créneaux configurés avec succès",
                response));
  }

  /** Récupère la configuration actuellement en place pour un bien. */
  @GetMapping("/config/{propertyId}")
  @Operation(
      summary = "Récupérer la configuration d'un bien",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Configuration récupérée"),
    @ApiResponse(responseCode = "403", description = "Bien n'appartient pas à votre agence"),
    @ApiResponse(responseCode = "404", description = "Bien ou configuration non trouvé")
  })
  public ResponseEntity<CustomResponse> getConfiguration(
      @Parameter(description = "ID du bien") @PathVariable("propertyId") UUID propertyId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitAvailabilityResponse response =
        visitService.getConfiguredAvailability(caller.getSub(), propertyId);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Configuration récupérée",
            response));
  }

  /** Met à jour les dates fermées (blackout) pour un bien. */
  @PutMapping("/config/{propertyId}/blackout-dates")
  @Operation(
      summary = "Mettre à jour les dates fermées",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dates mises à jour"),
    @ApiResponse(responseCode = "404", description = "Bien ou configuration non trouvé")
  })
  public ResponseEntity<CustomResponse> updateBlackoutDates(
      @Parameter(description = "ID du bien") @PathVariable("propertyId") UUID propertyId,
      @RequestBody @Valid UpdateBlackoutDatesDto request,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    visitService.updateBlackoutDates(caller.getSub(), propertyId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Dates fermées mises à jour",
            null));
  }

  /** Récupère les demandes de visite de l'agence (PENDING en priorité). */
  @GetMapping
  @Operation(
      summary = "Récupérer mes demandes de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Demandes récupérées"),
    @ApiResponse(responseCode = "401", description = "Non authentifié")
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
            "Demandes de visite récupérées",
            response));
  }

  /** Confirme une demande de visite. */
  @PatchMapping("/{visitRequestId}/confirm")
  @Operation(
      summary = "Confirmer une demande de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Demande confirmée"),
    @ApiResponse(
        responseCode = "400",
        description = "Erreur métier (créneau plein, statut invalide)"),
    @ApiResponse(responseCode = "404", description = "Demande non trouvée")
  })
  public ResponseEntity<CustomResponse> confirmRequest(
      @Parameter(description = "ID de la demande") @PathVariable("visitRequestId")
          UUID visitRequestId,
      @RequestBody @Valid ConfirmVisitRequestDto request,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitRequestResponse response =
        visitService.confirmVisitRequest(caller.getSub(), visitRequestId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY, Constants.Status.OK, "Demande confirmée", response));
  }

  /** Rejette une demande de visite. */
  @PatchMapping("/{visitRequestId}/reject")
  @Operation(
      summary = "Rejeter une demande de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Demande rejetée"),
    @ApiResponse(responseCode = "400", description = "Erreur métier (statut invalide)"),
    @ApiResponse(responseCode = "404", description = "Demande non trouvée")
  })
  public ResponseEntity<CustomResponse> rejectRequest(
      @Parameter(description = "ID de la demande") @PathVariable("visitRequestId")
          UUID visitRequestId,
      @RequestBody @Valid RejectVisitRequestDto request,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitRequestResponse response =
        visitService.rejectVisitRequest(caller.getSub(), visitRequestId, request);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY, Constants.Status.OK, "Demande rejetée", response));
  }

  /** Assigne un agent à une demande de visite. */
  @PatchMapping("/{visitRequestId}/assign-agent/{agentId}")
  @Operation(
      summary = "Assigner un agent à une demande de visite",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Agent assigné"),
    @ApiResponse(responseCode = "404", description = "Demande ou agent non trouvé")
  })
  public ResponseEntity<CustomResponse> assignAgent(
      @Parameter(description = "ID de la demande") @PathVariable("visitRequestId")
          UUID visitRequestId,
      @Parameter(description = "ID de l'agent") @PathVariable("agentId") UUID agentId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);

    VisitRequestResponse response =
        visitService.assignAgent(caller.getSub(), visitRequestId, agentId);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY, Constants.Status.OK, "Agent assigné", response));
  }
}
