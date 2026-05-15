package com.africa.ubaxplatform.technicien.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.technicien.dto.CreateTechnicienRequest;
import com.africa.ubaxplatform.technicien.dto.TechnicienResponse;
import com.africa.ubaxplatform.technicien.dto.UpdateTechnicienRequest;
import com.africa.ubaxplatform.technicien.service.interfaces.TechnicienService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/v1/technicians")
@RequiredArgsConstructor
@Tag(
    name = "Techniciens SAV",
    description =
        "🔧 **PARTNER (agence ou hôtel)** — Gestion des prestataires / techniciens externes.\n\n"
            + "Les techniciens n'ont pas de compte sur la plateforme. "
            + "Ils sont référencés pour faciliter l'assignation aux tickets SAV.")
public class TechnicienController {

  private final TechnicienService technicienService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping
  @Operation(
      summary = "Ajouter un technicien",
      description =
          "🔧 **Rôle requis :** `PARTNER` (agence ou hôtel).\n\n"
              + "Crée un profil de technicien externe rattaché à votre structure. "
              + "Les métiers disponibles sont dans `GET /v1/code-list/type/TECHNICIEN_PROFESSION`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Technicien créé"),
    @ApiResponse(responseCode = "400", description = "Données invalides"),
    @ApiResponse(responseCode = "403", description = "Pas PARTNER d'une structure")
  })
  public ResponseEntity<CustomResponse> create(
      @RequestBody @Valid CreateTechnicienRequest request,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    TechnicienResponse response = technicienService.create(authentication.getName(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.TECHNICIEN_CREATE_SUCCESS,
                response));
  }

  @GetMapping
  @Operation(
      summary = "Lister les techniciens de ma structure",
      description =
          "🔧 **Rôle requis :** `PARTNER`.\n\n"
              + "Retourne la liste paginée des techniciens rattachés à votre agence ou hôtel.\n\n"
              + "Filtre optionnel `?available=true` pour n'afficher que les disponibles.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste paginée retournée"),
    @ApiResponse(responseCode = "403", description = "Pas PARTNER d'une structure")
  })
  public ResponseEntity<CustomResponse> list(
      @RequestParam(required = false) Boolean available,
      @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC)
          Pageable pageable,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    Page<TechnicienResponse> page =
        technicienService.list(authentication.getName(), available, pageable);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TECHNICIEN_GET_LIST_SUCCESS,
            page));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Détail d'un technicien",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Technicien retourné"),
    @ApiResponse(responseCode = "403", description = "Technicien d'une autre structure"),
    @ApiResponse(responseCode = "404", description = "Technicien introuvable")
  })
  public ResponseEntity<CustomResponse> getById(
      @PathVariable UUID id, JwtAuthenticationToken authentication, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    TechnicienResponse response = technicienService.getById(authentication.getName(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TECHNICIEN_GET_SUCCESS,
            response));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Modifier un technicien",
      description = "🔧 **Rôle requis :** `PARTNER`. Seuls les champs fournis sont mis à jour.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Technicien mis à jour"),
    @ApiResponse(responseCode = "403", description = "Technicien d'une autre structure"),
    @ApiResponse(responseCode = "404", description = "Technicien introuvable")
  })
  public ResponseEntity<CustomResponse> update(
      @PathVariable UUID id,
      @RequestBody @Valid UpdateTechnicienRequest request,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    TechnicienResponse response = technicienService.update(authentication.getName(), id, request);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TECHNICIEN_UPDATE_SUCCESS,
            response));
  }

  @PatchMapping("/{id}/availability")
  @Operation(
      summary = "Basculer la disponibilité d'un technicien",
      description =
          "🔧 **Rôle requis :** `PARTNER`.\n\n" + "Alterne entre `disponible` et `indisponible`.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Disponibilité mise à jour")
  public ResponseEntity<CustomResponse> toggleAvailability(
      @PathVariable UUID id, JwtAuthenticationToken authentication, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    TechnicienResponse response =
        technicienService.toggleAvailability(authentication.getName(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TECHNICIEN_UPDATE_SUCCESS,
            response));
  }

  @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Uploader / remplacer la photo du technicien",
      description =
          "🔧 **Rôle requis :** `PARTNER`.\n\n"
              + "Upload ou remplace la photo du technicien. L'agent gère cette opération car les"
              + " techniciens n'ont pas de compte sur la plateforme.\n\n"
              + "**Formats acceptés :** JPEG, PNG, WEBP — max 5 Mo.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema =
                  @Schema(
                      type = "object",
                      requiredProperties = {"file"})))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Avatar mis à jour"),
    @ApiResponse(responseCode = "400", description = "Fichier vide, format ou taille invalide"),
    @ApiResponse(responseCode = "403", description = "Technicien d'une autre structure"),
    @ApiResponse(responseCode = "404", description = "Technicien introuvable")
  })
  public ResponseEntity<CustomResponse> uploadAvatar(
      @PathVariable UUID id,
      @RequestPart("file") MultipartFile file,
      JwtAuthenticationToken authentication,
      HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    TechnicienResponse response =
        technicienService.uploadAvatar(authentication.getName(), id, file);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TECHNICIEN_AVATAR_UPDATE_SUCCESS,
            response));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Supprimer un technicien",
      description = "🔧 **Rôle requis :** `PARTNER`. Suppression logique (soft delete).",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Technicien supprimé"),
    @ApiResponse(responseCode = "403", description = "Technicien d'une autre structure"),
    @ApiResponse(responseCode = "404", description = "Technicien introuvable")
  })
  public ResponseEntity<CustomResponse> delete(
      @PathVariable UUID id, JwtAuthenticationToken authentication, HttpServletRequest httpRequest)
      throws CustomException {
    RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.PARTNER);
    technicienService.delete(authentication.getName(), id);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.TECHNICIEN_DELETE_SUCCESS,
            null));
  }
}
