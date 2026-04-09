package com.africa.ubaxplatform.storage.controller;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.storage.dto.PresignedUrlResponse;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion du stockage objet MinIO.
 *
 * <p>Permet au frontend d'obtenir une URL présignée PUT pour uploader un fichier directement vers
 * MinIO sans transiter par le backend. Pattern recommandé pour les fichiers volumineux (médias,
 * documents).
 *
 * <p><b>Workflow d'upload frontend :</b>
 *
 * <ol>
 *   <li>{@code GET /v1/storage/presign?bucket=properties-media&key=uuid.webp} → obtenir l'URL
 *   <li>Frontend fait un {@code PUT} direct vers MinIO avec l'URL présignée
 *   <li>Frontend notifie le backend avec l'{@code objectName} retourné
 * </ol>
 */
@RestController
@RequestMapping("/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage")
public class StorageController {

  private final MinioService minioService;
  private final RequestHeaderParser requestHeaderParser;

  // private static final int DEFAULT_EXPIRY_SECONDS = 900; // 15 minutes
  private static final int MAX_EXPIRY_SECONDS = 3600; // 1 heure

  /** Buckets accessibles via l'endpoint presign. Les autres sont rejetés (sécurité). */
  private static final Set<String> ALLOWED_BUCKETS =
      Set.of(
          "properties-media",
          "property-documents",
          "tenant-documents",
          "agencies-logos",
          "users-avatars",
          "ticket-attachments");

  /**
   * Génère une URL présignée PUT pour uploader un fichier directement vers MinIO.
   *
   * @param bucket bucket cible (doit être dans la liste des buckets autorisés)
   * @param key nom de l'objet dans le bucket (ex : {@code propertyId/uuid.webp}). Si absent, un
   *     UUID est généré automatiquement.
   * @param expires durée de validité en secondes (défaut : 900, max : 3600)
   */
  @GetMapping("/presign")
  @Operation(
      summary = "Générer une URL présignée PUT MinIO",
      description =
          "Retourne une URL PUT à usage unique permettant d'uploader un fichier directement vers "
              + "MinIO depuis le frontend. L'URL expire après `expires` secondes (défaut 15 min).",
      security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<CustomResponse> generatePresignedUrl(
      @Parameter(description = "Bucket cible", example = "properties-media") @RequestParam
          String bucket,
      @Parameter(description = "Nom de l'objet dans le bucket", example = "propertyId/uuid.webp")
          @RequestParam(required = false)
          String key,
      @Parameter(description = "Durée de validité en secondes (max 3600)", example = "900")
          @RequestParam(defaultValue = "900")
          int expires,
      HttpServletRequest request)
      throws CustomException {

    RoleGuard.requireAuthenticated(requestHeaderParser, request);

    if (!ALLOWED_BUCKETS.contains(bucket)) {
      throw new BadRequestException("Bucket non autorisé : " + bucket);
    }

    if (expires <= 0 || expires > MAX_EXPIRY_SECONDS) {
      throw new BadRequestException(
          "La durée de validité doit être comprise entre 1 et " + MAX_EXPIRY_SECONDS + " secondes");
    }

    String objectName = (key != null && !key.isBlank()) ? key : UUID.randomUUID().toString();

    PresignedUrlResponse response = minioService.generatePresignedUrl(bucket, objectName, expires);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "URL présignée générée avec succès",
            response));
  }
}
