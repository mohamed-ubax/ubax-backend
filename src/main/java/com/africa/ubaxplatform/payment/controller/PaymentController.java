package com.africa.ubaxplatform.payment.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.dto.PaymentCreateRequest;
import com.africa.ubaxplatform.payment.dto.PaymentResponse;
import com.africa.ubaxplatform.payment.dto.PaymentStatusUpdateRequest;
import com.africa.ubaxplatform.payment.service.interfaces.PaymentService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(
    name = "Payment",
    description = "Gestion des paiements (loyers, cautions, commissions, ventes)")
public class PaymentController {

  private final PaymentService paymentService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Liste & détail ─────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les paiements",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne la liste paginée des paiements de l'agence connectée. "
              + "Les ADMIN voient tous les paiements toutes agences confondues.\n\n"
              + "**Filtres disponibles :** `status`, `type`, `propertyId`, `contractId`, `tenantId`, `from`, `to`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée de paiements",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> list(
      @RequestParam(required = false) PaymentStatus status,
      @RequestParam(required = false) PaymentType type,
      @RequestParam(required = false) UUID propertyId,
      @RequestParam(required = false) UUID contractId,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.DESC)
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
            paymentService.list(
                caller.getSub(),
                status,
                type,
                propertyId,
                contractId,
                tenantId,
                from,
                to,
                pageable)));
  }

  @GetMapping("/late")
  @Operation(
      summary = "Paiements en retard",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne les paiements dont la date d'échéance est dépassée et dont le statut est `PENDING` ou `PARTIAL`. "
              + "Triés par date d'échéance croissante (les plus anciens en premier).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des paiements en retard",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> listLate(HttpServletRequest httpRequest)
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
            paymentService.listLate(caller.getSub())));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'un paiement",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail du paiement",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Paiement introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
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
            paymentService.getById(id)));
  }

  // ── Création ───────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Enregistrer un paiement",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Enregistre un paiement (loyer, caution, commission, vente). "
              + "Le statut initial est calculé automatiquement : `PAID` si `paidDate` et `amountPaid >= amount`, "
              + "`PARTIAL` si paiement incomplet, `LATE` si l'échéance est déjà dépassée, sinon `PENDING`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Paiement enregistré",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Contrat, locataire ou bien introuvable",
        content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PaymentCreateRequest.class)))
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid PaymentCreateRequest request, HttpServletRequest httpRequest)
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
                paymentService.create(caller.getSub(), request)));
  }

  // ── Mise à jour statut ─────────────────────────────────────────

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Mettre à jour le statut d'un paiement",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Permet de marquer un paiement comme `PAID`, `PARTIAL`, `LATE` ou `CANCELLED`. "
              + "Un paiement `CANCELLED` ne peut plus être modifié.\n\n"
              + "Si `status = PARTIAL` et `amountPaid >= amount`, le statut est automatiquement corrigé en `PAID`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Statut mis à jour",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class))),
    @ApiResponse(responseCode = "400", description = "Transition invalide", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Paiement introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PaymentStatusUpdateRequest.class)))
  public ResponseEntity<CustomResponse> updateStatus(
      @PathVariable UUID id,
      @RequestBody @Valid PaymentStatusUpdateRequest request,
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
            ResponseMessageConstants.PAYMENT_CREATE_SUCCESS,
            paymentService.updateStatus(id, caller.getSub(), request)));
  }

  // ── Suppression ────────────────────────────────────────────────

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Supprimer un paiement",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Supprime un paiement en statut `PENDING`, `PARTIAL`, `LATE` ou `CANCELLED`. "
              + "Un paiement `PAID` ne peut pas être supprimé.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Paiement supprimé", content = @Content),
    @ApiResponse(
        responseCode = "400",
        description = "Impossible de supprimer un paiement encaissé",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Paiement introuvable", content = @Content)
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
    paymentService.delete(id, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PAYMENT_GET_SUCCESS,
            null));
  }
}
