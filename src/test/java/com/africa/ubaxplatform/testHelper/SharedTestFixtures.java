package com.africa.ubaxplatform.testHelper;

import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.entity.UserSubRole;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.payment.codeList.CostCenter;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.codeList.PaymentMethod;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.entity.Expense;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.technicien.entity.Technicien;
import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import com.africa.ubaxplatform.ticketing.entity.Ticket;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SharedTestFixtures {

  private SharedTestFixtures() {}

  public static final String KEYCLOAK_ID = "kc-" + UUID.randomUUID();
  public static final UUID USER_ID = UUID.randomUUID();
  public static final UUID AGENCY_ID = UUID.randomUUID();
  public static final UUID HOTEL_ID = UUID.randomUUID();
  public static final UUID TECHNICIEN_ID = UUID.randomUUID();
  public static final UUID PAYMENT_ID = UUID.randomUUID();
  public static final UUID EXPENSE_ID = UUID.randomUUID();
  public static final UUID TICKET_ID = UUID.randomUUID();
  public static final UUID CONTRACT_ID = UUID.randomUUID();
  public static final UUID PROPERTY_ID = UUID.randomUUID();
  public static final UUID TENANT_ID = UUID.randomUUID();
  public static final UUID SUB_ROLE_ID = UUID.randomUUID();

  public static Agency buildAgency() {
    Agency agency = Agency.builder().name("Agence Test CI").city("Abidjan").build();
    injectId(agency, AGENCY_ID);
    return agency;
  }

  public static Hotel buildHotel() {
    Hotel hotel = Hotel.builder().name("Hotel Test CI").build();
    injectId(hotel, HOTEL_ID);
    return hotel;
  }

  public static User buildHotelUser(String keycloakId, UUID userId, Hotel hotel) {
    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName("Amara")
            .lastName("Koné")
            .email("amara@hotel.ci")
            .phone("+2250700000004")
            .roles(new HashSet<>(Set.of(UserRole.PARTNER)))
            .hotel(hotel)
            .build();
    injectId(user, userId);
    return user;
  }

  public static Technicien buildTechnicien(Agency agency, Hotel hotel) {
    Technicien t =
        Technicien.builder()
            .firstName("Mamadou")
            .lastName("Diallo")
            .phone("+2250712345678")
            .profession("PLOMBIER")
            .agency(agency)
            .hotel(hotel)
            .build();
    injectId(t, TECHNICIEN_ID);
    return t;
  }

  public static User buildPartnerUser() {
    return buildPartnerUser(KEYCLOAK_ID, USER_ID, buildAgency());
  }

  public static User buildPartnerUser(String keycloakId, UUID userId, Agency agency) {
    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName("Kofi")
            .lastName("Asante")
            .email("kofi@agence.ci")
            .phone("+2250700000001")
            .roles(new HashSet<>(Set.of(UserRole.PARTNER)))
            .agency(agency)
            .build();
    injectId(user, userId);
    return user;
  }

  public static User buildAdminUser(String keycloakId, UUID userId, UserRole role) {
    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName("Admin")
            .lastName("UBAX")
            .email("admin@ubax.africa")
            .phone("+2250700000002")
            .roles(new HashSet<>(Set.of(role)))
            .build();
    injectId(user, userId);
    return user;
  }

  public static User buildUserWithoutAgency(String keycloakId, UUID userId) {
    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName("Solo")
            .lastName("User")
            .email("solo@test.ci")
            .phone("+2250700000003")
            .roles(new HashSet<>(Set.of(UserRole.CLIENT)))
            .build();
    injectId(user, userId);
    return user;
  }

  public static UserSubRole buildSubRole(User user, String role, RoleScope scope) {
    return UserSubRole.builder().id(SUB_ROLE_ID).user(user).role(role).scope(scope).build();
  }

  public static Payment buildPayment(Agency agency, User recorder, PaymentStatus status) {
    Payment p =
        Payment.builder()
            .agency(agency)
            .recordedBy(recorder)
            .paymentType(PaymentType.RENT)
            .paymentMethod(PaymentMethod.MOBILE_MONEY)
            .status(status)
            .amount(BigDecimal.valueOf(350_000))
            .dueDate(LocalDate.now().plusDays(5))
            .reference("PAY-2025-RENT-ABCD1234")
            .build();
    injectId(p, PAYMENT_ID);
    return p;
  }

  public static Expense buildExpense(Agency agency, User creator) {
    Expense e =
        Expense.builder()
            .agency(agency)
            .createdBy(creator)
            .category(ExpenseCategory.MAINTENANCE)
            .costCenter(CostCenter.AGENCY_GENERAL)
            .label("Entretien général")
            .amount(BigDecimal.valueOf(150_000))
            .expenseDate(LocalDate.now())
            .build();
    injectId(e, EXPENSE_ID);
    return e;
  }

  public static Ticket buildTicket(Contract contract, User reporter, TicketStatus status) {
    Ticket t =
        Ticket.builder()
            .contract(contract)
            .reporter(reporter)
            .category("ELECTRICIEN")
            .title("Panne électrique salon")
            .description("Le disjoncteur saute chaque soir depuis plusieurs jours")
            .status(status)
            .build();
    injectId(t, TICKET_ID);
    return t;
  }

  public static void injectId(Object entity, UUID id) {
    try {
      var idField = BaseEntity.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException("Cannot inject id: " + e.getMessage(), e);
    }
  }
}
