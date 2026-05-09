package com.africa.ubaxplatform.unit.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.AgencyRepository;
import com.africa.ubaxplatform.auth.repository.HotelRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.dashboard.dto.AdminDashboardResponse;
import com.africa.ubaxplatform.dashboard.dto.AgencyDashboardResponse;
import com.africa.ubaxplatform.dashboard.service.impl.DashboardServiceImpl;
import com.africa.ubaxplatform.payment.repository.ExpenseRepository;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.reservation.repository.ReservationRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import com.africa.ubaxplatform.ticketing.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl")
class DashboardServiceImplTest {

  @Mock UserRepository userRepo;
  @Mock AgencyRepository agencyRepo;
  @Mock HotelRepository hotelRepo;
  @Mock PaymentRepository paymentRepo;
  @Mock ExpenseRepository expenseRepo;
  @Mock PropertyRepository propertyRepo;
  @Mock ContractRepository contractRepo;
  @Mock ReservationRepository reservationRepo;
  @Mock TicketRepository ticketRepo;

  @InjectMocks DashboardServiceImpl service;

  private static final String KC_ID = "kc-dg-001";
  private Agency agency;
  private User caller;

  @BeforeEach
  void setUp() {
    agency = Agency.builder().name("Agence Test CI").city("Abidjan").build();
    SharedTestFixtures.injectId(agency, SharedTestFixtures.AGENCY_ID);

    caller =
        User.builder()
            .keycloakId(KC_ID)
            .firstName("DG")
            .lastName("Agence")
            .email("dg@agence.ci")
            .agency(agency)
            .build();
    SharedTestFixtures.injectId(caller, SharedTestFixtures.USER_ID);
  }

  @Nested
  @DisplayName("getAgencyDashboard()")
  class GetAgencyDashboard {

    @BeforeEach
    void stubRepos() {
      // userRepo est utilisé dans tous les tests (certains le re-stubbent, d'autres non)
      when(userRepo.findByKeycloakId(KC_ID)).thenReturn(Optional.of(caller));

      // Les stubs suivants ne sont pas appelés dans les tests d'échec précoce (user/agency not
      // found)
      // → lenient() pour éviter UnnecessaryStubbingException
      lenient()
          .when(paymentRepo.sumPaidByAgencyAndPeriod(any(UUID.class), any(), any()))
          .thenReturn(BigDecimal.valueOf(500_000));
      lenient()
          .when(expenseRepo.sumByAgencyAndPeriod(any(UUID.class), any(), any()))
          .thenReturn(BigDecimal.valueOf(100_000));
      lenient()
          .when(paymentRepo.sumOverdueByAgency(any(UUID.class), any(LocalDate.class)))
          .thenReturn(BigDecimal.valueOf(50_000));

      lenient().when(propertyRepo.countByAgencyId(any(UUID.class))).thenReturn(10L);
      lenient().when(propertyRepo.countByAgencyIdAndStatus(any(UUID.class), any())).thenReturn(6L);
      lenient().when(contractRepo.countByAgencyAndStatus(any(UUID.class), any())).thenReturn(5L);
      lenient().when(paymentRepo.countByAgencyIdAndStatus(any(UUID.class), any())).thenReturn(3L);
      lenient()
          .when(paymentRepo.findLateByAgency(any(UUID.class), any(LocalDate.class)))
          .thenReturn(List.of());
      lenient()
          .when(paymentRepo.sumPaidByTypeAndPeriod(any(UUID.class), any(), any()))
          .thenReturn(List.of());
      lenient()
          .when(expenseRepo.sumByCategoryAndPeriod(any(UUID.class), any(), any()))
          .thenReturn(List.of());
    }

    @Test
    @DisplayName("Succès – dashboard calculé avec les bonnes valeurs financières")
    void getAgencyDashboard_success() throws CustomException {
      AgencyDashboardResponse result =
          service.getAgencyDashboard(KC_ID, LocalDate.now().withDayOfMonth(1), LocalDate.now());

      assertThat(result).isNotNull();
      assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
      assertThat(result.totalExpenses()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
      assertThat(result.netRevenue()).isEqualByComparingTo(BigDecimal.valueOf(400_000));
      assertThat(result.totalProperties()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Succès – dates nulles remplacées par le mois courant")
    void getAgencyDashboard_nullDates_usesCurrentMonth() throws CustomException {
      AgencyDashboardResponse result = service.getAgencyDashboard(KC_ID, null, null);

      assertThat(result.periodFrom()).isNotNull();
      assertThat(result.periodTo()).isNotNull();
    }

    @Test
    @DisplayName("Taux de recouvrement – 100% si aucun montant impayé")
    void getAgencyDashboard_zeroOverdue_recoveryRate100() throws CustomException {
      when(paymentRepo.sumOverdueByAgency(any(), any())).thenReturn(BigDecimal.ZERO);

      AgencyDashboardResponse result = service.getAgencyDashboard(KC_ID, null, null);

      assertThat(result.recoveryRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → USER_NOT_FOUND")
    void getAgencyDashboard_userNotFound_throws() {
      when(userRepo.findByKeycloakId(KC_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getAgencyDashboard(KC_ID, null, null))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Echec – utilisateur sans agence → DASHBOARD_GET_FAILURE")
    void getAgencyDashboard_noAgency_throws() {
      caller.setAgency(null);
      when(userRepo.findByKeycloakId(KC_ID)).thenReturn(Optional.of(caller));

      assertThatThrownBy(() -> service.getAgencyDashboard(KC_ID, null, null))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.DASHBOARD_GET_FAILURE);
    }
  }

  @Nested
  @DisplayName("getAdminDashboard()")
  class GetAdminDashboard {

    @Test
    @DisplayName("Succès – dashboard admin agrège tous les KPIs globaux")
    void getAdminDashboard_success() {
      when(agencyRepo.countByActiveAndDeletedAtIsNull(true)).thenReturn(12L);
      when(hotelRepo.countByActiveAndDeletedAtIsNull(true)).thenReturn(5L);
      when(userRepo.countByRoleAndDeletedAtIsNull(UserRole.CLIENT)).thenReturn(320L);
      when(userRepo.countByRoleAndDeletedAtIsNull(UserRole.OWNER)).thenReturn(45L);
      when(reservationRepo.countByStatusAndDeletedAtIsNull(any())).thenReturn(8L);
      when(propertyRepo.countByStatus(any())).thenReturn(20L);
      when(ticketRepo.countByStatusIn(any())).thenReturn(7L);

      AdminDashboardResponse result = service.getAdminDashboard();

      assertThat(result).isNotNull();
      assertThat(result.totalActiveAgencies()).isEqualTo(12L);
      assertThat(result.totalActiveHotels()).isEqualTo(5L);
      assertThat(result.totalClients()).isEqualTo(320L);
      assertThat(result.totalOwners()).isEqualTo(45L);
      assertThat(result.openTickets()).isEqualTo(7L);
    }
  }
}
