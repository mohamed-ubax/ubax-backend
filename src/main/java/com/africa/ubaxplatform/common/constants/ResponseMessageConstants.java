package com.africa.ubaxplatform.common.constants;

/**
 * Constantes de messages de réponse retournés dans le champ {@code message} de {@link
 * com.africa.ubaxplatform.common.response.CustomResponse}.
 *
 * <p>Convention : {@code <DOMAINE>_<OPERATION>_<SUCCES|FAILURE>[_RAISON]}
 */
public class ResponseMessageConstants {

  private ResponseMessageConstants() {}

  // ── AUTH ────────────────────────────────────────────────────────
  public static final String USER_LOGIN_SUCCESS = "USER_LOGIN_SUCCESS";
  public static final String USER_LOGOUT_SUCCESS = "USER_LOGOUT_SUCCESS";
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String USER_UNAUTHORIZED = "USER_UNAUTHORIZED";
  public static final String USER_FORBIDDEN = "USER_FORBIDDEN";

  // ── USER ────────────────────────────────────────────────────────
  public static final String USER_GET_SUCCESS = "USER_GET_SUCCESS";
  public static final String USER_GET_LIST_SUCCESS = "USER_GET_LIST_SUCCESS";
  public static final String USER_CREATE_SUCCESS = "USER_CREATE_SUCCESS";
  public static final String USER_UPDATE_SUCCESS = "USER_UPDATE_SUCCESS";
  public static final String USER_DELETE_SUCCESS = "USER_DELETE_SUCCESS";
  public static final String USER_GET_FAILURE = "USER_GET_FAILURE";
  public static final String USER_GET_FAILURE_NOT_FOUND = "USER_GET_FAILURE_NOT_FOUND";
  public static final String USER_CREATE_FAILURE = "USER_CREATE_FAILURE";
  public static final String USER_CREATE_FAILURE_ALREADY_EXISTS = "USER_CREATE_FAILURE_ALREADY_EXISTS";
  public static final String USER_UPDATE_FAILURE = "USER_UPDATE_FAILURE";
  public static final String USER_DELETE_FAILURE = "USER_DELETE_FAILURE";

  // ── PROPERTY ────────────────────────────────────────────────────
  public static final String PROPERTY_GET_SUCCESS = "PROPERTY_GET_SUCCESS";
  public static final String PROPERTY_GET_LIST_SUCCESS = "PROPERTY_GET_LIST_SUCCESS";
  public static final String PROPERTY_CREATE_SUCCESS = "PROPERTY_CREATE_SUCCESS";
  public static final String PROPERTY_UPDATE_SUCCESS = "PROPERTY_UPDATE_SUCCESS";
  public static final String PROPERTY_DELETE_SUCCESS = "PROPERTY_DELETE_SUCCESS";
  public static final String PROPERTY_GET_FAILURE = "PROPERTY_GET_FAILURE";
  public static final String PROPERTY_GET_FAILURE_NOT_FOUND = "PROPERTY_GET_FAILURE_NOT_FOUND";
  public static final String PROPERTY_CREATE_FAILURE = "PROPERTY_CREATE_FAILURE";
  public static final String PROPERTY_CREATE_FAILURE_BAD_REQUEST = "PROPERTY_CREATE_FAILURE_BAD_REQUEST";
  public static final String PROPERTY_UPDATE_FAILURE = "PROPERTY_UPDATE_FAILURE";
  public static final String PROPERTY_UPDATE_FAILURE_NOT_FOUND = "PROPERTY_UPDATE_FAILURE_NOT_FOUND";
  public static final String PROPERTY_DELETE_FAILURE = "PROPERTY_DELETE_FAILURE";
  public static final String PROPERTY_DELETE_FAILURE_NOT_FOUND = "PROPERTY_DELETE_FAILURE_NOT_FOUND";

  // ── TENANT ──────────────────────────────────────────────────────
  public static final String TENANT_GET_SUCCESS = "TENANT_GET_SUCCESS";
  public static final String TENANT_GET_LIST_SUCCESS = "TENANT_GET_LIST_SUCCESS";
  public static final String TENANT_CREATE_SUCCESS = "TENANT_CREATE_SUCCESS";
  public static final String TENANT_UPDATE_SUCCESS = "TENANT_UPDATE_SUCCESS";
  public static final String TENANT_DELETE_SUCCESS = "TENANT_DELETE_SUCCESS";
  public static final String TENANT_GET_FAILURE_NOT_FOUND = "TENANT_GET_FAILURE_NOT_FOUND";
  public static final String TENANT_CREATE_FAILURE = "TENANT_CREATE_FAILURE";
  public static final String TENANT_UPDATE_FAILURE = "TENANT_UPDATE_FAILURE";
  public static final String TENANT_UPDATE_FAILURE_NOT_FOUND = "TENANT_UPDATE_FAILURE_NOT_FOUND";

