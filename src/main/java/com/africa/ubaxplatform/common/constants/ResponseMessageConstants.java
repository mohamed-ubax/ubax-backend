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
  public static final String USER_FORGOT_PASSWORD_SUCCESS = "USER_FORGOT_PASSWORD_SUCCESS";
  public static final String USER_INVALID_CREDENTIALS = "USER_INVALID_CREDENTIALS";
  public static final String USER_ROLE_ASSIGN_SUCCESS = "USER_ROLE_ASSIGN_SUCCESS";
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String USER_UNAUTHORIZED = "USER_UNAUTHORIZED";
  public static final String USER_FORBIDDEN = "USER_FORBIDDEN";

  // ── OTP ─────────────────────────────────────────────────────────
  public static final String OTP_SENT_SUCCESS = "OTP_SENT_SUCCESS";
  public static final String OTP_VERIFIED_SUCCESS = "OTP_VERIFIED_SUCCESS";
  public static final String OTP_INVALID = "OTP_INVALID";
  public static final String OTP_NOT_FOUND = "OTP_NOT_FOUND";

  // ── ADMIN ───────────────────────────────────────────────────────
  public static final String ADMIN_CREATE_SUCCESS = "ADMIN_CREATE_SUCCESS";
  public static final String ADMIN_ROLE_ASSIGN_SUCCESS = "ADMIN_ROLE_ASSIGN_SUCCESS";
  public static final String ADMIN_DELETE_SUCCESS = "ADMIN_DELETE_SUCCESS";

  // ── USER ────────────────────────────────────────────────────────
  public static final String USER_GET_SUCCESS = "USER_GET_SUCCESS";
  public static final String USER_GET_LIST_SUCCESS = "USER_GET_LIST_SUCCESS";
  public static final String USER_CREATE_SUCCESS = "USER_CREATE_SUCCESS";
  public static final String USER_UPDATE_SUCCESS = "USER_UPDATE_SUCCESS";
  public static final String USER_DELETE_SUCCESS = "USER_DELETE_SUCCESS";
  public static final String USER_GET_FAILURE = "USER_GET_FAILURE";
  public static final String USER_GET_FAILURE_NOT_FOUND = "USER_GET_FAILURE_NOT_FOUND";
  public static final String USER_CREATE_FAILURE = "USER_CREATE_FAILURE";
  public static final String USER_CREATE_FAILURE_ALREADY_EXISTS =
      "USER_CREATE_FAILURE_ALREADY_EXISTS";
  public static final String TEAM_MEMBER_CREATE_SUCCESS = "TEAM_MEMBER_CREATE_SUCCESS";
  public static final String USER_UPDATE_FAILURE = "USER_UPDATE_FAILURE";
  public static final String USER_DELETE_FAILURE = "USER_DELETE_FAILURE";

  // ── PROPERTY ────────────────────────────────────────────────────
  public static final String PROPERTY_GET_SUCCESS = "PROPERTY_GET_SUCCESS";
  public static final String PROPERTY_GET_LIST_SUCCESS = "PROPERTY_GET_LIST_SUCCESS";
  public static final String PROPERTY_CREATE_SUCCESS = "PROPERTY_CREATE_SUCCESS";
  public static final String PROPERTY_UPDATE_SUCCESS = "PROPERTY_UPDATE_SUCCESS";
  public static final String PROPERTY_DELETE_SUCCESS = "PROPERTY_DELETE_SUCCESS";
  public static final String PROPERTY_STATUS_UPDATE_SUCCESS = "PROPERTY_STATUS_UPDATE_SUCCESS";
  public static final String PROPERTY_GET_FAILURE = "PROPERTY_GET_FAILURE";
  public static final String PROPERTY_GET_FAILURE_NOT_FOUND = "PROPERTY_GET_FAILURE_NOT_FOUND";
  public static final String PROPERTY_CREATE_FAILURE = "PROPERTY_CREATE_FAILURE";
  public static final String PROPERTY_CREATE_FAILURE_BAD_REQUEST =
      "PROPERTY_CREATE_FAILURE_BAD_REQUEST";
  public static final String PROPERTY_UPDATE_FAILURE = "PROPERTY_UPDATE_FAILURE";
  public static final String PROPERTY_UPDATE_FAILURE_NOT_FOUND =
      "PROPERTY_UPDATE_FAILURE_NOT_FOUND";
  public static final String PROPERTY_DELETE_FAILURE = "PROPERTY_DELETE_FAILURE";
  public static final String PROPERTY_DELETE_FAILURE_NOT_FOUND =
      "PROPERTY_DELETE_FAILURE_NOT_FOUND";

  // ── PROPERTY MEDIA ───────────────────────────────────────────────
  public static final String PROPERTY_MEDIA_ADD_SUCCESS = "PROPERTY_MEDIA_ADD_SUCCESS";
  public static final String PROPERTY_MEDIA_DELETE_SUCCESS = "PROPERTY_MEDIA_DELETE_SUCCESS";
  public static final String PROPERTY_MEDIA_COVER_SUCCESS = "PROPERTY_MEDIA_COVER_SUCCESS";
  public static final String PROPERTY_MEDIA_NOT_FOUND = "PROPERTY_MEDIA_NOT_FOUND";

  // ── PROPERTY DOCUMENT ────────────────────────────────────────────
  public static final String PROPERTY_DOCUMENT_ADD_SUCCESS = "PROPERTY_DOCUMENT_ADD_SUCCESS";
  public static final String PROPERTY_DOCUMENT_DELETE_SUCCESS = "PROPERTY_DOCUMENT_DELETE_SUCCESS";
  public static final String PROPERTY_DOCUMENT_VERIFY_SUCCESS = "PROPERTY_DOCUMENT_VERIFY_SUCCESS";
  public static final String PROPERTY_DOCUMENT_NOT_FOUND = "PROPERTY_DOCUMENT_NOT_FOUND";

  // ── PARTNER APPLICATION ─────────────────────────────────────────
  public static final String PARTNER_APPLICATION_SUBMIT_SUCCESS =
      "PARTNER_APPLICATION_SUBMIT_SUCCESS";
  public static final String PARTNER_APPLICATION_GET_SUCCESS = "PARTNER_APPLICATION_GET_SUCCESS";
  public static final String PARTNER_APPLICATION_GET_LIST_SUCCESS =
      "PARTNER_APPLICATION_GET_LIST_SUCCESS";
  public static final String PARTNER_APPLICATION_DECISION_SUCCESS =
      "PARTNER_APPLICATION_DECISION_SUCCESS";
  public static final String PARTNER_APPLICATION_ALREADY_EXISTS =
      "PARTNER_APPLICATION_ALREADY_EXISTS";
  public static final String PARTNER_APPLICATION_NOT_FOUND = "PARTNER_APPLICATION_NOT_FOUND";
  public static final String PARTNER_APPLICATION_INVALID_TRANSITION =
      "PARTNER_APPLICATION_INVALID_TRANSITION";
  public static final String PARTNER_APPLICATION_COMMENT_REQUIRED =
      "PARTNER_APPLICATION_COMMENT_REQUIRED";

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
  public static final String CONTRACT_CREATE_FAILURE_BAD_REQUEST =
      "CONTRACT_CREATE_FAILURE_BAD_REQUEST";
  public static final String CONTRACT_UPDATE_FAILURE = "CONTRACT_UPDATE_FAILURE";
  public static final String CONTRACT_UPDATE_FAILURE_NOT_FOUND =
      "CONTRACT_UPDATE_FAILURE_NOT_FOUND";
  public static final String CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION =
      "CONTRACT_UPDATE_FAILURE_INVALID_TRANSITION";

  // ── PAYMENT ─────────────────────────────────────────────────────
  public static final String PAYMENT_GET_SUCCESS = "PAYMENT_GET_SUCCESS";
  public static final String PAYMENT_GET_LIST_SUCCESS = "PAYMENT_GET_LIST_SUCCESS";
  public static final String PAYMENT_CREATE_SUCCESS = "PAYMENT_CREATE_SUCCESS";
  public static final String PAYMENT_CREATE_FAILURE = "PAYMENT_CREATE_FAILURE";
  public static final String PAYMENT_CREATE_FAILURE_BAD_REQUEST =
      "PAYMENT_CREATE_FAILURE_BAD_REQUEST";
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
  public static final String TICKET_UPDATE_FAILURE_INVALID_TRANSITION =
      "TICKET_UPDATE_FAILURE_INVALID_TRANSITION";
  public static final String TICKET_UPDATE_FAILURE_INVALID_STATUS =
      "TICKET_UPDATE_FAILURE_INVALID_STATUS";
  public static final String TICKET_MESSAGE_CREATE_SUCCESS = "TICKET_MESSAGE_CREATE_SUCCESS";
  public static final String TICKET_MESSAGE_GET_LIST_SUCCESS = "TICKET_MESSAGE_GET_LIST_SUCCESS";
  public static final String TICKET_ATTACHMENT_GET_LIST_SUCCESS =
      "TICKET_ATTACHMENT_GET_LIST_SUCCESS";

  // ── BAILLEUR ────────────────────────────────────────────────────
  public static final String BAILLEUR_APPLICATION_SUBMIT_SUCCESS =
      "BAILLEUR_APPLICATION_SUBMIT_SUCCESS";
  public static final String BAILLEUR_APPLICATION_GET_SUCCESS = "BAILLEUR_APPLICATION_GET_SUCCESS";
  public static final String BAILLEUR_APPLICATION_GET_LIST_SUCCESS =
      "BAILLEUR_APPLICATION_GET_LIST_SUCCESS";
  public static final String BAILLEUR_APPLICATION_DECISION_SUCCESS =
      "BAILLEUR_APPLICATION_DECISION_SUCCESS";
  public static final String BAILLEUR_APPLICATION_NOT_FOUND = "BAILLEUR_APPLICATION_NOT_FOUND";
  public static final String BAILLEUR_APPLICATION_CONFLICT = "BAILLEUR_APPLICATION_CONFLICT";
  public static final String BAILLEUR_APPLICATION_INVALID_STATUS =
      "BAILLEUR_APPLICATION_INVALID_STATUS";
  public static final String BAILLEUR_AGENCY_NOT_FOUND = "BAILLEUR_AGENCY_NOT_FOUND";
  public static final String BAILLEUR_GET_LIST_SUCCESS = "BAILLEUR_GET_LIST_SUCCESS";
  public static final String BAILLEUR_MY_APPLICATIONS_SUCCESS = "BAILLEUR_MY_APPLICATIONS_SUCCESS";
  public static final String BAILLEUR_MY_AGENCIES_SUCCESS = "BAILLEUR_MY_AGENCIES_SUCCESS";

  // ── RESERVATION ─────────────────────────────────────────────────
  public static final String RESERVATION_GET_SUCCESS = "RESERVATION_GET_SUCCESS";
  public static final String RESERVATION_GET_LIST_SUCCESS = "RESERVATION_GET_LIST_SUCCESS";
  public static final String RESERVATION_CREATE_SUCCESS = "RESERVATION_CREATE_SUCCESS";
  public static final String RESERVATION_UPDATE_SUCCESS = "RESERVATION_UPDATE_SUCCESS";
  public static final String RESERVATION_DELETE_SUCCESS = "RESERVATION_DELETE_SUCCESS";
  public static final String RESERVATION_GET_FAILURE_NOT_FOUND =
      "RESERVATION_GET_FAILURE_NOT_FOUND";
  public static final String RESERVATION_CREATE_FAILURE_BAD_REQUEST =
      "RESERVATION_CREATE_FAILURE_BAD_REQUEST";
  public static final String RESERVATION_CREATE_FAILURE_UNAVAILABLE =
      "RESERVATION_CREATE_FAILURE_UNAVAILABLE";
  public static final String RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION =
      "RESERVATION_UPDATE_FAILURE_INVALID_TRANSITION";

  // ── AGENCY ──────────────────────────────────────────────────────
  public static final String AGENCY_LIST_SUCCESS = "AGENCY_LIST_SUCCESS";

  // ── ADMIN PARTNER ────────────────────────────────────────────────
  public static final String PARTNER_GET_LIST_SUCCESS = "PARTNER_GET_LIST_SUCCESS";
  public static final String PARTNER_GET_SUCCESS = "PARTNER_GET_SUCCESS";
  public static final String PARTNER_NOT_FOUND = "PARTNER_NOT_FOUND";
  public static final String PARTNER_SUSPEND_SUCCESS = "PARTNER_SUSPEND_SUCCESS";
  public static final String PARTNER_ACTIVATE_SUCCESS = "PARTNER_ACTIVATE_SUCCESS";
  public static final String PARTNER_ALREADY_SUSPENDED = "PARTNER_ALREADY_SUSPENDED";
  public static final String PARTNER_ALREADY_ACTIVE = "PARTNER_ALREADY_ACTIVE";
  public static final String PARTNER_SUBSCRIPTION_UPDATE_SUCCESS =
      "PARTNER_SUBSCRIPTION_UPDATE_SUCCESS";
  public static final String CLIENT_GET_LIST_SUCCESS = "CLIENT_GET_LIST_SUCCESS";

  // ── DASHBOARD ───────────────────────────────────────────────────
  public static final String DASHBOARD_GET_SUCCESS = "DASHBOARD_GET_SUCCESS";
  public static final String DASHBOARD_GET_FAILURE = "DASHBOARD_GET_FAILURE";
  public static final String DASHBOARD_ADMIN_GET_SUCCESS = "DASHBOARD_ADMIN_GET_SUCCESS";

  // ── CODE LIST ───────────────────────────────────────────────────
  public static final String CODELIST_GET_SUCCESS = "CODELIST_GET_SUCCESS";
  public static final String CODELIST_POST_SUCCESS = "CODELIST_POST_SUCCESS";
  public static final String CODELIST_PUT_SUCCESS = "CODELIST_PUT_SUCCESS";
  public static final String CODELIST_GET_FAILURE_NOT_FOUND = "CODELIST_GET_FAILURE_NOT_FOUND";
  public static final String CODELIST_GET_FAILURE_BAD_REQUEST = "CODELIST_GET_FAILURE_BAD_REQUEST";
  public static final String CODELIST_POST_FAILURE = "CODELIST_POST_FAILURE";
  public static final String CODELIST_POST_DUPLICATE = "CODELIST_POST_DUPLICATE";
  public static final String CODELIST_PUT_FAILURE = "CODELIST_PUT_FAILURE";
  public static final String CODELIST_PUT_FAILURE_BAD_REQUEST = "CODELIST_PUT_FAILURE_BAD_REQUEST";
}
