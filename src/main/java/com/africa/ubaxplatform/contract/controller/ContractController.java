package com.africa.ubaxplatform.contract.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.dto.ContractResponse;
import com.africa.ubaxplatform.contract.dto.ContractStatsResponse;
import com.africa.ubaxplatform.contract.dto.CreateContractRequest;
import com.africa.ubaxplatform.contract.dto.TerminateContractRequest;
import com.africa.ubaxplatform.contract.service.interfaces.ContractService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contracts", description = "Gestion des contrats (bail, vente, réservation, mandat)")
public class ContractController {

  private final ContractService contractService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(summary = "Créer un contrat", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Contrat créé"),
    @ApiResponse(responseCode = "400", description = "Requête invalide"),
    @ApiResponse(responseCode = "404", description = "Bien ou propriétaire introuvable"),
  })
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateContractRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);
    ContractResponse response = contractService.create(caller.getSub(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.CONTRACT_CREATE_SUCCESS,
                response));
  }

  @GetMapping
  @Operation(summary = "Lister les contrats", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste retournée"),
  })
  public ResponseEntity<CustomResponse> list(
      @RequestParam(required = false) ContractStatus status,
      @RequestParam(required = false) String search,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);

    Page<ContractResponse> page;
    if (caller.hasRole(UserRole.ADMIN) || caller.hasRole(UserRole.SUPER_ADMIN)) {
      page = contractService.listAll(status, search, pageable);
    } else {
      page = contractService.list(caller.getSub(), status, search, pageable);
    }

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_GET_LIST_SUCCESS,
            page));
  }

  @GetMapping("/stats")
  @Operation(
      summary = "KPIs contrats (total, actifs, en attente, résiliés/annulés)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Statistiques retournées"),
  })
  public ResponseEntity<CustomResponse> getStats(HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);
    ContractStatsResponse stats = contractService.getStats(caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_GET_STATS_SUCCESS,
            stats));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Détail d'un contrat", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contrat trouvé"),
    @ApiResponse(responseCode = "403", description = "Accès refusé"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.CLIENT);
    ContractResponse response = contractService.getById(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_GET_SUCCESS,
            response));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Modifier un contrat (DRAFT uniquement)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contrat modifié"),
    @ApiResponse(responseCode = "400", description = "Contrat non en brouillon"),
    @ApiResponse(responseCode = "403", description = "Accès refusé"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> update(
      @PathVariable UUID id,
      @RequestBody @Valid CreateContractRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);
    ContractResponse response = contractService.update(caller.getSub(), id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/submit")
  @Operation(
      summary = "Soumettre un contrat pour signature (DRAFT → PENDING_SIGNATURE)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Soumis pour signature"),
    @ApiResponse(responseCode = "400", description = "Transition de statut invalide"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> submit(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);
    ContractResponse response = contractService.submit(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/activate")
  @Operation(
      summary = "Activer un contrat (PENDING_SIGNATURE → ACTIVE) — admin seulement",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contrat activé"),
    @ApiResponse(responseCode = "400", description = "Transition de statut invalide"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> activate(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller = RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    ContractResponse response = contractService.activate(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/terminate")
  @Operation(
      summary = "Résilier un contrat actif (ACTIVE → TERMINATED)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contrat résilié"),
    @ApiResponse(responseCode = "400", description = "Transition de statut invalide"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> terminate(
      @PathVariable UUID id,
      @RequestBody @Valid TerminateContractRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);
    ContractResponse response = contractService.terminate(caller.getSub(), id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/cancel")
  @Operation(
      summary = "Annuler un contrat (DRAFT ou PENDING_SIGNATURE → CANCELLED)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contrat annulé"),
    @ApiResponse(responseCode = "400", description = "Annulation impossible dans ce statut"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> cancel(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.PARTNER, UserRole.OWNER, UserRole.ADMIN);
    ContractResponse response = contractService.cancel(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }
}
