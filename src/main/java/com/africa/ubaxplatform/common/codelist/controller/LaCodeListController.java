package com.africa.ubaxplatform.common.codelist.controller;

import com.africa.ubaxplatform.common.codelist.dto.LaCodeListDto;
import com.africa.ubaxplatform.common.codelist.service.interfaces.LaCodeListService;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST des référentiels de valeurs (code lists).
 *
 * <p>Endpoints publics :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list/type/{type}} – liste des valeurs d'un type (selects frontend)
 * </ul>
 *
 * <p>Endpoints admin (rôle {@code UBAX_ADMIN} ou {@code UBAX_SUPER_ADMIN} requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list} – liste paginée de tous les code lists
 *   <li>{@code GET /v1/code-list/{id}} – détail d'un code list
 *   <li>{@code POST /v1/code-list} – création manuelle
 *   <li>{@code PUT /v1/code-list/{id}} – mise à jour
 * </ul>
 */
@RestController
@RequestMapping("/v1/code-list")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CodeList", description = "Référentiels de valeurs persistées (selects frontend)")
public class LaCodeListController {

  private final LaCodeListService codeListService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Public ─────────────────────────────────────────────────────

  /**
   * Retourne toutes les valeurs d'un type de code list. Endpoint principal pour peupler les selects
   * du frontend.
   *
   * <p>Exemple : {@code GET /v1/code-list/type/PROPERTY_TYPE}
   */
  @GetMapping("/type/{type}")
  @Operation(
      summary = "Liste des valeurs par type",
      description =
          "Retourne toutes les entrées d'un type donné. Utiliser les constantes de type : "
              + "PROPERTY_TYPE, TRANSACTION_TYPE, PROPERTY_CONDITION, PROPERTY_DOCUMENT_TYPE, "
              + "MEDIA_TYPE, PARTNER_TYPE, EMPLOYMENT_STATUS, ID_DOCUMENT_TYPE, "
              + "TICKET_CATEGORY, TICKET_PRIORITY, TICKET_ATTACHMENT_TYPE, COST_IMPUTED_TO, "
              + "CONTRACT_TYPE, SUBSCRIPTION_PLAN, ALERT_FREQUENCY, NEWSLETTER_FREQUENCY, DISPLAY_MODE.",
      tags = {"CodeList"})
  @ApiResponse(
      responseCode = "200",
      description = "Liste récupérée avec succès",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  public ResponseEntity<CustomResponse> findAllByType(@PathVariable String type) {
    log.info("Récupération des code lists de type : {}", type);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAllByType(type).stream().map(LaCodeListDto::from)));
  }

  // ── Admin ──────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Liste paginée de tous les code lists (admin)",
      description = "Accessible uniquement aux administrateurs. Supporte la pagination et le tri.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  public ResponseEntity<CustomResponse> findAll(
      @PageableDefault(size = 25, sort = "type", direction = Sort.Direction.ASC) Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    log.info("Récupération paginée des code lists");
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAll(pageable).map(LaCodeListDto::from)));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'un code list par id (admin)",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Code list trouvé",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  @ApiResponse(responseCode = "404", description = "Code list introuvable")
  public ResponseEntity<CustomResponse> findById(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    log.info("Récupération du code list id : {}", id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            LaCodeListDto.from(codeListService.findById(id))));
  }

  @PostMapping
  @Operation(
      summary = "Créer un code list (admin)",
      description =
          "Crée une nouvelle valeur de référentiel. Le couple type/value doit être unique.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "201",
      description = "Code list créé avec succès",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "400", description = "Données invalides")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  @ApiResponse(responseCode = "409", description = "Ce couple type/value existe déjà")
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid LaCodeListDto dto, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    log.info("Création d'un code list type={} value={}", dto.getType(), dto.getValue());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.CODELIST_POST_SUCCESS,
                LaCodeListDto.from(codeListService.createCodeList(dto.toEntity()))));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Modifier un code list (admin)",
      description =
          "Met à jour un code list existant. L'id dans le body doit correspondre à celui du path.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Code list mis à jour avec succès",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "400", description = "Données invalides ou id incohérent")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  @ApiResponse(responseCode = "404", description = "Code list introuvable")
  @ApiResponse(responseCode = "409", description = "Ce couple type/value existe déjà")
  public ResponseEntity<CustomResponse> update(
      @PathVariable UUID id, @RequestBody @Valid LaCodeListDto dto, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    log.info("Mise à jour du code list id : {}", id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_PUT_SUCCESS,
            LaCodeListDto.from(codeListService.updateCodeList(dto.toEntity(), id))));
  }
}
