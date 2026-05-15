package com.africa.ubaxplatform.unit.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.technicien.entity.Technicien;
import com.africa.ubaxplatform.technicien.repository.TechnicienRepository;
import com.africa.ubaxplatform.testHelper.SharedTestFixtures;
import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import com.africa.ubaxplatform.ticketing.dto.AddTicketMessageRequest;
import com.africa.ubaxplatform.ticketing.dto.AssignTicketRequest;
import com.africa.ubaxplatform.ticketing.dto.CreateTicketRequest;
import com.africa.ubaxplatform.ticketing.dto.ScheduleInterventionRequest;
import com.africa.ubaxplatform.ticketing.dto.TicketMessageResponse;
import com.africa.ubaxplatform.ticketing.dto.TicketResponse;
import com.africa.ubaxplatform.ticketing.dto.UpdateRepairCostRequest;
import com.africa.ubaxplatform.ticketing.dto.UpdateTicketStatusRequest;
import com.africa.ubaxplatform.ticketing.entity.Ticket;
import com.africa.ubaxplatform.ticketing.entity.TicketMessage;
import com.africa.ubaxplatform.ticketing.repository.TicketAttachmentRepository;
import com.africa.ubaxplatform.ticketing.repository.TicketMessageRepository;
import com.africa.ubaxplatform.ticketing.repository.TicketRepository;
import com.africa.ubaxplatform.ticketing.service.impl.TicketServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImpl – tests unitaires")
class TicketServiceImplTest {

  @Mock private TicketRepository ticketRepo;
  @Mock private TicketMessageRepository messageRepo;
  @Mock private TicketAttachmentRepository attachmentRepo;
  @Mock private UserRepository userRepo;
  @Mock private ContractRepository contractRepo;
  @Mock private TechnicienRepository technicienRepo;

  @InjectMocks private TicketServiceImpl service;

  private Agency agency;
  private User reporter;
  private Contract contract;
  private Ticket openTicket;

