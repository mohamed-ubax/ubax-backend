package com.africa.ubaxplatform.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends Exception {

  private final Exception exception;
  private final String codeMessage;

  public CustomException(String codeMessage, Throwable cause) {
    super(codeMessage, cause);
    this.codeMessage = codeMessage;
    this.exception = null;
  }

  public CustomException(Exception cause) {
    super(cause.getMessage(), cause);
    this.exception = cause;
    this.codeMessage = null;
  }

  public CustomException(String codeMessage) {
    super(codeMessage);
    this.codeMessage = codeMessage;
    this.exception = null;
  }

  public CustomException(Exception exception, String codeMessage) {
    super(codeMessage, exception);
    this.exception = exception;
    this.codeMessage = codeMessage;
  }

  @Override
  public String getMessage() {
    return codeMessage != null ? codeMessage : super.getMessage();
  }
}
