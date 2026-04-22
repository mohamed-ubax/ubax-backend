package com.africa.ubaxplatform.partner.controller;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import com.africa.ubaxplatform.partner.service.interfaces.PartnerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur de gestion des demandes d'adhésion partenaire.
 *
 * <p>Endpoints publics :
 *
 * <ul>
 *   <li>{@code POST /v1/partner/apply} – soumission d'une demande (sans authentification)
 * </ul>
 *
 * <p>Endpoints admin (rôle {@code UBAX_ADMIN} requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/partner/admin/applications} – liste paginée avec filtre par statut
 *   <li>{@code GET /v1/partner/admin/applications/{id}} – détail + historique
 *   <li>{@code PATCH /v1/partner/admin/applications/{id}/decision} – approbation / rejet /
 *       incomplet
 * </ul>
 */
@RestController
@RequestMapping("/v1/partner")
@RequiredArgsConstructor
@Tag(name = "Partner")
public class PartnerController {

  private final PartnerApplicationService partnerApplicationService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Public ─────────────────────────────────────────────────────

  /**
   * Soumission publique d'une demande d'adhésion partenaire.
   *
   * <p>Accessible sans authentification. Déclenche une confirmation par email au partenaire et une
   * notification à l'administrateur.
   */
  @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Soumettre une demande d'adhésion partenaire",
      description =
          "🌐 **Public** – Formulaire multipart d'adhésion partenaire (agence immobilière ou hôtel).\n\n"
              + "**Champs :**\n"
              + "- `data` (JSON, obligatoire) : informations de la société\n"
              + "- `rccm` (PDF/JPEG/PNG – max 10 Mo) : registre de commerce\n"
              + "- `dfe` (PDF/JPEG/PNG – max 10 Mo) : déclaration fiscale\n"
              + "- `bail` (PDF/JPEG/PNG – max 10 Mo) : contrat de bail\n"
              + "- `logo` (JPEG/PNG/WEBP – max 5 Mo) : logo de l'entreprise\n\n"
              + "Déclenche une confirmation par email et une notification admin.",
      tags = {"Partner"})
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Demande soumise avec succès",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PartnerApplicationResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Données invalides ou fichier non autorisé",
        content = @Content),
    @ApiResponse(responseCode = "409", description = "Demande déjà existante", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema =
                  @Schema(
                      type = "object",
                      requiredProperties = {"data"})))
  public ResponseEntity<CustomResponse> apply(
      @Parameter(
              description = "Données JSON de la demande",
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = PartnerApplicationRequest.class)))
          @RequestPart("data")
          @Valid
          PartnerApplicationRequest request,
      @Parameter(
              description = "RCCM – Registre de commerce (PDF/JPEG/PNG – max 10 Mo)",
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart(value = "rccm", required = false)
          MultipartFile rccm,
      @Parameter(
              description = "DFE – Déclaration fiscale (PDF/JPEG/PNG – max 10 Mo)",
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart(value = "dfe", required = false)
          MultipartFile dfe,
      @Parameter(
              description = "Bail – Contrat de bail (PDF/JPEG/PNG – max 10 Mo)",
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart(value = "bail", required = false)
          MultipartFile bail,
      @Parameter(
              description = "Logo de l'entreprise (JPEG/PNG/WEBP – max 5 Mo)",
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart(value = "logo", required = false)
          MultipartFile logo)
      throws CustomException {

    PartnerApplicationResponse response =
        partnerApplicationService.apply(request, rccm, dfe, bail, logo);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.PARTNER_APPLICATION_SUBMIT_SUCCESS,
                response));
  }

  // ── Admin ──────────────────────────────────────────────────────

  /**
   * Liste paginée des demandes, avec filtre optionnel par statut.
   *
   * <p>Exemples : {@code ?status=PENDING&page=0&size=20&sort=submittedAt,desc}
   */
  @GetMapping("/admin/applications")
  @Operation(
      summary = "Lister les demandes d'adhésion (admin)",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Liste paginée des demandes avec filtre optionnel par statut.",
      tags = {"Partner"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée des demandes",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PartnerApplicationResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> listApplications(
      @RequestParam(required = false) ApplicationStatus status,
      @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    Page<PartnerApplicationResponse> page =
        partnerApplicationService.listApplications(status, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_APPLICATION_GET_LIST_SUCCESS,
            page));
  }

  /** Détail d'une demande avec l'intégralité de son journal de statuts. */
  @GetMapping("/admin/applications/{id}")
  @Operation(
      summary = "Détail d'une demande d'adhésion (admin)",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne la demande avec l'intégralité de son journal de statuts.",
      tags = {"Partner"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail de la demande",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PartnerApplicationResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Demande introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getApplication(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    PartnerApplicationResponse response = partnerApplicationService.getApplication(id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_APPLICATION_GET_SUCCESS,
            response));
  }

  /** Applique une décision administrative (UNDER_REVIEW, APPROVED, REJECTED, INCOMPLETE). */
  @PatchMapping("/admin/applications/{id}/decision")
  @Operation(
      summary = "Statuer sur une demande d'adhésion (admin)",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Applique une décision administrative. Si `newStatus = APPROVED`, crée automatiquement le compte partenaire Keycloak.",
      tags = {"Partner"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Décision appliquée",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PartnerApplicationResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Statut invalide ou motif manquant pour REJECTED/INCOMPLETE",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Demande introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> decide(
      @PathVariable UUID id,
      @RequestParam
          @Parameter(
              description = "Nouveau statut. PENDING est interdit (réservé à la soumission).",
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {"UNDER_REVIEW", "APPROVED", "REJECTED", "INCOMPLETE"}))
          ApplicationStatus newStatus,
      @RequestParam(required = false)
          @Parameter(description = "Motif obligatoire si newStatus est REJECTED ou INCOMPLETE")
          String comment,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    String adminKeycloakId = authentication.getName();
    PartnerApplicationResponse response =
        partnerApplicationService.decide(id, adminKeycloakId, newStatus, comment);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_APPLICATION_DECISION_SUCCESS,
            response));
  }
}
