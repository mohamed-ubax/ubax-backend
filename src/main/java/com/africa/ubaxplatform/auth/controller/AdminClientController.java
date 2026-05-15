package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.auth.service.interfaces.ClientListingService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/clients")
@RequiredArgsConstructor
@Tag(name = "Administration UBAX")
public class AdminClientController {

  private final ClientListingService clientListingService;
  private final RequestHeaderParser requestHeaderParser;

  @GetMapping
  @Operation(
      summary = "Lister les clients de la plateforme",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne la liste paginée de tous les clients actifs.\n\n"
              + "- Sans filtre : tous les clients `UBAX_CLIENT`\n"
              + "- `?agencyId=` : clients ayant au moins un contrat sur un bien de l'agence\n"
              + "- `?hotelId=` : clients ayant au moins une réservation dans l'hôtel\n\n"
              + "Les deux filtres sont mutuellement exclusifs ; `agencyId` est prioritaire.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste paginée retournée"),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide"),
    @ApiResponse(responseCode = "403", description = "Accès refusé – rôle ADMIN requis"),
    @ApiResponse(responseCode = "404", description = "Agence ou hôtel introuvable")
  })
  public ResponseEntity<CustomResponse> listClients(
      @RequestParam(required = false) UUID agencyId,
      @RequestParam(required = false) UUID hotelId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);

    Page<ClientUserResponse> page;
    if (agencyId != null) {
      page = clientListingService.listClientsByAgency(agencyId, pageable);
    } else if (hotelId != null) {
      page = clientListingService.listClientsByHotel(hotelId, pageable);
    } else {
      page = clientListingService.listAllClients(pageable);
    }

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CLIENT_GET_LIST_SUCCESS,
            page));
  }
}
