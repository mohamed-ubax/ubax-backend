package com.africa.ubaxplatform.common.exception;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.response.CustomResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<CustomResponse> handleValidationException(
      MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + " : " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
    log.warn("[VALIDATION] Champs invalides → {}", message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY, Constants.Status.BAD_REQUEST, message, null));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<CustomResponse> handleMissingParam(
      MissingServletRequestParameterException e) {
    log.warn(
        "[MISSING_PARAM] Paramètre '{}' ({}) absent de la requête",
        e.getParameterName(),
        e.getParameterType());
    String message =
        "Paramètre obligatoire manquant : '"
            + e.getParameterName()
            + "' (type : "
            + e.getParameterType()
            + ")";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY, Constants.Status.BAD_REQUEST, message, null));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<CustomResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    String expected = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "inconnu";
    log.warn(
        "[TYPE_MISMATCH] Paramètre '{}' → valeur '{}' ne correspond pas au type attendu {}",
        e.getName(),
        e.getValue(),
        expected);
    String message =
        "Valeur invalide pour '"
            + e.getName()
            + "' : '"
            + e.getValue()
            + "' — attendu : "
            + expected;
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY, Constants.Status.BAD_REQUEST, message, null));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<CustomResponse> handleMalformedJson(HttpMessageNotReadableException e) {
    log.warn("[MALFORMED_JSON] Corps de la requête illisible : {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY,
                Constants.Status.BAD_REQUEST,
                "Corps JSON malformé ou type de contenu incorrect",
                null));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<CustomResponse> handleNoResource(NoResourceFoundException e) {
    log.warn("[NOT_FOUND] Route inexistante : {} {}", e.getHttpMethod(), e.getResourcePath());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            new CustomResponse(
                Constants.Message.NOT_FOUND_BODY,
                Constants.Status.NOT_FOUND,
                "Route introuvable : " + e.getResourcePath(),
                null));
  }

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<CustomResponse> handleException(CustomException e) {
    log.error("Erreur code: {}", e.getCodeMessage());
    return ResponseEntity.status(determineHttpStatus(e.getException())).body(getResponse(e));
  }

  @ExceptionHandler(TokenRetrievalException.class)
  public ResponseEntity<CustomResponse> handleTokenRetrievalException(TokenRetrievalException e) {
    log.error("Token retrieval error: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            new CustomResponse(
                Constants.Message.UNAUTHORIZED_BODY,
                Constants.Status.UNAUTHORIZED,
                "Échec de l'authentification - Token non disponible",
                null));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<CustomResponse> handleNotFoundException(NotFoundException e) {
    log.error("Not found error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            new CustomResponse(
                Constants.Message.NOT_FOUND_BODY,
                Constants.Status.NOT_FOUND,
                e.getMessage(),
                null));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<CustomResponse> handleConflictException(ConflictException e) {
    log.warn("Conflict error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            new CustomResponse(
                Constants.Message.CONFLICT_BODY, Constants.Status.CONFLICT, e.getMessage(), null));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<CustomResponse> handleBadRequestException(BadRequestException e) {
    log.warn("Bad request error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY,
                Constants.Status.BAD_REQUEST,
                e.getMessage(),
                null));
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<CustomResponse> handleStorageException(StorageException e) {
    log.error("Storage error: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new CustomResponse(
                Constants.Message.SERVER_ERROR_BODY,
                Constants.Status.INTERNAL_SERVER_ERROR,
                e.getMessage(),
                null));
  }

  @ExceptionHandler(InvalidDataAccessApiUsageException.class)
  public ResponseEntity<CustomResponse> handleInvalidSort(InvalidDataAccessApiUsageException e) {
    log.warn(
        "[INVALID_SORT] Expression de tri invalide : {}", e.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY,
                Constants.Status.BAD_REQUEST,
                "Paramètre 'sort' invalide — utilisez un nom de champ valide (ex: createdAt, price)",
                null));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<CustomResponse> maxUploadSizeExceeded(MaxUploadSizeExceededException e) {
    log.info("error: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new CustomResponse(
                Constants.Message.BAD_REQUEST_BODY,
                Constants.Status.BAD_REQUEST,
                "Max size 50MB",
                null));
  }

  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<CustomResponse> handleHttpClientError(HttpClientErrorException e) {
    log.error("Keycloak HTTP error {}: {}", e.getStatusCode(), e.getMessage());
    HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
    if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
    String body =
        switch (status) {
          case UNAUTHORIZED -> Constants.Message.UNAUTHORIZED_BODY;
          case FORBIDDEN -> "FORBIDDEN";
          case NOT_FOUND -> Constants.Message.NOT_FOUND_BODY;
          case BAD_REQUEST -> Constants.Message.BAD_REQUEST_BODY;
          default -> Constants.Message.SERVER_ERROR_BODY;
        };
    return ResponseEntity.status(status)
        .body(new CustomResponse(body, status.value(), e.getMessage(), null));
  }

  private HttpStatus determineHttpStatus(Exception e) {
    if (e == null) return HttpStatus.INTERNAL_SERVER_ERROR;
    log.error(e.getClass().getName(), e);
    if (e instanceof ConflictException || e instanceof EntityExistsException)
      return HttpStatus.CONFLICT;
    if (e instanceof BadRequestException
        || e instanceof IllegalArgumentException
        || e instanceof DataIntegrityViolationException) return HttpStatus.BAD_REQUEST;
    if (e instanceof UnAuthorizedException) return HttpStatus.FORBIDDEN;
    if (e instanceof EntityNotFoundException || e instanceof NotFoundException)
      return HttpStatus.NOT_FOUND;
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private CustomResponse getResponse(CustomException e) {
    if (e.getException() instanceof ConflictException
        || e.getException() instanceof EntityExistsException)
      return new CustomResponse(
          Constants.Message.CONFLICT_BODY, Constants.Status.CONFLICT, e.getCodeMessage(), null);
    if (e.getException() instanceof BadRequestException
        || e.getException() instanceof IllegalArgumentException
        || e.getException() instanceof DataIntegrityViolationException)
      return new CustomResponse(
          Constants.Message.BAD_REQUEST_BODY,
          Constants.Status.BAD_REQUEST,
          e.getCodeMessage(),
          null);
    if (e.getException() instanceof UnAuthorizedException)
      return new CustomResponse(
          Constants.Message.FORBIDDEN_BODY, Constants.Status.FORBIDDEN, e.getCodeMessage(), null);
    if (e.getException() instanceof EntityNotFoundException
        || e.getException() instanceof NotFoundException)
      return new CustomResponse(
          Constants.Message.NOT_FOUND_BODY, Constants.Status.NOT_FOUND, e.getCodeMessage(), null);
    return new CustomResponse(
        Constants.Message.SERVER_ERROR_BODY,
        Constants.Status.INTERNAL_SERVER_ERROR,
        e.getCodeMessage() != null ? e.getCodeMessage() : e.getMessage(),
        null);
  }
}
