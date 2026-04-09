package com.africa.ubaxplatform.common.exception;

import org.springframework.http.HttpStatus;

/** Exception levée lors d'une opération sur le stockage objet (MinIO). */
public class StorageException extends CustomRuntimeException {

  public StorageException(String message) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
