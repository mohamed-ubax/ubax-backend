package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.dto.UserResponse;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.mapper.UserMapper;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.UserRoleService;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Endpoints de gestion du profil utilisateur (avatar, informations personnelles). */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Mobile")
@Slf4j
public class UserProfileController {

  private final MinioService minioService;
  private final UserRepository userRepository;
  private final UserRoleService userRoleService;

  private static final String BUCKET_AVATARS = "users-avatars";
  private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 Mo
  private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

  // ── Get by keycloakId ──────────────────────────────────────────

  @GetMapping("/keycloak/{keycloakId}")
  @Operation(
      summary = "Récupérer son profil via le keycloakId",
      description =
          "🔑 **Authentifié** – Retourne le profil de l'utilisateur connecté. Le `keycloakId` fourni doit correspondre au token JWT du caller.",
      tags = {"Mobile"})
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Profil retourné",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "keycloakId ne correspond pas au token",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getByKeycloakId(
      @PathVariable String keycloakId, JwtAuthenticationToken authentication) {

    String callerKeycloakId = authentication.getName();
    log.info("callerKeycloakId : {}", callerKeycloakId);
    if (!callerKeycloakId.equals(keycloakId)) {
      throw new UnAuthorizedException("Accès refusé");
    }

    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));

    List<String> subRoles =
        userRoleService.getSubRoles(user.getId(), null).stream().map(r -> r.role()).toList();
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            UserMapper.toResponse(user, subRoles)));
  }

  // ── Get by userId ───────────────────────────────────────────────

  @GetMapping("/{userId}")
  @Operation(
      summary = "Récupérer son profil via l'userId interne",
      description =
          "🔑 **Authentifié** – Retourne le profil de l'utilisateur connecté. L'`userId` fourni doit correspondre au token JWT du caller.",
      tags = {"Mobile"})
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Profil retourné",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Token absent ou invalide",
        content = @Content),
    @ApiResponse(
        responseCode = "403",
        description = "userId ne correspond pas au token",
        content = @Content),
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable", content = @Content)
  })
  public ResponseEntity<CustomResponse> getByUserId(
      @PathVariable UUID userId, JwtAuthenticationToken authentication) {

    String callerKeycloakId = authentication.getName();

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));

    if (!callerKeycloakId.equals(user.getKeycloakId())) {
      throw new UnAuthorizedException("Accès refusé");
    }

    List<String> subRoles =
        userRoleService.getSubRoles(user.getId(), null).stream().map(r -> r.role()).toList();
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.USER_GET_SUCCESS,
            UserMapper.toResponse(user, subRoles)));
  }

  // ── Upload Avatar ───────────────────────────────────────────────

  /**
   * Upload ou remplace la photo de profil de l'utilisateur connecté.
   *
   * <p>L'objet MinIO est nommé {@code {keycloakId}.{ext}} : uploader une nouvelle image remplace
   * automatiquement l'ancienne dans le bucket {@code users-avatars}.
   *
   * @param file image au format JPEG, PNG ou WEBP (max 5 Mo)
   * @param authentication token JWT de l'utilisateur connecté
   */
  @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Uploader / remplacer la photo de profil",
      description =
          "🔑 **Authentifié** – Upload ou remplace la photo de profil de l'utilisateur connecté.\n\n"
              + "**Formats acceptés :** JPEG, PNG, WEBP – max 5 Mo.\n\n"
              + "L'objet MinIO est nommé `{keycloakId}.{ext}` : uploader une nouvelle image remplace automatiquement l'ancienne.",
      tags = {"Mobile"})
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Avatar mis à jour – `data` contient `avatarUrl`",
        content = @Content(mediaType = "application/json")),
    @ApiResponse(
        responseCode = "400",
        description = "Fichier vide, type MIME non autorisé ou taille dépassée",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Token absent ou invalide", content = @Content)
  })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema =
                  @Schema(
                      type = "object",
                      requiredProperties = {"file"})))
  public ResponseEntity<CustomResponse> uploadAvatar(
      @Parameter(
              description = "Image de profil (JPEG, PNG, WEBP – max 5 Mo)",
              required = true,
              content = @Content(schema = @Schema(type = "string", format = "binary")))
          @RequestPart("file")
          MultipartFile file,
      JwtAuthenticationToken authentication) {

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(
              new CustomResponse(
                  Constants.Message.BAD_REQUEST_BODY,
                  Constants.Status.BAD_REQUEST,
                  "Le fichier est vide",
                  null));
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
      return ResponseEntity.badRequest()
          .body(
              new CustomResponse(
                  Constants.Message.BAD_REQUEST_BODY,
                  Constants.Status.BAD_REQUEST,
                  "Format de fichier non supporté. Formats acceptés : JPEG, PNG, WEBP",
                  null));
    }

    if (file.getSize() > MAX_SIZE_BYTES) {
      return ResponseEntity.badRequest()
          .body(
              new CustomResponse(
                  Constants.Message.BAD_REQUEST_BODY,
                  Constants.Status.BAD_REQUEST,
                  "La taille du fichier ne doit pas dépasser 5 Mo",
                  null));
    }

    String keycloakId = authentication.getName();
    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));

    String extension =
        switch (contentType) {
          case "image/jpeg" -> ".jpg";
          case "image/png" -> ".png";
          default -> ".webp";
        };

    String objectName = keycloakId + extension;

    // Supprimer l'ancien fichier MinIO si l'extension a changé (évite les fichiers orphelins)
    String existingAvatarUrl = user.getAvatarUrl();
    if (existingAvatarUrl != null) {
      String existingObjectName = extractObjectName(existingAvatarUrl);
      if (existingObjectName != null && !existingObjectName.equals(objectName)) {
        minioService.deleteFile(BUCKET_AVATARS, existingObjectName);
      }
    }

    String avatarUrl;
    try {
      avatarUrl =
          minioService.uploadFile(
              BUCKET_AVATARS, objectName, file.getInputStream(), file.getSize(), contentType);
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(
              new CustomResponse(
                  Constants.Message.SERVER_ERROR_BODY,
                  Constants.Status.INTERNAL_SERVER_ERROR,
                  "Erreur lors de l'upload de l'image : " + e.getMessage(),
                  null));
    }

    user.setAvatarUrl(avatarUrl);
    userRepository.save(user);

    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            "Photo de profil mise à jour",
            Map.of("avatarUrl", avatarUrl)));
  }

  /**
   * Extrait l'objectName MinIO depuis une URL stockée (formats anciens et nouveaux).
   *
   * <p>Exemples : - {@code http://localhost:9000/users-avatars/abc.jpg} → {@code abc.jpg} - {@code
   * /users-avatars/abc.jpg} → {@code abc.jpg} - {@code users-avatars/abc.jpg} → {@code abc.jpg}
   */
  private String extractObjectName(String avatarUrl) {
    if (avatarUrl == null) return null;
    String path =
        avatarUrl.contains("://") ? avatarUrl.replaceFirst("https?://[^/]+/", "") : avatarUrl;
    if (path.startsWith("/")) path = path.substring(1);
    int slash = path.indexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : null;
  }
}
