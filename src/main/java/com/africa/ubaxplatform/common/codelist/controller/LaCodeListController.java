package com.africa.ubaxplatform.common.codelist.controller;

import com.africa.ubaxplatform.common.codelist.dto.LaCodeListDto;
import com.africa.ubaxplatform.common.codelist.service.interfaces.LaCodeListService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
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
import org.springdoc.core.annotations.ParameterObject;
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
 * <p>Endpoints publics (aucun token requis) :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list/type/{type}} – liste des valeurs d'un type (selects frontend)
 * </ul>
 *
 * <p>Endpoints authentifiés (tout utilisateur connecté) :
 *
 * <ul>
 *   <li>{@code GET /v1/code-list} – liste paginée de tous les code lists
 *   <li>{@code GET /v1/code-list/{id}} – détail d'un code list
 * </ul>
 *
 * <p>Endpoints admin (rôle {@code UBAX_ADMIN} ou {@code UBAX_SUPER_ADMIN} requis) :
 *
 * <ul>
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
   * du frontend. Le type {@code ROLE_UBAX_INTERNAL} est restreint — utiliser {@code GET
   * /v1/code-list/admin/type/{type}} à la place.
   *
   * <p>Exemple : {@code GET /v1/code-list/type/PROPERTY_TYPE}
   */
  @GetMapping("/type/{type}")
  @Operation(
      summary = "Liste des valeurs par type",
      description =
          "🌐 **Public** — Retourne toutes les entrées actives d'un type donné. "
              + "Aucun token requis.\n\n"
              + "---\n\n"
              + "### Référentiel complet des types\n\n"
              + "| Type | Valeurs |\n"
              + "|------|---------|\n"
              + "| `ROLE_AGENCE` | `DIRECTEUR_AGENCE` · `COMMERCIAL` · `COMPTABLE_AGENCE` · `AGENT_SAV` · `AGENT_IMMOBILIER` |\n"
              + "| `ROLE_HOTEL` | `GERANT_HOTEL` · `RECEPTIONNISTE` · `COMPTABLE_HOTEL` · `RESPONSABLE_HEBERGEMENT` |\n"
              + "| `ID_TYPE` | `CNI` · `PASSEPORT` · `PERMIS_CONDUIRE` · `TITRE_SEJOUR` · `CARTE_CONSULAIRE` |\n"
              + "| `PROPERTY_AMENITY` | `POOL` · `GENERATOR` · `WATER_TANK` · `AC` · `SECURITY` · `PARKING` · `ELEVATOR` · `GARDEN` · `FURNISHED` · `PETS` |\n"
              + "| `TECHNICIEN_PROFESSION` | `PLOMBIER` · `ELECTRICIEN` · `SERRURIER` · `MENUISIER` · `MACON` · `PEINTRE` · `CLIMATISATION` · `VITRERIE` · `JARDINAGE` · `NETTOYAGE` · `DESINSECTISATION` · `AUTRE` |\n"
              + "| `TICKET_CATEGORY` | Voir seeds V019 / V048 |\n"
              + "| `TICKET_PRIORITY` | `LOW` · `NORMAL` · `HIGH` · `URGENT` |\n"
              + "| `TICKET_ATTACHMENT_TYPE` | `INCIDENT_PHOTO` · `INCIDENT_VIDEO` · `INTERVENTION_REPORT` · `INVOICE` · `OTHER` |\n"
              + "| `PROPERTY_TYPE` | Voir seed V027 |\n"
              + "| `TRANSACTION_TYPE` | Voir seed V027 |\n"
              + "| `CONTRACT_TYPE` | `LEASE` · `SALE` · `RENT_TO_OWN` |\n"
              + "| `EMPLOYMENT_STATUS` | `EMPLOYED` · `SELF_EMPLOYED` · `STUDENT` · `RETIRED` · `UNEMPLOYED` · `OTHER` |\n"
              + "| `PARTNER_TYPE` | `AGENCY` · `HOTEL` |\n"
              + "| `COST_IMPUTED_TO` | Voir seed V019 |\n\n"
              + "> ��️ **`ROLE_UBAX_INTERNAL`** est restreint aux admins. "
              + "Utiliser `GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL` (JWT ADMIN requis).",
      tags = {"CodeList"})
  @ApiResponse(
      responseCode = "200",
      description = "Liste récupérée avec succès",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "403", description = "Type restreint — utiliser /admin/type/")
  public ResponseEntity<CustomResponse> findAllByType(@PathVariable String type)
      throws CustomException {
    if ("ROLE_UBAX_INTERNAL".equals(type)) {
      throw new CustomException(
          new UnAuthorizedException(
              "Le type ROLE_UBAX_INTERNAL est restreint. "
                  + "Utiliser GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL (ADMIN requis)."));
    }
    log.info("Récupération des code lists de type : {}", type);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAllByType(type).stream().map(LaCodeListDto::from)));
  }

  /**
   * Retourne toutes les valeurs d'un type de code list — accès réservé aux admins. Donne accès au
   * type restreint {@code ROLE_UBAX_INTERNAL} inaccessible via l'endpoint public.
   *
   * <p>Exemple : {@code GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL}
   */
  @GetMapping("/admin/type/{type}")
  @Operation(
      summary = "Liste des valeurs par type (admin)",
      description =
          "🛡 **Rôle requis :** `ADMIN` ou `SUPER_ADMIN`.\n\n"
              + "Identique à `GET /v1/code-list/type/{type}` mais réservé aux admins. "
              + "Permet d'accéder aux types restreints non exposés publiquement.\n\n"
              + "---\n\n"
              + "### Type restreint accessible via cet endpoint\n\n"
              + "| Type | Valeurs |\n"
              + "|------|---------|\n"
              + "| `ROLE_UBAX_INTERNAL` | `DIRECTEUR_GENERAL` · `SUPPORT_CLIENT` · `OPERATIONS` · `FINANCE` · `COMMERCIAL` |\n\n"
              + "**Exemple d'appel :** `GET /v1/code-list/admin/type/ROLE_UBAX_INTERNAL`\n\n"
              + "> Tous les types publics (ROLE_AGENCE, ROLE_HOTEL, ID_TYPE, etc.) "
              + "sont également accessibles via cet endpoint.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Liste récupérée avec succès",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LaCodeListDto.class)))
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
  public ResponseEntity<CustomResponse> findAllByTypeAdmin(
      @PathVariable String type, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
    log.info("Récupération admin des code lists de type : {}", type);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            codeListService.findAllByType(type).stream().map(LaCodeListDto::from)));
  }

  // ── Authentifié ────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Liste paginée de tous les code lists",
      description = "Accessible à tout utilisateur authentifié. Supporte la pagination et le tri.",
      tags = {"CodeList"},
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
  @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
  public ResponseEntity<CustomResponse> findAll(
      @ParameterObject @PageableDefault(size = 25, sort = "type", direction = Sort.Direction.ASC)
          Pageable pageable)
      throws CustomException {
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
      summary = "Détail d'un code list par id",
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
  @ApiResponse(responseCode = "404", description = "Code list introuvable")
  public ResponseEntity<CustomResponse> findById(@PathVariable UUID id) throws CustomException {
    log.info("Récupération du code list id : {}", id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.CODELIST_GET_SUCCESS,
            LaCodeListDto.from(codeListService.findById(id))));
  }

  // ── Admin ──────────────────────────────────────────────────────

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
