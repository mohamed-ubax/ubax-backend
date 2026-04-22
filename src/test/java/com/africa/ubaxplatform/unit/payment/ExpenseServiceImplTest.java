package com.africa.ubaxplatform.unit.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.payment.codeList.CostCenter;
import com.africa.ubaxplatform.payment.codeList.ExpenseCategory;
import com.africa.ubaxplatform.payment.dto.ExpenseCreateRequest;
import com.africa.ubaxplatform.payment.dto.ExpenseResponse;
import com.africa.ubaxplatform.payment.entity.Expense;
import com.africa.ubaxplatform.payment.repository.ExpenseRepository;
import com.africa.ubaxplatform.payment.service.impl.ExpenseServiceImpl;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseServiceImpl – tests unitaires")
class ExpenseServiceImplTest {

  @Mock private ExpenseRepository expenseRepo;
  @Mock private UserRepository userRepo;
  @Mock private PropertyRepository propertyRepo;

  @InjectMocks private ExpenseServiceImpl service;

  private Agency agency;
  private User caller;
  private Expense expense;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    caller = SharedTestFixtures.buildPartnerUser();
    expense = SharedTestFixtures.buildExpense(agency, caller);
  }

  private ExpenseCreateRequest buildGeneralRequest() {
    return new ExpenseCreateRequest(
        null,
        ExpenseCategory.MAINTENANCE,
        CostCenter.AGENCY_GENERAL,
        "Entretien général",
        BigDecimal.valueOf(150_000),
        null,
        LocalDate.now(),
        "Prestataire CI",
        "FAC-001",
        null,
        null);
  }

  private ExpenseCreateRequest buildPropertySpecificRequest(UUID propertyId) {
    return new ExpenseCreateRequest(
        propertyId,
        ExpenseCategory.MAINTENANCE,
        CostCenter.PROPERTY_SPECIFIC,
        "Réparation toiture",
        BigDecimal.valueOf(200_000),
        null,
        LocalDate.now(),
        null,
        null,
        null,
        null);
  }

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – dépense générale créée sans bien associé")
    void create_general_success() throws CustomException {
      ExpenseCreateRequest req = buildGeneralRequest();
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(expenseRepo.save(any(Expense.class)))
          .thenAnswer(
              inv -> {
                Expense e = inv.getArgument(0);
                SharedTestFixtures.injectId(e, SharedTestFixtures.EXPENSE_ID);
                return e;
              });

      ExpenseResponse result = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(result).isNotNull();
      assertThat(result.label()).isEqualTo("Entretien général");
      assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(150_000));

      ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
      verify(expenseRepo).save(captor.capture());
      assertThat(captor.getValue().getAgency().getId()).isEqualTo(SharedTestFixtures.AGENCY_ID);
      assertThat(captor.getValue().getProperty()).isNull();
    }

    @Test
    @DisplayName("Succès – dépense PROPERTY_SPECIFIC avec bien associé")
    void create_propertySpecific_success() throws CustomException {
      UUID propertyId = SharedTestFixtures.PROPERTY_ID;
      Property property = Property.builder().build();
      SharedTestFixtures.injectId(property, propertyId);

      ExpenseCreateRequest req = buildPropertySpecificRequest(propertyId);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(propertyRepo.findById(propertyId)).thenReturn(Optional.of(property));
      when(expenseRepo.save(any(Expense.class)))
          .thenAnswer(
              inv -> {
                Expense e = inv.getArgument(0);
                SharedTestFixtures.injectId(e, SharedTestFixtures.EXPENSE_ID);
                return e;
              });

      ExpenseResponse result = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(result).isNotNull();
      ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
      verify(expenseRepo).save(captor.capture());
      assertThat(captor.getValue().getProperty()).isNotNull();
    }

    @Test
    @DisplayName("Échec – PROPERTY_SPECIFIC sans propertyId → PAYMENT_CREATE_FAILURE_BAD_REQUEST")
    void create_propertySpecificWithoutPropertyId_throwsBadRequest() {
      ExpenseCreateRequest req =
          new ExpenseCreateRequest(
              null,
              ExpenseCategory.MAINTENANCE,
              CostCenter.PROPERTY_SPECIFIC,
              "Réparation",
              BigDecimal.valueOf(100_000),
              null,
              LocalDate.now(),
              null,
              null,
              null,
              null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
      verify(expenseRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – utilisateur sans agence → PAYMENT_CREATE_FAILURE_BAD_REQUEST")
    void create_noAgency_throwsBadRequest() {
      User noAgencyUser =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgencyUser));

      assertThatThrownBy(
              () -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildGeneralRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
      verify(expenseRepo, never()).save(any());
    }

    @Test
    @DisplayName("Échec – utilisateur introuvable → USER_NOT_FOUND")
    void create_userNotFound_throwsCustomException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildGeneralRequest()))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – ADMIN (sans agence) voit la dépense sans contrôle d'agence")
    void getById_adminWithoutAgency_returnsExpense() throws CustomException {
      User admin =
          SharedTestFixtures.buildAdminUser(
              SharedTestFixtures.KEYCLOAK_ID,
              SharedTestFixtures.USER_ID,
              com.africa.ubaxplatform.auth.codeList.UserRole.ADMIN);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(expenseRepo.findById(SharedTestFixtures.EXPENSE_ID)).thenReturn(Optional.of(expense));

      ExpenseResponse result =
          service.getById(SharedTestFixtures.EXPENSE_ID, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(result).isNotNull();
      assertThat(result.label()).isEqualTo("Entretien général");
    }

    @Test
    @DisplayName("Succès – PARTNER de la même agence voit la dépense")
    void getById_sameAgencyPartner_returnsExpense() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(expenseRepo.findById(SharedTestFixtures.EXPENSE_ID)).thenReturn(Optional.of(expense));

      ExpenseResponse result =
          service.getById(SharedTestFixtures.EXPENSE_ID, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Échec – dépense introuvable → PAYMENT_GET_FAILURE_NOT_FOUND")
    void getById_notFound_throwsNotFoundException() {
      UUID unknownId = UUID.randomUUID();
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(expenseRepo.findById(unknownId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(unknownId, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_GET_FAILURE_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – PARTNER d'une autre agence → USER_FORBIDDEN")
    void getById_differentAgency_throwsForbidden() {
      Agency otherAgency = Agency.builder().name("Autre Agence").build();
      SharedTestFixtures.injectId(otherAgency, UUID.randomUUID());

      User otherCaller =
          SharedTestFixtures.buildPartnerUser("other-kc", UUID.randomUUID(), otherAgency);

      when(userRepo.findByKeycloakId("other-kc")).thenReturn(Optional.of(otherCaller));
      when(expenseRepo.findById(SharedTestFixtures.EXPENSE_ID)).thenReturn(Optional.of(expense));

      assertThatThrownBy(() -> service.getById(SharedTestFixtures.EXPENSE_ID, "other-kc"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  @Nested
  @DisplayName("list()")
  class ListExpenses {

    @Test
    @DisplayName("Succès – retourne page paginée de l'agence")
    void list_success_returnsPage() throws CustomException {
      Page<Expense> page = new PageImpl<>(java.util.List.of(expense));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(expenseRepo.findWithFilters(
              SharedTestFixtures.AGENCY_ID, null, null, null, null, PageRequest.of(0, 10)))
          .thenReturn(page);

      Page<ExpenseResponse> result =
          service.list(
              SharedTestFixtures.KEYCLOAK_ID, null, null, null, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Échec – utilisateur sans agence → PAYMENT_CREATE_FAILURE_BAD_REQUEST")
    void list_noAgency_throwsBadRequest() {
      User noAgency =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgency));

      assertThatThrownBy(
              () ->
                  service.list(
                      SharedTestFixtures.KEYCLOAK_ID,
                      null,
                      null,
                      null,
                      null,
                      PageRequest.of(0, 10)))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_CREATE_FAILURE_BAD_REQUEST);
    }
  }

  @Nested
  @DisplayName("delete()")
  class Delete {

    @Test
    @DisplayName("Succès – dépense supprimée par le PARTNER de la même agence")
    void delete_sameAgency_deletesSuccessfully() throws CustomException {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(expenseRepo.findById(SharedTestFixtures.EXPENSE_ID)).thenReturn(Optional.of(expense));

      service.delete(SharedTestFixtures.EXPENSE_ID, SharedTestFixtures.KEYCLOAK_ID);

      verify(expenseRepo).delete(expense);
    }

    @Test
    @DisplayName("Succès – ADMIN sans agence peut supprimer (pas de contrôle d'agence)")
    void delete_adminWithoutAgency_deletesSuccessfully() throws CustomException {
      User admin =
          SharedTestFixtures.buildAdminUser(
              SharedTestFixtures.KEYCLOAK_ID,
              SharedTestFixtures.USER_ID,
              com.africa.ubaxplatform.auth.codeList.UserRole.ADMIN);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(admin));
      when(expenseRepo.findById(SharedTestFixtures.EXPENSE_ID)).thenReturn(Optional.of(expense));

      service.delete(SharedTestFixtures.EXPENSE_ID, SharedTestFixtures.KEYCLOAK_ID);

      verify(expenseRepo).delete(expense);
    }

    @Test
    @DisplayName("Échec – dépense introuvable → PAYMENT_GET_FAILURE_NOT_FOUND")
    void delete_notFound_throwsNotFoundException() {
      UUID unknownId = UUID.randomUUID();
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(caller));
      when(expenseRepo.findById(unknownId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(unknownId, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.PAYMENT_GET_FAILURE_NOT_FOUND);
      verify(expenseRepo, never()).delete(any());
    }
  }
}
