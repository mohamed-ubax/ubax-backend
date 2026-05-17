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
@Tag(
    name = "Contracts",
    description =
        "Gestion des contrats immobiliers.\n\n"
            + "**Types supportés et champs obligatoires (validés backend) :**\n\n"
            + "| Type | Description | Obligatoires | Notes |\n"
            + "|------|-------------|--------------|-------|\n"
            + "| `LEASE` | Bail de location | `tenantId`, `monthlyRent` |"
            + " `depositAmount` recommandé (`monthlyRent × 2`) · `endDate` optionnel (reconduction tacite)"
            + " · activation → 1er loyer créé |\n"
            + "| `SALE` | Acte de vente | `salePrice` | Pas de locataire, pas de loyer récurrent |\n"
            + "| `RENT_TO_OWN` | Location-vente | `tenantId`, `salePrice`, `monthlyInstallment`, `endDate` |"
            + " `endDate` **obligatoire** · `depositAmount` recommandé (`monthlyInstallment × 6`) |\n"
            + "| `RESERVATION` | Contrat de réservation | `reservationDeposit` |"
            + " `reservationDurationDays` recommandé (défaut 30 j) |\n"
            + "| `MANDATE` | Mandat de gestion locative | aucun champ financier |"
            + " `agencyCommissionRate` défaut 10 % · masquer `tenantId`, `monthlyRent`, `depositAmount` |")
public class ContractController {

  private final ContractService contractService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(
      summary = "Créer un contrat",
      description =
          "Crée un contrat en statut **DRAFT**. Le backend valide les champs obligatoires selon le"
              + " `contractType` — une `400` est retournée si un champ requis est absent.\n\n"
              + "**LEASE** — Bail de location :\n"
              + "```json\n"
              + "{\n"
              + "  \"contractType\": \"LEASE\",\n"
              + "  \"propertyId\": \"uuid-du-bien\",\n"
              + "  \"ownerId\": \"uuid-du-proprietaire\",\n"
              + "  \"tenantId\": \"uuid-dossier-locataire\",\n"
              + "  \"startDate\": \"2026-06-01\",\n"
              + "  \"monthlyRent\": 250000,\n"
              + "  \"depositAmount\": 500000,\n"
              + "  \"paymentDay\": 5\n"
              + "}\n"
              + "```\n\n"
              + "**SALE** — Acte de vente :\n"
              + "```json\n"
              + "{\n"
              + "  \"contractType\": \"SALE\",\n"
              + "  \"propertyId\": \"uuid-du-bien\",\n"
              + "  \"ownerId\": \"uuid-du-proprietaire\",\n"
              + "  \"startDate\": \"2026-06-01\",\n"
              + "  \"salePrice\": 25000000\n"
              + "}\n"
              + "```\n\n"
              + "**RENT_TO_OWN** — Location-vente (`endDate` obligatoire) :\n"
              + "```json\n"
              + "{\n"
              + "  \"contractType\": \"RENT_TO_OWN\",\n"
              + "  \"propertyId\": \"uuid-du-bien\",\n"
              + "  \"ownerId\": \"uuid-du-proprietaire\",\n"
              + "  \"tenantId\": \"uuid-dossier-locataire\",\n"
              + "  \"startDate\": \"2026-06-01\",\n"
              + "  \"endDate\": \"2031-06-01\",\n"
              + "  \"salePrice\": 12000000,\n"
              + "  \"monthlyInstallment\": 200000,\n"
              + "  \"depositAmount\": 1200000\n"
              + "}\n"
              + "```\n\n"
              + "**RESERVATION** — Contrat de réservation :\n"
              + "```json\n"
              + "{\n"
              + "  \"contractType\": \"RESERVATION\",\n"
              + "  \"propertyId\": \"uuid-du-bien\",\n"
              + "  \"ownerId\": \"uuid-du-proprietaire\",\n"
              + "  \"startDate\": \"2026-06-01\",\n"
              + "  \"reservationDeposit\": 500000,\n"
              + "  \"reservationDurationDays\": 30\n"
              + "}\n"
              + "```\n\n"
              + "**MANDATE** — Mandat de gestion locative :\n"
              + "```json\n"
              + "{\n"
              + "  \"contractType\": \"MANDATE\",\n"
              + "  \"propertyId\": \"uuid-du-bien\",\n"
              + "  \"ownerId\": \"uuid-du-proprietaire\",\n"
              + "  \"startDate\": \"2026-06-01\",\n"
              + "  \"endDate\": \"2027-06-01\",\n"
              + "  \"agencyCommissionRate\": 10.00,\n"
              + "  \"specialClauses\": \"...\",\n"
              + "  \"terminationConditions\": \"...\"\n"
              + "}\n"
              + "```",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Contrat créé en DRAFT"),
    @ApiResponse(
        responseCode = "400",
        description =
            "Champ obligatoire manquant pour le type (tenantId/monthlyRent pour LEASE,"
                + " salePrice pour SALE, salePrice+monthlyInstallment+endDate+tenantId pour"
                + " RENT_TO_OWN, reservationDeposit pour RESERVATION) · ou contractType invalide"),
    @ApiResponse(
        responseCode = "404",
        description = "Bien, propriétaire ou dossier locataire introuvable"),
  })
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateContractRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
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
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);

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
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
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
            UserRole.SUPER_ADMIN,
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
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
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
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
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
      summary = "Activer un contrat (PENDING_SIGNATURE → ACTIVE)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contrat activé"),
    @ApiResponse(responseCode = "400", description = "Transition de statut invalide"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> activate(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
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
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    ContractResponse response = contractService.terminate(caller.getSub(), id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/generate-pdf")
  @Operation(
      summary = "Régénérer le PDF d'un contrat (tout statut)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "PDF régénéré"),
    @ApiResponse(responseCode = "404", description = "Contrat introuvable"),
  })
  public ResponseEntity<CustomResponse> regeneratePdf(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    ContractResponse response = contractService.regeneratePdf(caller.getSub(), id);
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
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    ContractResponse response = contractService.cancel(caller.getSub(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CONTRACT_UPDATE_SUCCESS,
            response));
  }
}
