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

  /** Valeurs des code lists — miroir des valeurs persistées dans {@code la_code_list}. */
  public static class CodeList {

    private CodeList() {}

    public static class TicketPriority {
      private TicketPriority() {}

      public static final String LOW = "LOW";
      public static final String NORMAL = "NORMAL";
      public static final String HIGH = "HIGH";
      public static final String URGENT = "URGENT";
    }

    public static class TicketCategory {
      private TicketCategory() {}

      public static final String LEAK = "LEAK";
      public static final String ELECTRICAL = "ELECTRICAL";
      public static final String LOCK = "LOCK";
      public static final String PLUMBING = "PLUMBING";
      public static final String APPLIANCE = "APPLIANCE";
      public static final String STRUCTURE = "STRUCTURE";
      public static final String PEST = "PEST";
      public static final String COMMON_AREA = "COMMON_AREA";
      public static final String OTHER = "OTHER";
    }

    public static class AttachmentType {
      private AttachmentType() {}

      public static final String INCIDENT_PHOTO = "INCIDENT_PHOTO";
      public static final String INCIDENT_VIDEO = "INCIDENT_VIDEO";
      public static final String INTERVENTION_REPORT = "INTERVENTION_REPORT";
      public static final String INVOICE = "INVOICE";
      public static final String OTHER = "OTHER";
    }

    public static class CostImputedTo {
      private CostImputedTo() {}

      public static final String OWNER = "OWNER";
      public static final String TENANT = "TENANT";
      public static final String SHARED = "SHARED";
    }

    public static class PropertyType {
      private PropertyType() {}

      public static final String APARTMENT = "APARTMENT";
      public static final String VILLA = "VILLA";
      public static final String HOUSE = "HOUSE";
      public static final String LAND = "LAND";
      public static final String OFFICE = "OFFICE";
      public static final String WAREHOUSE = "WAREHOUSE";
      public static final String STORE = "STORE";
    }

    public static class TransactionType {
      private TransactionType() {}

      public static final String SALE = "SALE";
      public static final String RENT = "RENT";
      public static final String RENT_FURNISHED = "RENT_FURNISHED";
    }

    public static class PropertyCondition {
      private PropertyCondition() {}

      public static final String NEW = "NEW";
      public static final String GOOD = "GOOD";
      public static final String RENOVATE = "RENOVATE";
    }

    public static class MediaType {
      private MediaType() {}

      public static final String PHOTO = "PHOTO";
      public static final String VIDEO = "VIDEO";
      public static final String PLAN_2D = "PLAN_2D";
      public static final String PLAN_3D = "PLAN_3D";
      public static final String VISIT_360 = "VISIT_360";
    }

    public static class PropertyDocumentType {
      private PropertyDocumentType() {}

      public static final String TITLE_DEED = "TITLE_DEED";
      public static final String BUILDING_PERMIT = "BUILDING_PERMIT";
      public static final String DIAGNOSTIC = "DIAGNOSTIC";
      public static final String CADASTRAL_PLAN = "CADASTRAL_PLAN";
      public static final String INSURANCE = "INSURANCE";
      public static final String CONFORMITY_CERTIFICATE = "CONFORMITY_CERTIFICATE";
      public static final String OTHER = "OTHER";
    }

    public static class ContractType {
      private ContractType() {}

      public static final String LEASE = "LEASE";
      public static final String SALE = "SALE";
      public static final String RESERVATION = "RESERVATION";
      public static final String MANDATE = "MANDATE";
    }

    public static class SubscriptionPlan {
      private SubscriptionPlan() {}

      public static final String BASIC = "BASIC";
      public static final String PRO = "PRO";
      public static final String PREMIUM = "PREMIUM";
    }

    public static class AlertFrequency {
      private AlertFrequency() {}

      public static final String REALTIME = "REALTIME";
      public static final String DAILY = "DAILY";
      public static final String WEEKLY = "WEEKLY";
    }

    public static class NewsletterFrequency {
      private NewsletterFrequency() {}

      public static final String WEEKLY = "WEEKLY";
      public static final String MONTHLY = "MONTHLY";
    }

    public static class DisplayMode {
      private DisplayMode() {}

      public static final String LIST = "LIST";
      public static final String MAP = "MAP";
      public static final String SPLIT = "SPLIT";
    }

    public static class PartnerType {
      private PartnerType() {}

      public static final String AGENCE_IMMOBILIERE = "AGENCE_IMMOBILIERE";
      public static final String HOTEL = "HOTEL";
    }

    public static class EmploymentStatus {
      private EmploymentStatus() {}

      public static final String EMPLOYEE = "EMPLOYEE";
      public static final String SELF_EMPLOYED = "SELF_EMPLOYED";
      public static final String STUDENT = "STUDENT";
      public static final String RETIRED = "RETIRED";
      public static final String UNEMPLOYED = "UNEMPLOYED";
      public static final String OTHER = "OTHER";
    }

    public static class IdDocumentType {
      private IdDocumentType() {}

      public static final String CNI = "CNI";
      public static final String PASSPORT = "PASSPORT";
      public static final String RESIDENCE_PERMIT = "RESIDENCE_PERMIT";
      public static final String DRIVER_LICENSE = "DRIVER_LICENSE";
    }
  }
}
