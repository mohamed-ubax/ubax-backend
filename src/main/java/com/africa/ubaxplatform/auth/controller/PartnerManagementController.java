package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.dto.AdminAgencyResponse;
import com.africa.ubaxplatform.auth.dto.AdminHotelResponse;
import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.dto.UpdateSubscriptionRequest;
import com.africa.ubaxplatform.auth.service.interfaces.AdminPartnerService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/partners")
@RequiredArgsConstructor
@Tag(name = "Administration UBAX")
public class PartnerManagementController {

  private final AdminPartnerService adminPartnerService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Agences ───────────────────────────────────────────────────────

  @GetMapping("/agencies")
  @Operation(
      summary = "Lister les agences",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nRetourne la liste paginée de toutes les agences non supprimées.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des agences",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminAgencyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> listAgencies(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    Page<AdminAgencyResponse> page = adminPartnerService.listAgencies(pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_GET_LIST_SUCCESS,
            page));
  }

  @PatchMapping("/agencies/{id}/suspend")
  @Operation(
      summary = "Suspendre une agence",
      description =
          "👑 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nDésactive le compte de l'agence (`is_active = false`). Les membres ne peuvent plus se connecter.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Agence suspendue",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminAgencyResponse.class))),
    @ApiResponse(responseCode = "404", description = "Agence introuvable", content = @Content),
    @ApiResponse(responseCode = "409", description = "Agence déjà suspendue", content = @Content)
  })
  public ResponseEntity<CustomResponse> suspendAgency(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_SUSPEND_SUCCESS,
            adminPartnerService.suspendAgency(id)));
  }

  @PatchMapping("/agencies/{id}/activate")
  @Operation(
      summary = "Réactiver une agence",
      description =
          "👑 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nRéactive le compte de l'agence (`is_active = true`).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Agence réactivée",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminAgencyResponse.class))),
    @ApiResponse(responseCode = "404", description = "Agence introuvable", content = @Content),
    @ApiResponse(responseCode = "409", description = "Agence déjà active", content = @Content)
  })
  public ResponseEntity<CustomResponse> activateAgency(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_ACTIVATE_SUCCESS,
            adminPartnerService.activateAgency(id)));
  }

  @PatchMapping("/agencies/{id}/subscription")
  @Operation(
      summary = "Mettre à jour l'abonnement d'une agence",
      description =
          "👑 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nModifie le plan (`BASIC | PRO | PREMIUM`) et la date d'expiration.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Abonnement mis à jour",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminAgencyResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(responseCode = "404", description = "Agence introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> updateAgencySubscription(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateSubscriptionRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_SUBSCRIPTION_UPDATE_SUCCESS,
            adminPartnerService.updateAgencySubscription(id, request)));
  }

  // ── Hôtels ────────────────────────────────────────────────────────

  @GetMapping("/hotels")
  @Operation(
      summary = "Lister les hôtels",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nRetourne la liste paginée de tous les hôtels non supprimés.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des hôtels",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminHotelResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> listHotels(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    Page<AdminHotelResponse> page = adminPartnerService.listHotels(pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_GET_LIST_SUCCESS,
            page));
  }

  @PatchMapping("/hotels/{id}/suspend")
  @Operation(
      summary = "Suspendre un hôtel",
      description =
          "👑 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nDésactive le compte de l'hôtel (`is_active = false`).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Hôtel suspendu",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminHotelResponse.class))),
    @ApiResponse(responseCode = "404", description = "Hôtel introuvable", content = @Content),
    @ApiResponse(responseCode = "409", description = "Hôtel déjà suspendu", content = @Content)
  })
  public ResponseEntity<CustomResponse> suspendHotel(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_SUSPEND_SUCCESS,
            adminPartnerService.suspendHotel(id)));
  }

  @PatchMapping("/hotels/{id}/activate")
  @Operation(
      summary = "Réactiver un hôtel",
      description =
          "👑 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nRéactive le compte de l'hôtel (`is_active = true`).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Hôtel réactivé",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminHotelResponse.class))),
    @ApiResponse(responseCode = "404", description = "Hôtel introuvable", content = @Content),
    @ApiResponse(responseCode = "409", description = "Hôtel déjà actif", content = @Content)
  })
  public ResponseEntity<CustomResponse> activateHotel(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_ACTIVATE_SUCCESS,
            adminPartnerService.activateHotel(id)));
  }

  @PatchMapping("/hotels/{id}/subscription")
  @Operation(
      summary = "Mettre à jour l'abonnement d'un hôtel",
      description =
          "👑 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nModifie le plan (`BASIC | PRO | PREMIUM`) et la date d'expiration.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Abonnement mis à jour",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AdminHotelResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(responseCode = "404", description = "Hôtel introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> updateHotelSubscription(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateSubscriptionRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_SUBSCRIPTION_UPDATE_SUCCESS,
            adminPartnerService.updateHotelSubscription(id, request)));
  }

  // ── Clients ───────────────────────────────────────────────────────

  @GetMapping("/clients")
  @Operation(
      summary = "Lister les clients",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\nRetourne la liste paginée des utilisateurs avec le rôle CLIENT.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des clients",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClientUserResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> listClients(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    Page<ClientUserResponse> page = adminPartnerService.listClients(pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CLIENT_GET_LIST_SUCCESS,
            page));
  }
}
