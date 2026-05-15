package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints admin back-office pour consulter les membres des structures partenaires.
 *
 * <p>Réservé aux rôles ADMIN et SUPER_ADMIN. Permet la consultation de l'équipe d'une agence ou
 * d'un hôtel sans contrainte de structure (contrairement aux endpoints PARTNER).
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration UBAX")
public class AdminPartnerController {

  private final UserRoleService userRoleService;
  private final RequestHeaderParser requestHeaderParser;

  @GetMapping("/agencies/{agencyId}/members")
  @Operation(
      summary = "Membres d'une agence (back-office)",
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Retourne tous les membres actifs d'une agence partenaire, "
              + "quel que soit le PARTNER appelant. Utilisé pour le support et l'audit.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée"),
    @ApiResponse(responseCode = "403", description = "ADMIN requis")
  })
  public ResponseEntity<CustomResponse> getAgencyMembers(
      @PathVariable UUID agencyId, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    var result = userRoleService.getAgencyMembersForAdmin(agencyId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            result));
  }

  @GetMapping("/agencies/{agencyId}/members/inactive")
  @Operation(
      summary = "Membres inactifs d'une agence (back-office)",
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Retourne les membres retirés (soft-deleted) d'une agence partenaire.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée"),
    @ApiResponse(responseCode = "403", description = "ADMIN requis")
  })
  public ResponseEntity<CustomResponse> getInactiveAgencyMembers(
      @PathVariable UUID agencyId, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    var result = userRoleService.getInactiveAgencyMembersForAdmin(agencyId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_LIST_SUCCESS,
            result));
  }

  @GetMapping("/hotels/{hotelId}/members")
  @Operation(
      summary = "Membres d'un hôtel (back-office)",
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Retourne tous les membres actifs d'un hôtel partenaire. "
              + "Utilisé pour le support et l'audit.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée"),
    @ApiResponse(responseCode = "403", description = "ADMIN requis")
  })
  public ResponseEntity<CustomResponse> getHotelMembers(
      @PathVariable UUID hotelId, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    var result = userRoleService.getHotelMembersForAdmin(hotelId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            result));
  }

  @GetMapping("/hotels/{hotelId}/members/inactive")
  @Operation(
      summary = "Membres inactifs d'un hôtel (back-office)",
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Retourne les membres retirés (soft-deleted) d'un hôtel partenaire.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée"),
    @ApiResponse(responseCode = "403", description = "ADMIN requis")
  })
  public ResponseEntity<CustomResponse> getInactiveHotelMembers(
      @PathVariable UUID hotelId, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    var result = userRoleService.getInactiveHotelMembersForAdmin(hotelId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_LIST_SUCCESS,
            result));
  }
}
