package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.AgencyResponse;
import com.africa.ubaxplatform.auth.service.interfaces.AgencyService;
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
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/v1/agencies")
@RequiredArgsConstructor
@Tag(
    name = "Agences",
    description = "Consultation des agences immobilières disponibles sur la plateforme UBAX")
public class AgencyController {

  private final AgencyService agencyService;
  private final RequestHeaderParser requestHeaderParser;

  @GetMapping
  @Operation(
      summary = "Lister les agences disponibles",
      description =
          "📱 **Mobile** · 🔒 **Rôles requis :** `CLIENT` · `OWNER` · `PARTNER` · `ADMIN`\n\n"
              + "Retourne la liste paginée des agences actives. "
              + "Paramètre optionnel `city` pour filtrer par ville (insensible à la casse).\n\n"
              + "Utilisé dans le flux mobile pour permettre au CLIENT de choisir une agence "
              + "avant de soumettre sa demande d'adhésion bailleur (`POST /v1/bailleur/apply`).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des agences",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AgencyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> list(
      @RequestParam(required = false) String city,
      @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser,
        httpRequest,
        UserRole.CLIENT,
        UserRole.OWNER,
        UserRole.PARTNER,
        UserRole.ADMIN,
        UserRole.SUPER_ADMIN);
    Page<AgencyResponse> page = agencyService.listForClient(city, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.AGENCY_LIST_SUCCESS,
            page));
  }
}
