package com.africa.ubaxplatform.storage.controller;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.storage.dto.PresignedUrlResponse;
import com.africa.ubaxplatform.storage.dto.StorageUploadResponse;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de gestion du stockage objet MinIO.
 *
 * <h2>Deux stratégies d'upload</h2>
 *
 * <h3>1 – Upload direct multipart (recommandé mobile, fichiers ≤ 50 Mo)</h3>
 *
 * <pre>
 * POST /v1/storage/upload?bucket=properties-media
 *   form-data : file=&lt;binaire&gt;
 *   → { fileUrl, objectName, bucket, fileName, fileSize, mimeType }
 * </pre>
 *
 * Puis appeler l'API métier avec {@code fileUrl} pour lier le fichier à l'entité :
 *
 * <pre>
 * POST /v1/properties/{id}/media  →  { fileUrl, mediaType, cover, … }
 * </pre>
 *
 * <h3>2 – Presigned URL PUT (recommandé web, fichiers volumineux)</h3>
 *
 * <pre>
 * GET /v1/storage/presign?bucket=properties-media&amp;key=folder/uuid.webp
 *   → { uploadUrl, publicUrl, objectName, bucket, expiresInSeconds }
 *
 * PUT {uploadUrl}  ← upload direct navigateur → MinIO (sans passer par le backend)
 *
 * POST /v1/properties/{id}/media  →  { fileUrl: publicUrl, … }
 * </pre>
 *
 * <h3>Buckets autorisés</h3>
 *
 * <ul>
 *   <li>{@code properties-media} – photos / vidéos / plans des biens
 *   <li>{@code property-documents} – documents légaux des biens
 *   <li>{@code tenant-documents} – pièces justificatives des locataires
 *   <li>{@code agencies-logos} – logos des agences
 *   <li>{@code users-avatars} – photos de profil
 *   <li>{@code ticket-attachments} – pièces jointes des tickets
 *   <li>{@code partner-documents} – documents légaux des partenaires
 * </ul>
 */
@RestController
@RequestMapping("/v1/storage")
@RequiredArgsConstructor
@Tag(
    name = "Storage",
    description = "Upload de fichiers vers MinIO (presigned URL ou multipart direct)")
public class StorageController {

  private final MinioService minioService;
  private final RequestHeaderParser requestHeaderParser;

  private static final int MAX_EXPIRY_SECONDS = 3600;

  private static final Set<String> ALLOWED_BUCKETS =
      Set.of(
          "properties-media",
          "property-documents",
          "tenant-documents",
          "agencies-logos",
          "users-avatars",
          "ticket-attachments",
          "partner-documents");

  // Taille max par type de bucket
  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024; // 10 Mo
  private static final long MAX_DOC_BYTES = 20L * 1024 * 1024; // 20 Mo
  private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024; // 100 Mo

  // Types MIME acceptés par bucket
  private static final Set<String> MIME_IMAGES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
  private static final Set<String> MIME_DOCS =
      Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");
  private static final Set<String> MIME_MEDIA =
      Set.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime", "video/mpeg");

  // ── 1. Upload direct multipart ──────────────────────────────────

