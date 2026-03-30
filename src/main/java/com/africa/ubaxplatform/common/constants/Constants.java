package com.africa.ubaxplatform.common.constants;

public class Constants {

  private Constants() {}

  public static final String X_JWT_ASSERTION = "X-JWT-Assertion";
  public static final String AUTHORIZATION = "Authorization";

  /** Préfixe des rôles UBAX dans le realm Keycloak (ex: {@code UBAX_ADMIN}). */
  public static final String KEYCLOAK_ROLE_PREFIX = "UBAX_";

  public static class Message {

    private Message() {}

    public static final String SUCCESS_BODY = "SUCCESS";
    public static final String SERVER_ERROR_BODY = "INTERNAL_SERVER_ERROR";
    public static final String UNAUTHORIZED_BODY = "UNAUTHORIZED";
    public static final String CONFLICT_BODY = "CONFLICT";
    public static final String BAD_REQUEST_BODY = "BAD_REQUEST";
    public static final String NOT_FOUND_BODY = "NOT_FOUND";
  }

  public static class Status {

    private Status() {}

    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int CONFLICT = 409;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
  }
}
