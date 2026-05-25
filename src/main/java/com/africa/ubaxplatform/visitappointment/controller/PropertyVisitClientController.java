package com.africa.ubaxplatform.visitappointment.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
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
 *
 * <ul>
 *   <li>{@code GET /v1/property-visits/available-slots/{propertyId}} – créneaux disponibles
 *       (public)
 *   <li>{@code POST /v1/property-visits} – soumettre une demande de visite
 *   <li>{@code GET /v1/property-visits/mine} – mes demandes
 *   <li>{@code GET /v1/property-visits/{visitRequestId}} – détail
 *   <li>{@code DELETE /v1/property-visits/{visitRequestId}} – annuler (PENDING uniquement)
 * </ul>
 */
@RestController
@RequestMapping("/v1/property-visits")
@RequiredArgsConstructor
@Tag(
    name = "Property Visits - Client",
    description =
        "Gestion des demandes de visite côté client. "
            + "Les clients consultent les créneaux disponibles et soumettent une demande pour un bien immobilier. "
            + "Workflow : **PENDING** → **CONFIRMED** / **REJECTED** | **CANCELLED** → **COMPLETED**. "
            + "Un créneau confirmé est bloqué pour les autres demandeurs (occupancy contrôlée).")
public class PropertyVisitClientController {

  private final PropertyVisitService visitService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Public ────────────────────────────────────────────────────────────────

  @GetMapping("/available-slots/{propertyId}")
  @Operation(
      summary = "Créneaux disponibles pour un bien",
      description =
          "Retourne les dates et créneaux disponibles pour les visites d'un bien donné "
              + "(prochains 30 jours par défaut). "
              + "Les créneaux déjà complets (`available = 0`) ne sont pas retournés. "
              + "La réponse inclut pour chaque date la liste des créneaux avec le nombre "
              + "de places restantes et l'indicateur `almostFull` (1 seule place restante).\n\n"
              + "**Accès public** — aucun JWT requis.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Créneaux récupérés avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitAvailabilityResponse.class))),
    @ApiResponse(responseCode = "404", description = "Bien introuvable")
  })
  public ResponseEntity<CustomResponse> getAvailableSlots(
      @Parameter(description = "ID du bien immobilier") @PathVariable("propertyId") UUID propertyId,
      @Parameter(description = "Nombre de jours à scanner à partir d'aujourd'hui (défaut : 30)")
          @RequestParam(defaultValue = "30")
          int daysAhead)
      throws CustomException {

    VisitAvailabilityResponse response =
        visitService.getAvailableSlotsForProperty(propertyId, daysAhead);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.VISIT_AVAILABILITY_GET_SUCCESS,
            response));
  }

  // ── Client ────────────────────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Soumettre une demande de visite",
      description =
          "Le client soumet une demande de visite pour un bien en statut **PUBLISHED**. "
              + "La demande est créée en statut **PENDING**, en attente de confirmation par l'agence. "
              + "La date et le créneau demandés doivent figurer dans les créneaux disponibles "
              + "(vérifiables via `GET /v1/property-visits/available-slots/{propertyId}`). "
              + "Le backend vérifie que le créneau n'est pas déjà complet (`maxVisitsPerSlot`).\n\n"
              + "**Rôles autorisés :** `CLIENT`, `OWNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Demande de visite créée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitRequestResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description =
            "Date ou créneau indisponible, bien non publié, ou données de validation invalides"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant — CLIENT ou OWNER requis"),
    @ApiResponse(responseCode = "404", description = "Bien ou utilisateur introuvable")
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
                ResponseMessageConstants.VISIT_REQUEST_CREATE_SUCCESS,
                response));
  }

  @GetMapping("/mine")
  @Operation(
      summary = "Mes demandes de visite",
      description =
          "Retourne la liste paginée des demandes de visite soumises par le client connecté, "
              + "triées par date de création décroissante. "
              + "Pagination par défaut : 20 éléments.\n\n"
              + "**Rôles autorisés :** `CLIENT`, `OWNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant — CLIENT ou OWNER requis")
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
            ResponseMessageConstants.VISIT_REQUEST_GET_LIST_SUCCESS,
            response));
  }

  @GetMapping("/{visitRequestId}")
  @Operation(
      summary = "Détail d'une demande de visite",
      description =
          "Retourne le détail complet d'une demande de visite. "
              + "Un CLIENT/OWNER ne peut consulter que ses propres demandes — 403 sinon. "
              + "Un PARTNER peut consulter les demandes de son agence via "
              + "`GET /v1/agency/property-visits`.\n\n"
              + "**Rôles autorisés :** `CLIENT`, `OWNER`, `PARTNER`, `ADMIN`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Demande retournée avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitRequestResponse.class))),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(
        responseCode = "403",
        description = "Accès non autorisé — demande appartenant à un autre utilisateur"),
    @ApiResponse(responseCode = "404", description = "Demande introuvable")
  })
  public ResponseEntity<CustomResponse> getDetail(
      @Parameter(description = "ID de la demande de visite") @PathVariable("visitRequestId")
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
            ResponseMessageConstants.VISIT_REQUEST_GET_SUCCESS,
            response));
  }

  @DeleteMapping("/{visitRequestId}")
  @Operation(
      summary = "Annuler une demande de visite",
      description =
          "Le client annule sa demande de visite. "
              + "Seules les demandes en statut **PENDING** sont annulables — 400 sinon. "
              + "L'annulation libère automatiquement le créneau (occupancy décrémentée).\n\n"
              + "**Rôles autorisés :** `CLIENT`, `OWNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Demande annulée avec succès"),
    @ApiResponse(
        responseCode = "400",
        description = "La demande n'est pas annulable — statut doit être PENDING"),
    @ApiResponse(responseCode = "401", description = "Token JWT absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Cette demande ne vous appartient pas"),
    @ApiResponse(responseCode = "404", description = "Demande introuvable")
  })
  public ResponseEntity<Void> cancelRequest(
      @Parameter(description = "ID de la demande de visite") @PathVariable("visitRequestId")
          UUID visitRequestId,
      HttpServletRequest httpRequest)
      throws CustomException {

    var caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);

    visitService.cancelVisitRequest(caller.getSub(), visitRequestId);

    return ResponseEntity.noContent().build();
  }
}
