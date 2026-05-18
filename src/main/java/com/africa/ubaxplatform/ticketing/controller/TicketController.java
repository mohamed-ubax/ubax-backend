package com.africa.ubaxplatform.ticketing.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import com.africa.ubaxplatform.ticketing.dto.AddTicketMessageRequest;
import com.africa.ubaxplatform.ticketing.dto.AssignTicketRequest;
import com.africa.ubaxplatform.ticketing.dto.CreateTicketRequest;
import com.africa.ubaxplatform.ticketing.dto.ScheduleInterventionRequest;
import com.africa.ubaxplatform.ticketing.dto.UpdateRepairCostRequest;
import com.africa.ubaxplatform.ticketing.dto.UpdateTicketStatusRequest;
import com.africa.ubaxplatform.ticketing.service.interfaces.TicketService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de gestion des tickets de maintenance et SAV.
 *
 * <p>Workflow : {@code OPEN → IN_ANALYSIS → TECHNICIAN_SENT → RESOLVED → CLOSED}
 *
 * <p>Accès :
 *
 * <ul>
 *   <li>Création : {@code PARTNER} ou {@code CLIENT}
 *   <li>Consultation, assignation, statut, intervention, coût : {@code PARTNER} ou {@code ADMIN}
 *   <li>Messages : tout utilisateur authentifié impliqué dans le ticket
 * </ul>
 */
@RestController
@RequestMapping("/v1/tickets")
@RequiredArgsConstructor
@Tag(
    name = "Ticketing",
    description =
        "🔑 **Authentifié** (création) · 🏢 **PARTNER / ADMIN** (gestion) – Tickets de maintenance SAV.\n\n"
            + "**Workflow :** `OPEN → IN_ANALYSIS → TECHNICIAN_SENT → RESOLVED → CLOSED`\n\n"
            + "**Catégories :** LEAK · ELECTRICAL · LOCK · PLUMBING · APPLIANCE · STRUCTURE · PEST · COMMON_AREA · OTHER\n\n"
            + "**Priorités :** LOW · NORMAL · HIGH · URGENT")
public class TicketController {