  // ── CONTRACT ────────────────────────────────────────────────────
  public static final String CONTRACT_GET_SUCCESS = "CONTRACT_GET_SUCCESS";
  public static final String CONTRACT_GET_LIST_SUCCESS = "CONTRACT_GET_LIST_SUCCESS";
  public static final String CONTRACT_CREATE_SUCCESS = "CONTRACT_CREATE_SUCCESS";
  public static final String CONTRACT_UPDATE_SUCCESS = "CONTRACT_UPDATE_SUCCESS";
  public static final String CONTRACT_DELETE_SUCCESS = "CONTRACT_DELETE_SUCCESS";
  public static final String CONTRACT_GET_FAILURE_NOT_FOUND = "CONTRACT_GET_FAILURE_NOT_FOUND";
  public static final String CONTRACT_CREATE_FAILURE = "CONTRACT_CREATE_FAILURE";
  public static final String CONTRACT_CREATE_FAILURE_BAD_REQUEST = "CONTRACT_CREATE_FAILURE_BAD_REQUEST";
  public static final String CONTRACT_UPDATE_FAILURE = "CONTRACT_UPDATE_FAILURE";
  public static final String CONTRACT_UPDATE_FAILURE_NOT_FOUND = "CONTRACT_UPDATE_FAILURE_NOT_FOUND";

  // ── PAYMENT ─────────────────────────────────────────────────────
  public static final String PAYMENT_GET_SUCCESS = "PAYMENT_GET_SUCCESS";
  public static final String PAYMENT_GET_LIST_SUCCESS = "PAYMENT_GET_LIST_SUCCESS";
  public static final String PAYMENT_CREATE_SUCCESS = "PAYMENT_CREATE_SUCCESS";
  public static final String PAYMENT_CREATE_FAILURE = "PAYMENT_CREATE_FAILURE";
  public static final String PAYMENT_CREATE_FAILURE_BAD_REQUEST = "PAYMENT_CREATE_FAILURE_BAD_REQUEST";
  public static final String PAYMENT_GET_FAILURE_NOT_FOUND = "PAYMENT_GET_FAILURE_NOT_FOUND";

  // ── DOCUMENT ────────────────────────────────────────────────────
  public static final String DOCUMENT_GET_SUCCESS = "DOCUMENT_GET_SUCCESS";
  public static final String DOCUMENT_UPLOAD_SUCCESS = "DOCUMENT_UPLOAD_SUCCESS";
  public static final String DOCUMENT_DELETE_SUCCESS = "DOCUMENT_DELETE_SUCCESS";
  public static final String DOCUMENT_GET_FAILURE_NOT_FOUND = "DOCUMENT_GET_FAILURE_NOT_FOUND";
  public static final String DOCUMENT_UPLOAD_FAILURE = "DOCUMENT_UPLOAD_FAILURE";
  public static final String DOCUMENT_UPLOAD_FAILURE_MAX_SIZE = "DOCUMENT_UPLOAD_FAILURE_MAX_SIZE";

  // ── NOTIFICATION ────────────────────────────────────────────────
  public static final String NOTIFICATION_GET_LIST_SUCCESS = "NOTIFICATION_GET_LIST_SUCCESS";
  public static final String NOTIFICATION_MARK_READ_SUCCESS = "NOTIFICATION_MARK_READ_SUCCESS";
  public static final String NOTIFICATION_SEND_FAILURE = "NOTIFICATION_SEND_FAILURE";

  // ── TICKETING ───────────────────────────────────────────────────
  public static final String TICKET_GET_SUCCESS = "TICKET_GET_SUCCESS";
  public static final String TICKET_GET_LIST_SUCCESS = "TICKET_GET_LIST_SUCCESS";
  public static final String TICKET_CREATE_SUCCESS = "TICKET_CREATE_SUCCESS";
  public static final String TICKET_UPDATE_SUCCESS = "TICKET_UPDATE_SUCCESS";
  public static final String TICKET_GET_FAILURE_NOT_FOUND = "TICKET_GET_FAILURE_NOT_FOUND";
  public static final String TICKET_CREATE_FAILURE = "TICKET_CREATE_FAILURE";
  public static final String TICKET_UPDATE_FAILURE_NOT_FOUND = "TICKET_UPDATE_FAILURE_NOT_FOUND";
}
