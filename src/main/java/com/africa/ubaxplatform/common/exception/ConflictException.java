package com.africa.ubaxplatform.common.exception;

/** Levée quand une ressource est en conflit avec une entrée existante (HTTP 409). */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
