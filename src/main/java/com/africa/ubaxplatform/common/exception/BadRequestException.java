package com.africa.ubaxplatform.common.exception;

/** Levée quand une requête est sémantiquement invalide (HTTP 400). */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
