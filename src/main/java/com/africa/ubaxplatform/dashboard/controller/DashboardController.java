package com.africa.ubaxplatform.dashboard.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.dashboard.dto.AgencyDashboardResponse;
import com.africa.ubaxplatform.dashboard.service.interfaces.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Tableau de bord analytique de l'espace agence")
public class DashboardController {

  private final DashboardService dashboardService;
  private final RequestHeaderParser requestHeaderParser;

  @GetMapping("/agency")
  @Operation(
      summary = "Tableau de bord agence",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne les KPIs financiers et portfolio de l'agence connectée pour la période choisie.\n\n"
              + "**Période par défaut :** mois courant (1er jour → dernier jour).\n\n"
              + "**KPIs financiers :** `totalRevenue` · `totalExpenses` · `netRevenue` · "
              + "`overdueAmount` · `recoveryRate`\n\n"
              + "**KPIs portfolio :** `totalProperties` · `publishedProperties` · `reservedProperties` · "
              + "`activeContracts` · `pendingPaymentsCount` · `latePaymentsCount` · `paidPaymentsCount`\n\n"
              + "**Décompositions :** `revenueByType` · `expensesByCategory`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "KPIs du tableau de bord",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AgencyDashboardResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Aucune agence associée au compte",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> getAgencyDashboard(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.AGENCY,
            UserRole.AGENT,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.DASHBOARD_GET_SUCCESS,
            dashboardService.getAgencyDashboard(caller.getSub(), from, to)));
  }
}
