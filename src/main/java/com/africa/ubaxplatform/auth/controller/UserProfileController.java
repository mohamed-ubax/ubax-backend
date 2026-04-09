package com.africa.ubaxplatform.auth.controller;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
public class UserProfileController {

  private final MinioService minioService;
  private final UserRepository userRepository;

  private static final String BUCKET_AVATARS = "users-avatars";
  private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 Mo
  private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

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
          "Formats acceptés : JPEG, PNG, WEBP – taille maximale : 5 Mo. "
              + "L'URL retournée est directement accessible si le bucket est public.",
      tags = {"Mobile"})
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CustomResponse> uploadAvatar(
      @RequestPart("file") MultipartFile file, JwtAuthenticationToken authentication) {

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
}