  private final TicketService ticketService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(
      summary = "Créer un ticket",
      description = "Déclare un incident dans le cadre d'un contrat actif. Reporter = appelant.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Ticket créé"),
    @ApiResponse(responseCode = "400", description = "Contrat introuvable ou données invalides"),
    @ApiResponse(responseCode = "401", description = "Non authentifié")
  })
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateTicketRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.CLIENT, UserRole.OWNER);
    var result = ticketService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.TICKET_CREATE_SUCCESS,
                result));
  }

  @GetMapping
  @Operation(
      summary = "Lister les tickets de l'agence",
      description =
          "Retourne les tickets paginés de l'agence connectée. Filtrables par statut et agent assigné.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste paginée")
  public ResponseEntity<CustomResponse> list(
      @Parameter(description = "Filtre par statut") @RequestParam(required = false)
          TicketStatus status,
      @Parameter(description = "Filtre par agent assigné (UUID)") @RequestParam(required = false)
          UUID assignedToId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    var result = ticketService.list(caller.getSub(), status, assignedToId, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_GET_LIST_SUCCESS,
            result));
  }

  @GetMapping("/mine")
  @Operation(
      summary = "Mes tickets (CLIENT / OWNER)",
      description =
          "Retourne les tickets déclarés par l'utilisateur connecté, triés du plus récent au plus ancien.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste paginée")
  public ResponseEntity<CustomResponse> listMine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER, UserRole.PARTNER);
    var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_GET_LIST_SUCCESS,
            ticketService.listMine(caller.getSub(), pageable)));
  }

  @GetMapping("/{ticketId}")
  @Operation(
      summary = "Détail d'un ticket",
      description =
          "Retourne le détail complet d'un ticket. "
              + "CLIENT et OWNER ne peuvent consulter que leurs propres tickets (vérification reporter).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ticket trouvé"),
    @ApiResponse(responseCode = "403", description = "Ticket appartenant à un autre utilisateur"),
    @ApiResponse(responseCode = "404", description = "Ticket introuvable")
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID ticketId, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN,
            UserRole.CLIENT,
            UserRole.OWNER);
    boolean isRestricted =
        caller.getRole() == UserRole.CLIENT || caller.getRole() == UserRole.OWNER;
    String keycloakId = isRestricted ? caller.getSub() : null;
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_GET_SUCCESS,
            ticketService.getById(ticketId, keycloakId)));
  }

  @PatchMapping("/{ticketId}/assign")
  @Operation(
      summary = "Assigner un ticket",
      description = "Assigne le ticket à un agent SAV ou prestataire interne.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ticket assigné"),
    @ApiResponse(responseCode = "404", description = "Ticket ou agent introuvable")
  })
  public ResponseEntity<CustomResponse> assign(
      @PathVariable UUID ticketId,
      @RequestBody @Valid AssignTicketRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_UPDATE_SUCCESS,
            ticketService.assign(ticketId, request)));
  }

  @PatchMapping("/{ticketId}/status")
  @Operation(
      summary = "Changer le statut d'un ticket",
      description =
          "Transitions valides : OPEN→IN_ANALYSIS|CANCELLED · IN_ANALYSIS→TECHNICIAN_SENT|RESOLVED|CANCELLED"
              + " · TECHNICIAN_SENT→RESOLVED|CANCELLED · RESOLVED→CLOSED",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
    @ApiResponse(responseCode = "400", description = "Transition invalide ou note manquante"),
    @ApiResponse(responseCode = "404", description = "Ticket introuvable")
  })
  public ResponseEntity<CustomResponse> updateStatus(
      @PathVariable UUID ticketId,
      @RequestBody @Valid UpdateTicketStatusRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_UPDATE_SUCCESS,
            ticketService.updateStatus(ticketId, request, caller.getSub())));
  }

  @PatchMapping("/{ticketId}/schedule")
  @Operation(
      summary = "Planifier une intervention",
      description =
          "Enregistre le technicien mandaté et la date d'intervention planifiée. "
              + "Passe automatiquement le ticket en `TECHNICIAN_SENT`.\n\n"
              + "**Deux modes mutuellement exclusifs :**\n\n"
              + "- **Mode plateforme** (`technicienId` fourni) : nom et téléphone récupérés depuis le profil du technicien — champs requis : `technicienId` + `interventionScheduledAt`.\n"
              + "- **Mode libre** (`technicienId` absent) : prestataire ponctuel non référencé — champs requis : `technicianName` + `technicianPhone` + `interventionScheduledAt`.\n\n"
              + "`interventionPrice` est optionnel dans les deux modes (≥ 0). "
              + "`interventionScheduledAt` doit être dans le futur.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Intervention planifiée — ticket passé en TECHNICIAN_SENT"),
    @ApiResponse(
        responseCode = "400",
        description =
            "Transition invalide · `interventionScheduledAt` dans le passé · mode libre sans `technicianName` ou `technicianPhone`"),
    @ApiResponse(responseCode = "404", description = "Ticket ou technicien introuvable")
  })
  public ResponseEntity<CustomResponse> scheduleIntervention(
      @PathVariable UUID ticketId,
      @RequestBody @Valid ScheduleInterventionRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_UPDATE_SUCCESS,
            ticketService.scheduleIntervention(ticketId, request)));
  }

  @PatchMapping("/{ticketId}/repair-cost")
  @Operation(
      summary = "Saisir le coût de réparation",
      description = "Enregistre le coût de réparation et son imputation (OWNER/TENANT/SHARED).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Coût enregistré"),
    @ApiResponse(responseCode = "404", description = "Ticket introuvable")
  })
  public ResponseEntity<CustomResponse> updateRepairCost(
      @PathVariable UUID ticketId,
      @RequestBody @Valid UpdateRepairCostRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_UPDATE_SUCCESS,
            ticketService.updateRepairCost(ticketId, request)));
  }

  @PostMapping("/{ticketId}/messages")
  @Operation(
      summary = "Ajouter un message",
      description =
          "Ajoute un message dans le fil de discussion du ticket. "
              + "Type PUBLIC (visible par toutes les parties) ou INTERNAL (note équipe SAV uniquement).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Message ajouté"),
    @ApiResponse(responseCode = "404", description = "Ticket introuvable")
  })
  public ResponseEntity<CustomResponse> addMessage(
      @PathVariable UUID ticketId,
      @RequestBody @Valid AddTicketMessageRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.CLIENT,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    var result = ticketService.addMessage(ticketId, request, caller.getSub());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.TICKET_MESSAGE_CREATE_SUCCESS,
                result));
  }

  @GetMapping("/{ticketId}/messages")
  @Operation(
      summary = "Messages d'un ticket",
      description = "Retourne l'historique des messages du fil de discussion.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste des messages")
  public ResponseEntity<CustomResponse> listMessages(
      @PathVariable UUID ticketId, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser,
        httpRequest,
        UserRole.PARTNER,
        UserRole.CLIENT,
        UserRole.OWNER,
        UserRole.ADMIN,
        UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_MESSAGE_GET_LIST_SUCCESS,
            ticketService.listMessages(ticketId)));
  }

  @GetMapping("/{ticketId}/attachments")
  @Operation(
      summary = "Pièces jointes d'un ticket",
      description = "Retourne les pièces jointes directement liées au ticket.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste des pièces jointes")
  public ResponseEntity<CustomResponse> listAttachments(
      @PathVariable UUID ticketId, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser,
        httpRequest,
        UserRole.PARTNER,
        UserRole.CLIENT,
        UserRole.OWNER,
        UserRole.ADMIN,
        UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TICKET_ATTACHMENT_GET_LIST_SUCCESS,
            ticketService.listAttachments(ticketId)));
  }
}
