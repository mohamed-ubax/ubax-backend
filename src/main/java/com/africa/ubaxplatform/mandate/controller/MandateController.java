package com.africa.ubaxplatform.mandate.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.mandate.codeList.MandateStatus;
import com.africa.ubaxplatform.mandate.dto.CreateMandateRequest;
import com.africa.ubaxplatform.mandate.dto.TerminateMandateRequest;
import com.africa.ubaxplatform.mandate.service.interfaces.MandateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/v1/mandates")
@RequiredArgsConstructor
@Tag(name = "Mandats de gestion (Agence ↔ Bailleur)")
public class MandateController {

  private final MandateService mandateService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(
      summary = "Créer un mandat de gestion",
      description =
          "🛡 **Rôle requis :** `PARTNER` (membre d'agence)\n\n"
              + "Crée un mandat de gestion entre l'agence et un bailleur approuvé. "
              + "Le bailleur doit déjà avoir un lien approuvé avec l'agence (`POST /v1/bailleur/apply`).",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> create(
      @Valid @RequestBody CreateMandateRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.MANDATE_CREATE_SUCCESS,
                mandateService.create(caller.getSub(), request)));
  }

  @GetMapping
  @Operation(
      summary = "Lister les mandats de l'agence",
      description = "🛡 **Rôle requis :** `PARTNER`",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> list(
      @RequestParam(required = false) MandateStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.MANDATE_GET_LIST_SUCCESS,
            mandateService.list(caller.getSub(), status, pageable)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'un mandat", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.MANDATE_GET_SUCCESS,
            mandateService.getById(caller.getSub(), id)));
  }

  @PatchMapping("/{id}/submit")
  @Operation(
      summary = "Soumettre le mandat pour signature",
      description = "Passe le mandat de `DRAFT` à `PENDING_SIGNATURE`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> submit(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.MANDATE_SUBMIT_SUCCESS,
            mandateService.submit(caller.getSub(), id)));
  }

  @PatchMapping("/{id}/activate")
  @Operation(
      summary = "Activer le mandat",
      description = "👑 **Rôle requis :** `ADMIN`\n\nPasse de `PENDING_SIGNATURE` à `ACTIVE`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> activate(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    var caller = RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.MANDATE_ACTIVATE_SUCCESS,
            mandateService.activate(caller.getSub(), id)));
  }

  @PatchMapping("/{id}/terminate")
  @Operation(
      summary = "Résilier le mandat",
      description = "Passe de `ACTIVE` à `TERMINATED`. Motif obligatoire.",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> terminate(
      @PathVariable UUID id,
      @Valid @RequestBody TerminateMandateRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.MANDATE_TERMINATE_SUCCESS,
            mandateService.terminate(caller.getSub(), id, request)));
  }

  @PatchMapping("/{id}/cancel")
  @Operation(
      summary = "Annuler le mandat",
      description = "Annule un mandat `DRAFT` ou `PENDING_SIGNATURE`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> cancel(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    var caller = RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.MANDATE_CANCEL_SUCCESS,
            mandateService.cancel(caller.getSub(), id)));
  }
}