  /**
   * Upload un fichier directement vers MinIO et retourne son URL publique.
   *
   * <p>Le fichier transite par le backend. Recommandé pour les fichiers ≤ 50 Mo ou les clients
   * mobiles. Pour les fichiers volumineux depuis un navigateur, préférer la presigned URL (section
   * 2).
   *
   * <p>Après cet appel, il faut appeler l'API métier correspondante pour lier l'URL à l'entité :
   *
   * <ul>
   *   <li>Photo bien → {@code POST /v1/properties/{id}/media}
   *   <li>Document bien → {@code POST /v1/properties/{id}/documents}
   *   <li>Document locataire → {@code PATCH /v1/tenants/profile} (champ {@code idDocumentUrl},
   *       etc.)
   *   <li>Logo agence → {@code POST /v1/storage/upload/agency-logo} (lien atomique)
   * </ul>
   */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload direct multipart vers MinIO",
      description =
          "🔑 **Authentifié** – Envoie le fichier au backend qui le transfère à MinIO. "
              + "Retourne l'URL publique finale à utiliser dans l'API métier pour lier le fichier à une entité.\n\n"
              + "**Après upload :** appeler l'API métier correspondante avec `fileUrl` :\n"
              + "- Photo bien → `POST /v1/properties/{id}/media`\n"
              + "- Document bien → `POST /v1/properties/{id}/documents`\n"
              + "- Logo agence → `POST /v1/storage/upload/agency-logo` (lien atomique recommandé)",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Fichier uploadé – `data` contient `fileUrl`, `objectName`, `bucket`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StorageUploadResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Fichier vide, bucket non autorisé, type MIME invalide ou taille dépassée",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  @RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema =
                  @Schema(
                      type = "object",
                      requiredProperties = {"file", "bucket"})))
  public ResponseEntity<CustomResponse> upload(
      @Parameter(
              description = "Fichier à uploader",
              required = true,
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart("file")
          MultipartFile file,
      @Parameter(description = "Bucket cible", example = "properties-media") @RequestParam
          String bucket,
      @Parameter(description = "Sous-dossier optionnel dans le bucket", example = "propertyId")
          @RequestParam(required = false)
          String folder,
      HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, httpRequest);
    validateBucket(bucket);
    validateFile(file, bucket);

    String objectName = buildObjectName(folder, file.getOriginalFilename(), file.getContentType());

    String fileUrl;
    try {
      fileUrl =
          minioService.uploadFile(
              bucket, objectName, file.getInputStream(), file.getSize(), file.getContentType());
    } catch (IOException e) {
      throw new BadRequestException("Impossible de lire le fichier : " + e.getMessage());
    }

    StorageUploadResponse response =
        new StorageUploadResponse(
            fileUrl,
            objectName,
            bucket,
            file.getOriginalFilename(),
            file.getSize(),
            file.getContentType());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                "Fichier uploadé avec succès",
                response));
  }

  // ── 2. Upload avec lien atomique – contextes spécifiques ───────

  /**
   * Upload le logo d'une agence et le lie immédiatement au profil agence.
   *
   * <p>Le lien est atomique : le logo est enregistré en DB dans la même transaction que l'upload.
   * Pas de second appel nécessaire.
   */
  @PostMapping(value = "/upload/agency-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload du logo agence (atomique)",
      description =
          "🔑 **Authentifié** – Uploade le logo et le lie immédiatement au profil agence de l'utilisateur connecté. "
              + "Formats : JPEG, PNG, WEBP – max 10 Mo.\n\n"
              + "Lien atomique : pas de second appel nécessaire.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Logo uploadé – `data` contient `fileUrl`, `objectName`, `bucket`",
        content = @Content(mediaType = "application/json")),
    @ApiResponse(
        responseCode = "400",
        description = "Fichier vide, type MIME invalide ou taille dépassée",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  @RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema =
                  @Schema(
                      type = "object",
                      requiredProperties = {"file"})))
  public ResponseEntity<CustomResponse> uploadAgencyLogo(
      @Parameter(
              description = "Logo de l'agence (JPEG, PNG, WEBP – max 10 Mo)",
              required = true,
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart("file")
          MultipartFile file,
      HttpServletRequest httpRequest)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, httpRequest);
    validateFile(file, "agencies-logos");

    String objectName = buildObjectName(null, file.getOriginalFilename(), file.getContentType());
    String fileUrl;
    try {
      fileUrl =
          minioService.uploadFile(
              "agencies-logos",
              objectName,
              file.getInputStream(),
              file.getSize(),
              file.getContentType());
    } catch (IOException e) {
      throw new BadRequestException("Impossible de lire le fichier : " + e.getMessage());
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                "Logo agence uploadé avec succès",
                Map.of(
                    "fileUrl", fileUrl,
                    "objectName", objectName,
                    "bucket", "agencies-logos")));
  }

  // ── 3. Presigned URL générique ──────────────────────────────────

  /**
   * Génère une URL présignée PUT pour uploader un fichier directement vers MinIO depuis le
   * frontend, <b>sans transiter par le backend</b>.
   *
   * <p><b>Workflow complet :</b>
   *
   * <ol>
   *   <li>Appeler cet endpoint pour obtenir {@code uploadUrl} et {@code publicUrl}
   *   <li>Faire un {@code PUT {uploadUrl}} depuis le navigateur/mobile avec le binaire du fichier
   *   <li>Appeler l'API métier avec {@code publicUrl} pour lier à l'entité :
   *       <ul>
   *         <li>{@code POST /v1/properties/{id}/media} → champ {@code fileUrl}
   *         <li>{@code POST /v1/properties/{id}/documents} → champ {@code fileUrl}
   *         <li>{@code PATCH /v1/tenants/profile} → champs {@code idDocumentUrl}, {@code
   *             incomeProofUrl}, etc.
   *       </ul>
   * </ol>
   */
  @GetMapping("/presign")
  @Operation(
      summary = "Générer une URL présignée PUT MinIO",
      description =
          "🔑 **Authentifié** – Retourne une URL PUT à usage unique. Le frontend l'utilise pour uploader **directement**"
              + " vers MinIO (sans passer par le backend). Après upload, appeler l'API "
              + "métier avec `publicUrl` pour lier le fichier à son entité.\n\n"
              + "**Workflow :** `GET /presign` → `PUT {uploadUrl}` → API métier avec `publicUrl`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "URL présignée générée – `data` contient `uploadUrl`, `publicUrl`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Bucket non autorisé ou durée invalide",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  public ResponseEntity<CustomResponse> generatePresignedUrl(
      @Parameter(description = "Bucket cible", example = "properties-media") @RequestParam
          String bucket,
      @Parameter(description = "Nom de l'objet (ex: propertyId/uuid.webp). UUID auto si absent.")
          @RequestParam(required = false)
          String key,
      @Parameter(description = "Durée de validité en secondes (max 3600)", example = "900")
          @RequestParam(defaultValue = "900")
          int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);
    validateBucket(bucket);

    if (expires <= 0 || expires > MAX_EXPIRY_SECONDS) {
      throw new BadRequestException(
          "La durée de validité doit être entre 1 et " + MAX_EXPIRY_SECONDS + " secondes");
    }

    String objectName = (key != null && !key.isBlank()) ? key : UUID.randomUUID().toString();
    PresignedUrlResponse response = minioService.generatePresignedUrl(bucket, objectName, expires);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "URL présignée générée – utilisez uploadUrl pour uploader, puis publicUrl pour lier à l'entité",
            response));
  }

  // ── 4. Presigned URL contextuelles (avec clé structurée) ───────

  /**
   * Génère une URL présignée PUT structurée pour un média de bien immobilier.
   *
   * <p>La clé MinIO est automatiquement structurée : {@code {propertyId}/{uuid}.{ext}}. Le {@code
   * publicUrl} retourné est à passer dans {@code POST /v1/properties/{id}/media → fileUrl}.
   */
  @GetMapping("/presign/property-media")
  @Operation(
      summary = "Presigned URL – média de bien",
      description =
          "🔑 **Authentifié** – Génère une presigned URL pour uploader un média (photo/vidéo/plan) d'un bien. "
              + "Clé auto-structurée : `{propertyId}/{uuid}.{ext}`. "
              + "Après upload, appeler `POST /v1/properties/{id}/media` avec `publicUrl`.\n\n"
              + "**Types MIME acceptés :** `image/jpeg`, `image/png`, `image/webp`, `video/mp4`, `video/quicktime`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "URL présignée – `data` contient `uploadUrl`, `publicUrl`, `objectName`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class))),
    @ApiResponse(responseCode = "400", description = "Type MIME non autorisé", content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  public ResponseEntity<CustomResponse> presignPropertyMedia(
      @Parameter(description = "UUID du bien", required = true) @RequestParam UUID propertyId,
      @Parameter(description = "Type MIME du fichier", example = "image/jpeg", required = true)
          @RequestParam
          String contentType,
      @Parameter(description = "Durée de validité en secondes (max 3600)", example = "900")
          @RequestParam(defaultValue = "900")
          int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);
    validateMime(contentType, MIME_MEDIA, "properties-media");

    String objectName = propertyId + "/" + UUID.randomUUID() + extFor(contentType);
    PresignedUrlResponse response =
        minioService.generatePresignedUrl(
            "properties-media", objectName, Math.min(expires, MAX_EXPIRY_SECONDS));

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Presigned URL média bien – uploadez via uploadUrl puis liez avec POST /v1/properties/{id}/media",
            response));
  }

  /**
   * Génère une URL présignée PUT structurée pour un document légal d'un bien.
   *
   * <p>La clé MinIO : {@code {propertyId}/doc-{uuid}.{ext}}. Le {@code publicUrl} est à passer dans
   * {@code POST /v1/properties/{id}/documents → fileUrl}.
   */
  @GetMapping("/presign/property-document")
  @Operation(
      summary = "Presigned URL – document légal de bien",
      description =
          "🔑 **Authentifié** – Génère une presigned URL pour uploader un document légal (PDF, image). "
              + "Après upload, appeler `POST /v1/properties/{id}/documents` avec `publicUrl`.\n\n"
              + "**Types MIME acceptés :** `application/pdf`, `image/jpeg`, `image/png`, `image/webp`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "URL présignée – `data` contient `uploadUrl`, `publicUrl`, `objectName`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class))),
    @ApiResponse(responseCode = "400", description = "Type MIME non autorisé", content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  public ResponseEntity<CustomResponse> presignPropertyDocument(
      @Parameter(description = "UUID du bien", required = true) @RequestParam UUID propertyId,
      @Parameter(description = "Type MIME du fichier", example = "application/pdf", required = true)
          @RequestParam
          String contentType,
      @RequestParam(defaultValue = "900") int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);
    validateMime(contentType, MIME_DOCS, "property-documents");

    String objectName = propertyId + "/doc-" + UUID.randomUUID() + extFor(contentType);
    PresignedUrlResponse response =
        minioService.generatePresignedUrl(
            "property-documents", objectName, Math.min(expires, MAX_EXPIRY_SECONDS));

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Presigned URL document bien – uploadez via uploadUrl puis liez avec POST /v1/properties/{id}/documents",
            response));
  }

  /**
   * Génère une URL présignée PUT pour un document locataire (KYC).
   *
   * <p>Clé MinIO : {@code tenant-{uuid}.{ext}}. L'URL est à passer dans {@code PATCH
   * /v1/tenants/profile} sur le champ correspondant ({@code idDocumentUrl}, {@code incomeProofUrl},
   * {@code addressProofUrl}).
   */
  @GetMapping("/presign/tenant-document")
  @Operation(
      summary = "Presigned URL – document locataire (KYC)",
      description =
          "🔑 **Authentifié** – Génère une presigned URL pour un document KYC du locataire (CNI, justificatif revenu…). "
              + "Après upload, appeler `PATCH /v1/tenants/profile` avec le champ approprié.\n\n"
              + "**Champs cibles :** `idDocumentUrl`, `incomeProofUrl`, `addressProofUrl`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "URL présignée – `data` contient `uploadUrl`, `publicUrl`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class))),
    @ApiResponse(responseCode = "400", description = "Type MIME non autorisé", content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  public ResponseEntity<CustomResponse> presignTenantDocument(
      @Parameter(description = "Type MIME du fichier", example = "application/pdf", required = true)
          @RequestParam
          String contentType,
      @RequestParam(defaultValue = "900") int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);
    validateMime(contentType, MIME_DOCS, "tenant-documents");

    String objectName = "tenant-" + UUID.randomUUID() + extFor(contentType);
    PresignedUrlResponse response =
        minioService.generatePresignedUrl(
            "tenant-documents", objectName, Math.min(expires, MAX_EXPIRY_SECONDS));

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Presigned URL doc locataire – uploadez via uploadUrl puis liez via PATCH /v1/tenants/profile",
            response));
  }

  /**
   * Génère une URL présignée PUT pour le logo d'une agence.
   *
   * <p>Si vous souhaitez un lien atomique (upload + enregistrement en DB en une seule requête),
   * utilisez plutôt {@code POST /v1/storage/upload/agency-logo}.
   */
  @GetMapping("/presign/agency-logo")
  @Operation(
      summary = "Presigned URL – logo agence",
      description =
          "🔑 **Authentifié** – Génère une presigned URL pour uploader le logo d'une agence. "
              + "Pour un lien automatique en DB, préférer `POST /v1/storage/upload/agency-logo`.\n\n"
              + "**Types MIME acceptés :** `image/jpeg`, `image/png`, `image/webp`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "URL présignée – `data` contient `uploadUrl`, `publicUrl`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class))),
    @ApiResponse(responseCode = "400", description = "Type MIME non autorisé", content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  public ResponseEntity<CustomResponse> presignAgencyLogo(
      @Parameter(description = "Type MIME de l'image", example = "image/png", required = true)
          @RequestParam
          String contentType,
      @RequestParam(defaultValue = "900") int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);
    validateMime(contentType, MIME_IMAGES, "agencies-logos");

    String objectName = "logo-" + UUID.randomUUID() + extFor(contentType);
    PresignedUrlResponse response =
        minioService.generatePresignedUrl(
            "agencies-logos", objectName, Math.min(expires, MAX_EXPIRY_SECONDS));

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Presigned URL logo agence générée",
            response));
  }

  /**
   * Génère une URL présignée PUT pour une pièce jointe de ticket.
   *
   * <p>Clé MinIO : {@code ticket-{ticketId}/{uuid}.{ext}}. L'URL est à passer dans {@code POST
   * /v1/tickets/{id}/attachments}.
   */
  @GetMapping("/presign/ticket-attachment")
  @Operation(
      summary = "Presigned URL – pièce jointe ticket",
      description =
          "🔑 **Authentifié** – Génère une presigned URL pour uploader une pièce jointe de ticket. "
              + "Après upload, appeler `POST /v1/tickets/{id}/attachments` avec `publicUrl`.\n\n"
              + "**Types MIME acceptés :** `image/jpeg`, `image/png`, `image/webp`, `video/mp4`",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "URL présignée – `data` contient `uploadUrl`, `publicUrl`",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlResponse.class))),
    @ApiResponse(responseCode = "400", description = "Type MIME non autorisé", content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  public ResponseEntity<CustomResponse> presignTicketAttachment(
      @Parameter(description = "UUID du ticket", required = true) @RequestParam UUID ticketId,
      @Parameter(description = "Type MIME du fichier", example = "image/jpeg", required = true)
          @RequestParam
          String contentType,
      @RequestParam(defaultValue = "900") int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);
    validateMime(contentType, MIME_MEDIA, "ticket-attachments");

    String objectName = "ticket-" + ticketId + "/" + UUID.randomUUID() + extFor(contentType);
    PresignedUrlResponse response =
        minioService.generatePresignedUrl(
            "ticket-attachments", objectName, Math.min(expires, MAX_EXPIRY_SECONDS));

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Presigned URL pièce jointe ticket – uploadez via uploadUrl puis liez via POST /v1/tickets/{id}/attachments",
            response));
  }

  // ── Helpers de validation ───────────────────────────────────────

  private void validateBucket(String bucket) throws BadRequestException {
    if (!ALLOWED_BUCKETS.contains(bucket)) {
      throw new BadRequestException(
          "Bucket non autorisé : " + bucket + ". Buckets acceptés : " + ALLOWED_BUCKETS);
    }
  }

  private void validateFile(MultipartFile file, String bucket) throws BadRequestException {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Le fichier est vide ou absent");
    }

    String mime = file.getContentType();
    if (mime == null) {
      throw new BadRequestException("Type MIME du fichier non détecté");
    }

    Set<String> allowedMimes = allowedMimesFor(bucket);
    if (!allowedMimes.contains(mime)) {
      throw new BadRequestException(
          "Type de fichier non autorisé pour le bucket '"
              + bucket
              + "' : "
              + mime
              + ". Types acceptés : "
              + allowedMimes);
    }

    long maxBytes = maxSizeFor(bucket, mime);
    if (file.getSize() > maxBytes) {
      throw new BadRequestException(
          "Fichier trop volumineux ("
              + (file.getSize() / 1024 / 1024)
              + " Mo). "
              + "Taille max : "
              + (maxBytes / 1024 / 1024)
              + " Mo");
    }
  }

  private void validateMime(String contentType, Set<String> allowed, String bucket)
      throws BadRequestException {
    if (!allowed.contains(contentType)) {
      throw new BadRequestException(
          "Type MIME non autorisé pour "
              + bucket
              + " : "
              + contentType
              + ". Acceptés : "
              + allowed);
    }
  }

  private Set<String> allowedMimesFor(String bucket) {
    return switch (bucket) {
      case "properties-media", "ticket-attachments" -> MIME_MEDIA;
      case "agencies-logos", "users-avatars" -> MIME_IMAGES;
      default -> MIME_DOCS; // property-documents, tenant-documents, partner-documents
    };
  }

  private long maxSizeFor(String bucket, String mime) {
    if (mime.startsWith("video/")) return MAX_VIDEO_BYTES;
    if (bucket.contains("document") || bucket.equals("partner-documents")) return MAX_DOC_BYTES;
    return MAX_IMAGE_BYTES;
  }

  private String buildObjectName(String folder, String originalName, String contentType) {
    String uuid = UUID.randomUUID().toString();
    String ext = extFor(contentType);
    return (folder != null && !folder.isBlank()) ? folder + "/" + uuid + ext : uuid + ext;
  }

  private String extFor(String contentType) {
    if (contentType == null) return ".bin";
    return switch (contentType) {
      case "application/pdf" -> ".pdf";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      case "video/mp4" -> ".mp4";
      case "video/quicktime" -> ".mov";
      case "video/mpeg" -> ".mpeg";
      default -> ".jpg";
    };
  }
}