  @BeforeEach
  void setUp() {
    agency = SharedTestFixtures.buildAgency();
    reporter = SharedTestFixtures.buildPartnerUser();

    contract = Contract.builder().build();
    SharedTestFixtures.injectId(contract, SharedTestFixtures.CONTRACT_ID);

    openTicket = SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.OPEN);
  }

  private CreateTicketRequest buildCreateRequest() {
    CreateTicketRequest req = new CreateTicketRequest();
    req.setContractId(SharedTestFixtures.CONTRACT_ID);
    req.setCategory("ELECTRICIEN");
    req.setTitle("Panne électrique salon");
    req.setDescription("Le disjoncteur saute chaque soir depuis plusieurs jours");
    req.setPriority("HIGH");
    return req;
  }

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("Succès – ticket créé avec la priorité HIGH")
    void create_success() throws Exception {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.of(contract));
      when(ticketRepo.save(any(Ticket.class)))
          .thenAnswer(
              inv -> {
                Ticket t = inv.getArgument(0);
                SharedTestFixtures.injectId(t, SharedTestFixtures.TICKET_ID);
                return t;
              });

      TicketResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest());

      assertThat(resp).isNotNull();
      assertThat(resp.category()).isEqualTo("ELECTRICIEN");
      assertThat(resp.status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("Succès – priorité par défaut NORMAL si non fournie")
    void create_defaultPriority() throws Exception {
      CreateTicketRequest req = buildCreateRequest();
      req.setPriority(null);

      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.of(contract));
      when(ticketRepo.save(any(Ticket.class)))
          .thenAnswer(
              inv -> {
                Ticket t = inv.getArgument(0);
                SharedTestFixtures.injectId(t, SharedTestFixtures.TICKET_ID);
                return t;
              });

      TicketResponse resp = service.create(SharedTestFixtures.KEYCLOAK_ID, req);

      assertThat(resp.priority()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("Echec – utilisateur introuvable → NotFoundException")
    void create_userNotFound_throwsNotFoundException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Echec – contrat introuvable → NotFoundException")
    void create_contractNotFound_throwsNotFoundException() {
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(contractRepo.findById(SharedTestFixtures.CONTRACT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(SharedTestFixtures.KEYCLOAK_ID, buildCreateRequest()))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("list()")
  class ListTickets {

    @Test
    @DisplayName("Succès – liste tous les tickets de l'agence (sans filtre)")
    void list_noFilter_success() throws Exception {
      Page<Ticket> page = new PageImpl<>(List.of(openTicket));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(ticketRepo.findByAgencyId(SharedTestFixtures.AGENCY_ID, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result = service.list(SharedTestFixtures.KEYCLOAK_ID, null, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – liste filtrée par statut OPEN")
    void list_withStatus_success() throws Exception {
      Page<Ticket> page = new PageImpl<>(List.of(openTicket));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(ticketRepo.findByAgencyIdAndStatus(
              SharedTestFixtures.AGENCY_ID, TicketStatus.OPEN, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result =
          service.list(
              SharedTestFixtures.KEYCLOAK_ID, TicketStatus.OPEN, null, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Succès – liste filtrée par agent assigné")
    void list_withAssignedTo_success() throws Exception {
      UUID agentId = UUID.randomUUID();
      Page<Ticket> page = new PageImpl<>(List.of(openTicket));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(ticketRepo.findByAgencyIdAndAssignedTo(
              SharedTestFixtures.AGENCY_ID, agentId, PageRequest.of(0, 10)))
          .thenReturn(page);

      var result =
          service.list(SharedTestFixtures.KEYCLOAK_ID, null, agentId, PageRequest.of(0, 10));

      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Echec – utilisateur sans agence → BadRequestException")
    void list_noAgency_throwsBadRequest() {
      User noAgency =
          SharedTestFixtures.buildUserWithoutAgency(
              SharedTestFixtures.KEYCLOAK_ID, SharedTestFixtures.USER_ID);
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(noAgency));

      assertThatThrownBy(
              () -> service.list(SharedTestFixtures.KEYCLOAK_ID, null, null, PageRequest.of(0, 10)))
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Nested
  @DisplayName("getById()")
  class GetById {

    @Test
    @DisplayName("Succès – ticket trouvé par ID")
    void getById_success() throws Exception {
      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));

      TicketResponse resp = service.getById(SharedTestFixtures.TICKET_ID, null);

      assertThat(resp).isNotNull();
      assertThat(resp.status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("Echec – ticket introuvable → NotFoundException")
    void getById_notFound_throwsNotFoundException() {
      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getById(SharedTestFixtures.TICKET_ID, null))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("assign()")
  class Assign {

    @Test
    @DisplayName("Succès – ticket assigné à un agent")
    void assign_success() throws Exception {
      User agent = SharedTestFixtures.buildPartnerUser("agent-kc", UUID.randomUUID(), agency);
      AssignTicketRequest req = new AssignTicketRequest();
      req.setAssignedToId(agent.getId());

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));
      when(userRepo.findById(agent.getId())).thenReturn(Optional.of(agent));
      when(ticketRepo.save(any(Ticket.class))).thenReturn(openTicket);

      TicketResponse resp = service.assign(SharedTestFixtures.TICKET_ID, req);

      assertThat(resp).isNotNull();
      verify(ticketRepo).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Echec – agent introuvable → NotFoundException")
    void assign_agentNotFound_throwsNotFoundException() {
      UUID agentId = UUID.randomUUID();
      AssignTicketRequest req = new AssignTicketRequest();
      req.setAssignedToId(agentId);

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));
      when(userRepo.findById(agentId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assign(SharedTestFixtures.TICKET_ID, req))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("updateStatus()")
  class UpdateStatus {

    @Test
    @DisplayName("Succès – OPEN → IN_ANALYSIS")
    void updateStatus_openToInAnalysis_success() throws Exception {
      UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
      req.setStatus("IN_ANALYSIS");

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));
      when(ticketRepo.save(any(Ticket.class))).thenReturn(openTicket);

      TicketResponse resp =
          service.updateStatus(SharedTestFixtures.TICKET_ID, req, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(resp.status()).isEqualTo(TicketStatus.IN_ANALYSIS);
    }

    @Test
    @DisplayName("Succès – RESOLVED → CLOSED avec note")
    void updateStatus_resolvedToClosed_withNote_success() throws Exception {
      Ticket resolvedTicket =
          SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.RESOLVED);

      UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
      req.setStatus("CLOSED");
      req.setResolutionNote("Problème résolu définitivement.");

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID))
          .thenReturn(Optional.of(resolvedTicket));
      when(ticketRepo.save(any(Ticket.class))).thenReturn(resolvedTicket);

      TicketResponse resp =
          service.updateStatus(SharedTestFixtures.TICKET_ID, req, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(resp.status()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("Echec – transition invalide OPEN → RESOLVED → BadRequestException")
    void updateStatus_invalidTransition_throwsBadRequest() {
      UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
      req.setStatus("RESOLVED");
      req.setResolutionNote("Note de résolution");

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));

      assertThatThrownBy(
              () ->
                  service.updateStatus(
                      SharedTestFixtures.TICKET_ID, req, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Echec – RESOLVED sans note de résolution → BadRequestException")
    void updateStatus_resolvedWithoutNote_throwsBadRequest() {
      Ticket inAnalysis =
          SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.IN_ANALYSIS);
      UpdateTicketStatusRequest req = new UpdateTicketStatusRequest();
      req.setStatus("RESOLVED");
      req.setResolutionNote(null);

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(inAnalysis));

      assertThatThrownBy(
              () ->
                  service.updateStatus(
                      SharedTestFixtures.TICKET_ID, req, SharedTestFixtures.KEYCLOAK_ID))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("obligatoire");
    }
  }

  @Nested
  @DisplayName("scheduleIntervention()")
  class ScheduleIntervention {

    @Test
    @DisplayName("Succès – IN_ANALYSIS → TECHNICIAN_SENT")
    void scheduleIntervention_inAnalysis_success() throws Exception {
      Ticket inAnalysis =
          SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.IN_ANALYSIS);

      ScheduleInterventionRequest req = new ScheduleInterventionRequest();
      req.setTechnicianName("Mamadou Diallo");
      req.setTechnicianPhone("+2250712345678");
      req.setInterventionScheduledAt(LocalDateTime.now().plusDays(2));

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(inAnalysis));
      when(ticketRepo.save(any(Ticket.class))).thenReturn(inAnalysis);

      TicketResponse resp = service.scheduleIntervention(SharedTestFixtures.TICKET_ID, req);

      assertThat(resp.status()).isEqualTo(TicketStatus.TECHNICIAN_SENT);
    }

    @Test
    @DisplayName("Echec – OPEN → TECHNICIAN_SENT transition invalide")
    void scheduleIntervention_openTicket_throwsBadRequest() {
      ScheduleInterventionRequest req = new ScheduleInterventionRequest();
      req.setTechnicianName("Tech");
      req.setTechnicianPhone("+2250712345678");
      req.setInterventionScheduledAt(LocalDateTime.now().plusDays(2));

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));

      assertThatThrownBy(() -> service.scheduleIntervention(SharedTestFixtures.TICKET_ID, req))
          .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Succès – technicien enregistré assigné via technicienId")
    void scheduleIntervention_withRegisteredTechnicien_success() throws Exception {
      Ticket inAnalysis =
          SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.IN_ANALYSIS);
      Technicien technicien = SharedTestFixtures.buildTechnicien(agency, null);

      ScheduleInterventionRequest req = new ScheduleInterventionRequest();
      req.setTechnicienId(SharedTestFixtures.TECHNICIEN_ID);
      req.setInterventionScheduledAt(LocalDateTime.now().plusDays(2));
      req.setInterventionPrice(java.math.BigDecimal.valueOf(75_000));

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(inAnalysis));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.of(technicien));
      when(ticketRepo.save(any(Ticket.class))).thenReturn(inAnalysis);

      TicketResponse resp = service.scheduleIntervention(SharedTestFixtures.TICKET_ID, req);

      assertThat(resp.status()).isEqualTo(TicketStatus.TECHNICIAN_SENT);
      assertThat(inAnalysis.getTechnicien()).isEqualTo(technicien);
      assertThat(inAnalysis.getInterventionPrice()).isEqualByComparingTo("75000");
    }

    @Test
    @DisplayName(
        "Echec – technicienId fourni mais introuvable → CustomException(NotFoundException)")
    void scheduleIntervention_registeredTechnicienNotFound_throwsCustomException() {
      Ticket inAnalysis =
          SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.IN_ANALYSIS);

      ScheduleInterventionRequest req = new ScheduleInterventionRequest();
      req.setTechnicienId(SharedTestFixtures.TECHNICIEN_ID);
      req.setInterventionScheduledAt(LocalDateTime.now().plusDays(2));

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(inAnalysis));
      when(technicienRepo.findByIdAndDeletedAtIsNull(SharedTestFixtures.TECHNICIEN_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.scheduleIntervention(SharedTestFixtures.TICKET_ID, req))
          .isInstanceOf(CustomException.class)
          .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Echec – ni technicienId ni nom/téléphone fournis → CustomException(BadRequest)")
    void scheduleIntervention_noTechnicienIdNoFields_throwsCustomException() {
      Ticket inAnalysis =
          SharedTestFixtures.buildTicket(contract, reporter, TicketStatus.IN_ANALYSIS);

      ScheduleInterventionRequest req = new ScheduleInterventionRequest();
      req.setInterventionScheduledAt(LocalDateTime.now().plusDays(2));

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(inAnalysis));

      assertThatThrownBy(() -> service.scheduleIntervention(SharedTestFixtures.TICKET_ID, req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining("technicienId");
    }
  }

  @Nested
  @DisplayName("updateRepairCost()")
  class UpdateRepairCost {

    @Test
    @DisplayName("Succès – coût de réparation enregistré")
    void updateRepairCost_success() throws Exception {
      UpdateRepairCostRequest req = new UpdateRepairCostRequest();
      req.setRepairCost(BigDecimal.valueOf(75_000));
      req.setCostImputedTo("OWNER");
      req.setResolutionNote("Joint changé sous l'évier.");

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));
      when(ticketRepo.save(any(Ticket.class))).thenReturn(openTicket);

      TicketResponse resp = service.updateRepairCost(SharedTestFixtures.TICKET_ID, req);

      assertThat(resp).isNotNull();
      verify(ticketRepo).save(any(Ticket.class));
    }
  }

  @Nested
  @DisplayName("addMessage()")
  class AddMessage {

    @Test
    @DisplayName("Succès – message PUBLIC ajouté au ticket")
    void addMessage_public_success() throws Exception {
      AddTicketMessageRequest req = new AddTicketMessageRequest();
      req.setMessage("Pouvez-vous clarifier la nature de la panne ?");
      req.setMessageType("PUBLIC");

      TicketMessage savedMsg =
          TicketMessage.builder()
              .ticket(openTicket)
              .sender(reporter)
              .message(req.getMessage())
              .messageType(TicketMessage.MessageType.PUBLIC)
              .build();
      SharedTestFixtures.injectId(savedMsg, UUID.randomUUID());

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(messageRepo.save(any(TicketMessage.class))).thenReturn(savedMsg);

      TicketMessageResponse resp =
          service.addMessage(SharedTestFixtures.TICKET_ID, req, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(resp).isNotNull();
      assertThat(resp.message()).isEqualTo("Pouvez-vous clarifier la nature de la panne ?");
    }

    @Test
    @DisplayName("Succès – message sans type fourni → défaut PUBLIC")
    void addMessage_noType_defaultsToPublic() throws Exception {
      AddTicketMessageRequest req = new AddTicketMessageRequest();
      req.setMessage("Message sans type.");
      req.setMessageType(null);

      TicketMessage savedMsg =
          TicketMessage.builder()
              .ticket(openTicket)
              .sender(reporter)
              .message(req.getMessage())
              .messageType(TicketMessage.MessageType.PUBLIC)
              .build();
      SharedTestFixtures.injectId(savedMsg, UUID.randomUUID());

      when(ticketRepo.findById(SharedTestFixtures.TICKET_ID)).thenReturn(Optional.of(openTicket));
      when(userRepo.findByKeycloakId(SharedTestFixtures.KEYCLOAK_ID))
          .thenReturn(Optional.of(reporter));
      when(messageRepo.save(any(TicketMessage.class))).thenReturn(savedMsg);

      TicketMessageResponse resp =
          service.addMessage(SharedTestFixtures.TICKET_ID, req, SharedTestFixtures.KEYCLOAK_ID);

      assertThat(resp.messageType()).isEqualTo(TicketMessage.MessageType.PUBLIC);
    }
  }

  @Nested
  @DisplayName("listMessages()")
  class ListMessages {

    @Test
    @DisplayName("Succès – liste des messages d'un ticket existant")
    void listMessages_success() throws Exception {
      TicketMessage msg =
          TicketMessage.builder()
              .ticket(openTicket)
              .sender(reporter)
              .message("Premier message")
              .messageType(TicketMessage.MessageType.PUBLIC)
              .build();
      SharedTestFixtures.injectId(msg, UUID.randomUUID());

      when(ticketRepo.existsById(SharedTestFixtures.TICKET_ID)).thenReturn(true);
      when(messageRepo.findByTicketIdOrderByCreatedAtAsc(SharedTestFixtures.TICKET_ID))
          .thenReturn(List.of(msg));

      var result = service.listMessages(SharedTestFixtures.TICKET_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).message()).isEqualTo("Premier message");
    }

    @Test
    @DisplayName("Echec – ticket inexistant → NotFoundException")
    void listMessages_ticketNotFound_throwsNotFoundException() {
      when(ticketRepo.existsById(SharedTestFixtures.TICKET_ID)).thenReturn(false);

      assertThatThrownBy(() -> service.listMessages(SharedTestFixtures.TICKET_ID))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
