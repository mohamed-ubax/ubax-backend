package com.africa.ubaxplatform.property.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.dto.PropertyBoostRequest;
import com.africa.ubaxplatform.property.dto.PropertyCreateRequest;
import com.africa.ubaxplatform.property.dto.PropertyDetailResponse;
import com.africa.ubaxplatform.property.dto.PropertyDocumentAddRequest;
import com.africa.ubaxplatform.property.dto.PropertyDocumentResponse;
import com.africa.ubaxplatform.property.dto.PropertyExpirationRequest;
import com.africa.ubaxplatform.property.dto.PropertyMediaAddRequest;
import com.africa.ubaxplatform.property.dto.PropertyMediaResponse;
import com.africa.ubaxplatform.property.dto.PropertyResponse;
import com.africa.ubaxplatform.property.dto.PropertyStatusUpdateRequest;
import com.africa.ubaxplatform.property.dto.PropertyUpdateRequest;
import com.africa.ubaxplatform.property.service.interfaces.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Property", description = "Gestion des biens immobiliers UBAX")
public class PropertyController {

  private final PropertyService propertyService;
  private final RequestHeaderParser requestHeaderParser;

  // ── Listing public ─────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "Lister les biens",
      description =
          "🌐 **Public** pour `status=PUBLISHED` (défaut). "
              + "🛡 **ADMIN / SUPER_ADMIN** requis pour tout autre statut (`PENDING`, `DRAFT`, `REJECTED`, `ARCHIVED`).\n\n"
              + "Filtres disponibles :\n"
              + "- `city` — ville du bien\n"
              + "- `propertyType` — type de bien (`APARTMENT`, `VILLA`, `HOTEL_SUITE`…)\n"
              + "- `transactionType` — nature de la transaction (`SALE`, `RENT`, `SHORT_STAY`…)\n"
              + "- `minPrice` / `maxPrice` — fourchette de prix en XOF\n"
              + "- `agencyId` — filtre par agence immobilière (UUID)\n"
              + "- `ownerId` — filtre par propriétaire/bailleur (UUID)\n"
              + "- `hotelId` — filtre par hôtel (UUID) — biens de type chambre/suite uniquement",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée de biens",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent – requis pour les statuts non publiés",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis pour les statuts non publiés",
        content = @Content)
  })
  @Parameters({
    @Parameter(
        name = "page",
        description = "Numéro de page (0-based)",
        example = "0",
        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
    @Parameter(
        name = "size",
        description = "Nombre de résultats par page (max 200)",
        example = "20",
        schema = @Schema(type = "integer", minimum = "1", maximum = "200", defaultValue = "20")),
    @Parameter(
        name = "sort",
        description =
            "Tri : champ,direction — ex: `createdAt,desc` · `price,asc` · `publishedAt,desc`",
        example = "publishedAt,desc",
        schema = @Schema(type = "string"))
  })
  public ResponseEntity<CustomResponse> list(
      @Parameter(
              description =
                  "Statut des biens (défaut : PUBLISHED). ADMIN requis pour tout autre statut.")
          @RequestParam(required = false, defaultValue = "PUBLISHED")
          PropertyStatus status,
      @Parameter(description = "Filtre par ville (insensible à la casse)")
          @RequestParam(required = false)
          String city,
      @Parameter(description = "Filtre par type de bien (APARTMENT, VILLA, HOTEL_SUITE…)")
          @RequestParam(required = false)
          String propertyType,
      @Parameter(description = "Filtre par type de transaction (SALE, RENT, SHORT_STAY…)")
          @RequestParam(required = false)
          String transactionType,
      @Parameter(description = "Prix minimum en XOF") @RequestParam(required = false)
          BigDecimal minPrice,
      @Parameter(description = "Prix maximum en XOF") @RequestParam(required = false)
          BigDecimal maxPrice,
      @Parameter(description = "Filtre par agence immobilière (UUID)")
          @RequestParam(required = false)
          UUID agencyId,
      @Parameter(description = "Filtre par propriétaire / bailleur (UUID)")
          @RequestParam(required = false)
          UUID ownerId,
      @Parameter(
              description =
                  "Filtre par hôtel – retourne uniquement les chambres/suites de cet hôtel (UUID)")
          @RequestParam(required = false)
          UUID hotelId,
      @PageableDefault(size = 20)
          @SortDefault.SortDefaults({
            @SortDefault(sort = "boosted", direction = Sort.Direction.DESC),
            @SortDefault(sort = "publishedAt", direction = Sort.Direction.DESC)
          })
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    if (status != PropertyStatus.PUBLISHED) {
      RoleGuard.requireAnyRole(
          requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    }
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_LIST_SUCCESS,
            propertyService.list(
                status,
                city,
                propertyType,
                transactionType,
                minPrice,
                maxPrice,
                agencyId,
                ownerId,
                hotelId,
                pageable)));
  }

  // ── Vues admin globales ────────────────────────────────────────

  @GetMapping("/admin/hotels")
  @Operation(
      summary = "Admin – Biens de tous les hôtels",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne tous les biens dont `hotel IS NOT NULL` (chambres, suites…), "
              + "toutes agences et tous hôtels confondus.\n\n"
              + "Filtres optionnels :\n"
              + "- `status` — statut du bien (défaut : tous)\n"
              + "- `hotelId` — restreindre à un hôtel précis",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée des biens hôteliers",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content)
  })
  @Parameters({
    @Parameter(
        name = "page",
        description = "Numéro de page (0-based)",
        example = "0",
        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
    @Parameter(
        name = "size",
        description = "Nombre de résultats par page (max 200)",
        example = "20",
        schema = @Schema(type = "integer", minimum = "1", maximum = "200", defaultValue = "20")),
    @Parameter(
        name = "sort",
        description = "Tri : champ,direction — ex: `createdAt,desc` · `price,asc`",
        example = "createdAt,desc",
        schema = @Schema(type = "string"))
  })
  public ResponseEntity<CustomResponse> listHotelProperties(
      @Parameter(description = "Filtre par statut (null = tous les statuts)")
          @RequestParam(required = false)
          PropertyStatus status,
      @Parameter(description = "Restreindre à un hôtel précis (UUID)")
          @RequestParam(required = false)
          UUID hotelId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_LIST_SUCCESS,
            propertyService.listHotelProperties(status, hotelId, pageable)));
  }

  @GetMapping("/admin/agencies")
  @Operation(
      summary = "Admin – Biens de toutes les agences",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retourne tous les biens dont `agency IS NOT NULL`, "
              + "toutes agences confondues.\n\n"
              + "Filtres optionnels :\n"
              + "- `status` — statut du bien (défaut : tous)\n"
              + "- `agencyId` — restreindre à une agence précise",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée des biens agence",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content)
  })
  @Parameters({
    @Parameter(
        name = "page",
        description = "Numéro de page (0-based)",
        example = "0",
        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
    @Parameter(
        name = "size",
        description = "Nombre de résultats par page (max 200)",
        example = "20",
        schema = @Schema(type = "integer", minimum = "1", maximum = "200", defaultValue = "20")),
    @Parameter(
        name = "sort",
        description = "Tri : champ,direction — ex: `createdAt,desc` · `price,asc`",
        example = "createdAt,desc",
        schema = @Schema(type = "string"))
  })
  public ResponseEntity<CustomResponse> listAgencyProperties(
      @Parameter(description = "Filtre par statut (null = tous les statuts)")
          @RequestParam(required = false)
          PropertyStatus status,
      @Parameter(description = "Restreindre à une agence précise (UUID)")
          @RequestParam(required = false)
          UUID agencyId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_LIST_SUCCESS,
            propertyService.listAgencyProperties(status, agencyId, pageable)));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'un bien (public)",
      description =
          "🌐 **Public** – Retourne le bien avec ses médias et documents. Aucune authentification requise.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Détail du bien",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyDetailResponse.class))),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getById(@PathVariable UUID id) throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_SUCCESS,
            propertyService.getById(id)));
  }

  // ── Mes biens (partenaire / agence) ────────────────────────────

  @GetMapping("/mine")
  @Operation(
      summary = "Mes biens",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Liste les biens de l'agence ou propriétaire connecté, tous statuts.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste paginée de mes biens",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content)
  })
  public ResponseEntity<CustomResponse> listMine(
      @RequestParam(required = false) PropertyStatus status,
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
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_LIST_SUCCESS,
            propertyService.listMine(caller.getSub(), status, pageable)));
  }

  // ── Création ───────────────────────────────────────────────────

  @PostMapping
  @Operation(
      summary = "Créer un bien (brouillon)",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Crée un bien en statut `DRAFT`. Le bien doit ensuite être soumis via `PATCH /{id}/submit` pour passer en modération.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Bien créé en DRAFT",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
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
              schema = @Schema(implementation = PropertyCreateRequest.class)))
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid PropertyCreateRequest request, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.PROPERTY_CREATE_SUCCESS,
                propertyService.create(caller.getSub(), request)));
  }

  // ── Mise à jour ────────────────────────────────────────────────

  @PutMapping("/{id}")
  @Operation(
      summary = "Mettre à jour un bien",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Mise à jour partielle d'un bien. Seuls les champs non-null sont modifiés. Le bien doit être en statut `DRAFT` ou `PENDING`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Bien mis à jour",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou bien appartenant à un autre propriétaire",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PropertyUpdateRequest.class)))
  public ResponseEntity<CustomResponse> update(
      @PathVariable UUID id,
      @RequestBody @Valid PropertyUpdateRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_UPDATE_SUCCESS,
            propertyService.update(id, caller.getSub(), request)));
  }

  @PatchMapping("/{id}/submit")
  @Operation(
      summary = "Soumettre un bien pour modération",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER`\n\n"
              + "Passe le statut de `DRAFT` à `PENDING`. Le bien sera examiné par un administrateur.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Bien soumis en PENDING",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Le bien n'est pas en statut DRAFT",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> submit(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_UPDATE_SUCCESS,
            propertyService.submit(id, caller.getSub())));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Archiver un bien",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Suppression logique : passe le statut à `ARCHIVED`. Le bien n'est plus visible publiquement.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Bien archivé", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant ou bien appartenant à un autre propriétaire",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> archive(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    propertyService.archive(id, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_DELETE_SUCCESS,
            null));
  }

  // ── Modération admin ───────────────────────────────────────────

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Mettre à jour le statut (modération admin)",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Publication, rejet ou changement de statut par un administrateur. "
              + "Le champ `rejectionReason` est **obligatoire** si `status = REJECTED`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Statut mis à jour",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Statut invalide ou motif de rejet manquant",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PropertyStatusUpdateRequest.class)))
  public ResponseEntity<CustomResponse> updateStatus(
      @PathVariable UUID id,
      @RequestBody @Valid PropertyStatusUpdateRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_STATUS_UPDATE_SUCCESS,
            propertyService.updateStatus(id, request)));
  }

  // ── Médias ─────────────────────────────────────────────────────

  @PostMapping(value = "/{id}/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload photo/vidéo d'un bien (atomique)",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Upload le fichier vers MinIO **ET** crée l'entrée `PropertyMedia` en une seule requête. "
              + "Recommandé pour mobile ou formulaires simples (≤ 50 Mo).\n\n"
              + "Pour les fichiers volumineux depuis le navigateur, utiliser "
              + "`GET /v1/storage/presign/property-media` puis `POST /v1/properties/{id}/media`.\n\n"
              + "**Formats acceptés :** JPEG, PNG, WEBP, MP4, MOV – max 10 Mo (images) / 100 Mo (vidéos)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Média uploadé et enregistré",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyMediaResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Fichier vide, type MIME non autorisé ou taille dépassée",
        content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> uploadMedia(
      @PathVariable UUID id,
      @Parameter(
              description = "Fichier image ou vidéo (JPEG, PNG, WEBP, MP4, MOV)",
              required = true,
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart("file")
          MultipartFile file,
      @Parameter(description = "Type de média : PHOTO, VIDEO, PLAN", example = "PHOTO")
          @RequestParam(defaultValue = "PHOTO")
          String mediaType,
      @Parameter(description = "Définir comme photo de couverture", example = "false")
          @RequestParam(defaultValue = "false")
          boolean cover,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.PROPERTY_MEDIA_ADD_SUCCESS,
                propertyService.uploadAndLinkMedia(id, caller.getSub(), file, mediaType, cover)));
  }

  @PostMapping("/{id}/media")
  @Operation(
      summary = "Lier un média déjà uploadé",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Enregistre un média déjà uploadé dans MinIO (bucket `properties-media`). "
              + "Utiliser après un upload via presigned URL.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Média lié au bien",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyMediaResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PropertyMediaAddRequest.class)))
  public ResponseEntity<CustomResponse> addMedia(
      @PathVariable UUID id,
      @RequestBody @Valid PropertyMediaAddRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.PROPERTY_MEDIA_ADD_SUCCESS,
                propertyService.addMedia(id, caller.getSub(), request)));
  }

  @GetMapping("/{id}/media")
  @Operation(
      summary = "Lister les médias d'un bien",
      description = "🌐 **Public** – Retourne la liste des médias (photos, vidéos, plans) du bien.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des médias",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyMediaResponse.class))),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> listMedia(@PathVariable UUID id) throws CustomException {
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_LIST_SUCCESS,
            propertyService.listMedia(id)));
  }

  @PatchMapping("/{id}/media/{mediaId}/cover")
  @Operation(
      summary = "Définir la photo de couverture",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Définit un média comme photo de couverture du bien. L'ancienne couverture est désactivée automatiquement.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Photo de couverture mise à jour",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyMediaResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Bien ou média introuvable",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> setCover(
      @PathVariable UUID id, @PathVariable UUID mediaId, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_MEDIA_COVER_SUCCESS,
            propertyService.setCover(id, mediaId, caller.getSub())));
  }

  @DeleteMapping("/{id}/media/{mediaId}")
  @Operation(
      summary = "Supprimer un média",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Supprime un média du bien et de MinIO.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Média supprimé", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Bien ou média introuvable",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> deleteMedia(
      @PathVariable UUID id, @PathVariable UUID mediaId, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    propertyService.deleteMedia(id, mediaId, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_MEDIA_DELETE_SUCCESS,
            null));
  }

  // ── Documents ──────────────────────────────────────────────────

  @PostMapping("/{id}/documents")
  @Operation(
      summary = "Ajouter un document légal",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Enregistre un document déjà uploadé dans MinIO (bucket `property-documents`). "
              + "Pour uploader d'abord, utiliser `GET /v1/storage/presign/property-document`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Document ajouté au bien",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyDocumentResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PropertyDocumentAddRequest.class)))
  public ResponseEntity<CustomResponse> addDocument(
      @PathVariable UUID id,
      @RequestBody @Valid PropertyDocumentAddRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.PROPERTY_DOCUMENT_ADD_SUCCESS,
                propertyService.addDocument(id, caller.getSub(), request)));
  }

  @GetMapping("/{id}/documents")
  @Operation(
      summary = "Lister les documents d'un bien",
      description =
          "🔑 **Authentifié** – Retourne les documents légaux du bien. "
              + "Les documents non publics sont filtrés selon le rôle de l'appelant.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Liste des documents",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyDocumentResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> listDocuments(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller = RoleGuard.requireAuthenticated(requestHeaderParser, httpRequest);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_GET_LIST_SUCCESS,
            propertyService.listDocuments(id, caller.getSub())));
  }

  @PatchMapping("/{id}/documents/{docId}/verify")
  @Operation(
      summary = "Vérifier un document (admin)",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Marque un document comme vérifié par l'administration. Un commentaire optionnel peut être ajouté.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Document vérifié",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyDocumentResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "Rôle insuffisant – ADMIN requis",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Bien ou document introuvable",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> verifyDocument(
      @PathVariable UUID id,
      @PathVariable UUID docId,
      @Parameter(description = "Commentaire optionnel de vérification")
          @RequestParam(required = false)
          String note,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_DOCUMENT_VERIFY_SUCCESS,
            propertyService.verifyDocument(id, docId, caller.getSub(), note)));
  }

  @DeleteMapping("/{id}/documents/{docId}")
  @Operation(
      summary = "Supprimer un document",
      description =
          "🏢 **Rôles requis :** `AGENCY` · `OWNER` · `AGENT` · `PARTNER` · `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Supprime un document du bien.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Document supprimé", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Bien ou document introuvable",
        content = @Content)
  })
  public ResponseEntity<CustomResponse> deleteDocument(
      @PathVariable UUID id, @PathVariable UUID docId, HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(
            requestHeaderParser,
            httpRequest,
            UserRole.PARTNER,
            UserRole.OWNER,
            UserRole.PARTNER,
            UserRole.PARTNER,
            UserRole.ADMIN,
            UserRole.SUPER_ADMIN);
    propertyService.deleteDocument(id, docId, caller.getSub());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_DOCUMENT_DELETE_SUCCESS,
            null));
  }

  // ── Boost & Expiration (Admin) ─────────────────────────────────

  @PatchMapping("/{id}/boost")
  @Operation(
      summary = "Activer / prolonger le boost d'une annonce",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Propulse l'annonce en tête des résultats de recherche pendant le nombre de jours indiqué. "
              + "Si un boost est déjà actif, la date d'expiration est recalculée depuis maintenant. "
              + "Le scheduler `PropertySchedulerJob` remet automatiquement `boosted = false` à l'expiration.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Boost activé",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PropertyBoostRequest.class)))
  public ResponseEntity<CustomResponse> boost(
      @PathVariable UUID id,
      @RequestBody @Valid PropertyBoostRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_STATUS_UPDATE_SUCCESS,
            propertyService.boost(id, request)));
  }

  @DeleteMapping("/{id}/boost")
  @Operation(
      summary = "Retirer le boost d'une annonce",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Désactive immédiatement le boost. L'annonce reste publiée mais n'est plus mise en avant.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Boost retiré",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> removeBoost(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_STATUS_UPDATE_SUCCESS,
            propertyService.removeBoost(id)));
  }

  @PatchMapping("/{id}/expiration")
  @Operation(
      summary = "Définir la date d'expiration automatique d'une annonce",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "À la date `expiresAt`, le scheduler archive automatiquement l'annonce (statut → `ARCHIVED`). "
              + "Utile pour les annonces à durée limitée ou les offres promotionnelles.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Expiration définie",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(responseCode = "400", description = "Date invalide ou passée", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PropertyExpirationRequest.class)))
  public ResponseEntity<CustomResponse> setExpiration(
      @PathVariable UUID id,
      @RequestBody @Valid PropertyExpirationRequest request,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_STATUS_UPDATE_SUCCESS,
            propertyService.setExpiration(id, request)));
  }

  @DeleteMapping("/{id}/expiration")
  @Operation(
      summary = "Supprimer la date d'expiration d'une annonce",
      description =
          "🛡 **Rôles requis :** `ADMIN` · `SUPER_ADMIN`\n\n"
              + "Retire la date d'expiration : l'annonce restera publiée indéfiniment jusqu'à archivage manuel.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Expiration supprimée",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PropertyResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant", content = @Content),
    @ApiResponse(responseCode = "404", description = "Bien introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> removeExpiration(
      @PathVariable UUID id, HttpServletRequest httpRequest) throws CustomException {
    RoleGuard.requireAnyRole(
        requestHeaderParser, httpRequest, UserRole.ADMIN, UserRole.SUPER_ADMIN);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.PROPERTY_STATUS_UPDATE_SUCCESS,
            propertyService.removeExpiration(id)));
  }
}
