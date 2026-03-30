package com.africa.ubaxplatform.common.exception;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.common.response.CustomResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

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

  private HttpStatus determineHttpStatus(Exception e) {
    if (e == null) return HttpStatus.INTERNAL_SERVER_ERROR;
    log.error(e.getClass().getName(), e);
    if (e instanceof EntityExistsException) return HttpStatus.CONFLICT;
    if (e instanceof IllegalArgumentException || e instanceof DataIntegrityViolationException)
      return HttpStatus.BAD_REQUEST;
    if (e instanceof UnAuthorizedException) return HttpStatus.UNAUTHORIZED;
    if (e instanceof EntityNotFoundException || e instanceof NotFoundException)
      return HttpStatus.NOT_FOUND;
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private CustomResponse getResponse(CustomException e) {
    if (e.getException() instanceof EntityExistsException)
      return new CustomResponse(
          Constants.Message.CONFLICT_BODY,
          Constants.Status.CONFLICT,
          e.getCodeMessage(),
          null);
    if (e.getException() instanceof IllegalArgumentException
        || e.getException() instanceof DataIntegrityViolationException)
      return new CustomResponse(
          Constants.Message.BAD_REQUEST_BODY,
          Constants.Status.BAD_REQUEST,
          e.getCodeMessage(),
          null);
    if (e.getException() instanceof UnAuthorizedException)
      return new CustomResponse(
          Constants.Message.UNAUTHORIZED_BODY,
          Constants.Status.UNAUTHORIZED,
          e.getCodeMessage(),
          null);
    if (e.getException() instanceof EntityNotFoundException
        || e.getException() instanceof NotFoundException)
      return new CustomResponse(
          Constants.Message.NOT_FOUND_BODY,
          Constants.Status.NOT_FOUND,
          e.getCodeMessage(),
          null);
    return new CustomResponse(
        Constants.Message.SERVER_ERROR_BODY,
        Constants.Status.INTERNAL_SERVER_ERROR,
        e.getCodeMessage() != null ? e.getCodeMessage() : e.getMessage(),
        null);
  }
}
