package com.africa.ubaxplatform.common.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class CustomRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -5322396651547611119L;

  private final HttpStatus status;

  public CustomRuntimeException(String message) {
    super(message);
    this.status = HttpStatus.BAD_REQUEST;
  }

  public CustomRuntimeException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public CustomRuntimeException(String message, Throwable cause) {
    super(message, cause);
    this.status = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public CustomRuntimeException(String message, Throwable cause, HttpStatus status) {
    super(message, cause);
    this.status = status;
  }
}
