package com.africa.ubaxplatform.favorite.controller;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.response.CustomResponse;
import com.africa.ubaxplatform.common.util.RequestHeaderParser;
import com.africa.ubaxplatform.common.util.RoleGuard;
import com.africa.ubaxplatform.favorite.service.interfaces.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favoris")
public class FavoriteController {

  private final FavoriteService favoriteService;
  private final RequestHeaderParser requestHeaderParser;

  @PostMapping("/{propertyId}")
  @Operation(
      summary = "Ajouter un bien aux favoris",
      description =
          "Ajoute le bien à la liste des favoris du CLIENT connecté. "
              + "Idempotent : si le bien est déjà en favori, retourne le favori existant sans erreur.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Bien ajouté aux favoris"),
    @ApiResponse(responseCode = "404", description = "Bien introuvable"),
    @ApiResponse(responseCode = "401", description = "Non authentifié")
  })
  public ResponseEntity<CustomResponse> add(
      @PathVariable UUID propertyId, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CustomResponse(
                Constants.Message.SUCCESS_BODY,
                Constants.Status.CREATED,
                ResponseMessageConstants.FAVORITE_ADD_SUCCESS,
                favoriteService.add(caller.getSub(), propertyId)));
  }

  @DeleteMapping("/{propertyId}")
  @Operation(
      summary = "Retirer un bien des favoris",
      description = "Supprime le bien de la liste des favoris du CLIENT connecté.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Bien retiré des favoris"),
    @ApiResponse(responseCode = "404", description = "Favori introuvable"),
    @ApiResponse(responseCode = "401", description = "Non authentifié")
  })
  public ResponseEntity<CustomResponse> remove(
      @PathVariable UUID propertyId, HttpServletRequest httpRequest) throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);
    favoriteService.remove(caller.getSub(), propertyId);
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.FAVORITE_REMOVE_SUCCESS,
            null));
  }

  @GetMapping("/mine")
  @Operation(
      summary = "Mes biens favoris",
      description =
          "Retourne la liste paginée des biens mis en favoris par le CLIENT connecté, "
              + "triée du plus récemment ajouté au plus ancien.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Liste paginée des biens favoris")
  public ResponseEntity<CustomResponse> listMine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest httpRequest)
      throws CustomException {
    RequestUser caller =
        RoleGuard.requireAnyRole(requestHeaderParser, httpRequest, UserRole.CLIENT, UserRole.OWNER);
    var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ResponseEntity.ok(
        new CustomResponse(
            Constants.Message.SUCCESS_BODY,
            Constants.Status.OK,
            ResponseMessageConstants.FAVORITE_GET_LIST_SUCCESS,
            favoriteService.listMine(caller.getSub(), pageable)));
  }
}
