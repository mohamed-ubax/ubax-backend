package com.africa.ubaxplatform.partner.controller;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.ApplicationDecisionRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import com.africa.ubaxplatform.partner.service.interfaces.PartnerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestBody;
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

  private static final long MAX_DOC_BYTES = 10L * 1024 * 1024; // 10 Mo (RCCM, DFE, bail)
  private static final long MAX_LOGO_BYTES = 5L * 1024 * 1024; //  5 Mo
  private static final Set<String> ALLOWED_DOC_TYPES =
      Set.of("application/pdf", "image/jpeg", "image/png");
  private static final Set<String> ALLOWED_LOGO_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

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
          "Formulaire multipart : champ \"data\" (JSON) + fichiers optionnels \"rccm\", \"dfe\", \"bail\" (PDF/JPEG/PNG – max 10 Mo) et \"logo\" (JPEG/PNG/WEBP – max 5 Mo).",
      tags = {"Partner"})
  public ResponseEntity<CustomResponse> apply(
      @RequestPart("data") @Valid PartnerApplicationRequest request,
      @RequestPart(value = "rccm", required = false) MultipartFile rccm,
      @RequestPart(value = "dfe", required = false) MultipartFile dfe,
      @RequestPart(value = "bail", required = false) MultipartFile bail,
      @RequestPart(value = "logo", required = false) MultipartFile logo) {

    String rccmUrl =
        partnerApplicationService.uploadIfPresent(rccm, "rccm", ALLOWED_DOC_TYPES, MAX_DOC_BYTES);
    String dfeUrl =
        partnerApplicationService.uploadIfPresent(dfe, "dfe", ALLOWED_DOC_TYPES, MAX_DOC_BYTES);
    String bailUrl =
        partnerApplicationService.uploadIfPresent(bail, "bail", ALLOWED_DOC_TYPES, MAX_DOC_BYTES);
    String logoUrl =
        partnerApplicationService.uploadIfPresent(logo, "logo", ALLOWED_LOGO_TYPES, MAX_LOGO_BYTES);

    PartnerApplicationResponse response =
        partnerApplicationService.apply(request, rccmUrl, dfeUrl, bailUrl, logoUrl);
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
      tags = {"Partner"},
      security = @SecurityRequirement(name = "bearerAuth"))
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
      tags = {"Partner"},
      security = @SecurityRequirement(name = "bearerAuth"))
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
      tags = {"Partner"},
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> decide(
      @PathVariable UUID id,
      @Valid @RequestBody ApplicationDecisionRequest decision,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    String adminKeycloakId = authentication.getName();
    PartnerApplicationResponse response =
        partnerApplicationService.decide(id, adminKeycloakId, decision);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PARTNER_APPLICATION_DECISION_SUCCESS,
            response));
  }
}
