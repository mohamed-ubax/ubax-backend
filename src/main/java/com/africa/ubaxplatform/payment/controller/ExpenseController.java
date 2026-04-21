package com.africa.ubaxplatform.payment.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.dto.ExpenseCreateRequest;
import com.africa.ubaxplatform.payment.dto.ExpenseResponse;
import com.africa.ubaxplatform.payment.service.interfaces.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expense", description = "Gestion des dépenses comptables de l'agence")
public class ExpenseController {

  private final ExpenseService expenseService;
  private final RequestHeaderParser requestHeaderParser;

  @GetMapping
  @Operation(
      summary = "Lister les dépenses",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne la liste paginée des dépenses de l'agence connectée.\n\n"
              + "**Filtres disponibles :** `category`, `propertyId`, `from`, `to`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée de dépenses",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExpenseResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> list(
      @RequestParam(required = false) ExpenseCategory category,
      @RequestParam(required = false) UUID propertyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC)
          Pageable pageable,
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
            ResponseMessageConstants.PAYMENT_GET_LIST_SUCCESS,
            expenseService.list(caller.getSub(), category, propertyId, from, to, pageable)));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'une dépense",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail de la dépense",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExpenseResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Dépense introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
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
            ResponseMessageConstants.PAYMENT_GET_SUCCESS,
            expenseService.getById(id, caller.getSub())));
  }

  @PostMapping
  @Operation(
      summary = "Ajouter une dépense",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Enregistre une dépense comptable de l'agence. "
              + "Si `costCenter = PROPERTY_SPECIFIC`, le champ `propertyId` est **obligatoire**.\n\n"
              + "Pour joindre une facture, uploader d'abord via `GET /v1/storage/presign?bucket=partner-documents` "
              + "puis renseigner `justificationUrl`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Dépense créée",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExpenseResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ExpenseCreateRequest.class)))
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid ExpenseCreateRequest request, HttpServletRequest httpRequest)
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
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.PAYMENT_CREATE_SUCCESS,
                expenseService.create(caller.getSub(), request)));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Supprimer une dépense",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Supprime définitivement une dépense. Seules les dépenses de l'agence connectée sont accessibles.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dépense supprimée", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Dépense introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> delete(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.AGENCY,
            UserRole.AGENT,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    expenseService.delete(id, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PAYMENT_GET_SUCCESS,
            null));
  }
}
