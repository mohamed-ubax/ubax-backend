package com.africa.ubaxplatform.unitTest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.auth.entity.OtpVerification;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.OtpVerificationRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.impl.RegistrationServiceImpl;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.notification.service.SmsService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationServiceImpl – tests unitaires")
class RegistrationServiceImplTest {

  @Mock private OtpVerificationRepository otpRepo;
  @Mock private UserRepository userRepo;
  @Mock private KeycloakAdminService keycloakAdminService;
  @Mock private SmsService smsService;
  @Mock private EmailService emailService;

  @InjectMocks private RegistrationServiceImpl service;

  @Nested
  @DisplayName("sendOtp()")
  class SendOtp {

    @Test
    @DisplayName("Succès – OTP sauvegardé et SMS envoyé")
    void sendOtp_success_savesOtpAndSendsSms() throws CustomException {
      String phone = "+2250700000001";
      when(userRepo.existsByPhone(phone)).thenReturn(false);
      when(otpRepo.findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(phone), any(LocalDateTime.class)))
          .thenReturn(Optional.empty());
      when(otpRepo.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

      service.sendOtp(phone);

      ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
      verify(otpRepo).save(captor.capture());
      OtpVerification saved = captor.getValue();
      assertThat(saved.getPhone()).isEqualTo(phone);
      assertThat(saved.getCode()).hasSize(6);
      assertThat(saved.isUsed()).isFalse();
      assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

      verify(smsService).sendOtp(eq(phone), eq(saved.getCode()));
    }

    @Test
    @DisplayName("Échec – numéro de téléphone déjà enregistré")
    void sendOtp_phoneAlreadyExists_throwsCustomException() {
      String phone = "+2250700000002";
      when(userRepo.existsByPhone(phone)).thenReturn(true);

      assertThatThrownBy(() -> service.sendOtp(phone))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);

      verify(otpRepo, never()).save(any());
      verify(smsService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("OTP précédent encore actif – marqué utilisé avant création du nouveau")
    void sendOtp_existingActiveOtp_marksOldUsedAndCreatesNew() throws CustomException {
      String phone = "+2250700000003";
      OtpVerification oldOtp =
          OtpVerification.builder()
              .phone(phone)
              .code("111111")
              .expiresAt(LocalDateTime.now().plusMinutes(3))
              .build();

      when(userRepo.existsByPhone(phone)).thenReturn(false);
      when(otpRepo.findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(phone), any(LocalDateTime.class)))
          .thenReturn(Optional.of(oldOtp));
      when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.sendOtp(phone);

      // Ancien OTP marqué utilisé
      assertThat(oldOtp.isUsed()).isTrue();
      // save appelé 2 fois : old OTP + new OTP
      verify(otpRepo).save(oldOtp);
      // Un SMS envoyé avec un nouveau code
      verify(smsService).sendOtp(eq(phone), anyString());
    }
  }

  @Nested
  @DisplayName("verifyOtp()")
  class VerifyOtp {

    @Test
    @DisplayName("Succès – OTP correct, marqué utilisé")
    void verifyOtp_success_marksOtpUsed() throws CustomException {
      String phone = "+2250700000010";
      String code = "654321";
      OtpVerification otp =
          OtpVerification.builder()
              .phone(phone)
              .code(code)
              .expiresAt(LocalDateTime.now().plusMinutes(4))
              .build();

      when(otpRepo.findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(phone), any(LocalDateTime.class)))
          .thenReturn(Optional.of(otp));
      when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.verifyOtp(phone, code);

      assertThat(otp.isUsed()).isTrue();
      verify(otpRepo).save(otp);
    }

    @Test
    @DisplayName("Échec – aucun OTP valide trouvé")
    void verifyOtp_noOtpFound_throwsCustomException() {
      String phone = "+2250700000011";
      when(otpRepo.findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(phone), any(LocalDateTime.class)))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.verifyOtp(phone, "123456"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.OTP_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – code OTP incorrect")
    void verifyOtp_wrongCode_throwsCustomException() {
      String phone = "+2250700000012";
      OtpVerification otp =
          OtpVerification.builder()
              .phone(phone)
              .code("999999")
              .expiresAt(LocalDateTime.now().plusMinutes(4))
              .build();

      when(otpRepo.findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(phone), any(LocalDateTime.class)))
          .thenReturn(Optional.of(otp));

      assertThatThrownBy(() -> service.verifyOtp(phone, "000000"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.OTP_INVALID);
    }
  }

  @Nested
  @DisplayName("register()")
  class Register {

    private RegisterCompleteRequest buildRequest(String phone, String email) {
      RegisterCompleteRequest req = new RegisterCompleteRequest();
      req.setPhone(phone);
      req.setEmail(email);
      req.setFirstName("Moussa");
      req.setLastName("Diallo");
      req.setPassword("Secret123!");
      req.setTitle("M.");
      return req;
    }

    private User buildSavedUser(String phone, String email, String keycloakId) {
      User user =
          User.builder()
              .keycloakId(keycloakId)
              .phone(phone)
              .email(email)
              .firstName("Moussa")
              .lastName("Diallo")
              .phoneVerified(true)
              .emailVerified(false)
              .roles(new HashSet<>(Set.of(UserRole.CLIENT)))
              .build();
      // simulate JPA-assigned ID
      try {
        var idField = com.africa.ubaxplatform.common.base.BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, UUID.randomUUID());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return user;
    }

    @Test
    @DisplayName("Succès – avec email, bienvenue envoyée")
    void register_success_withEmail_returnsRegisterResponse() throws Exception {
      String phone = "+2250700000020";
      String email = "moussa@example.com";
      String keycloakId = UUID.randomUUID().toString();

      when(userRepo.existsByEmail(email)).thenReturn(false);
      when(userRepo.existsByPhone(phone)).thenReturn(false);
      when(keycloakAdminService.createUser(any())).thenReturn(keycloakId);
      User saved = buildSavedUser(phone, email, keycloakId);
      when(userRepo.save(any(User.class))).thenReturn(saved);

      RegisterCompleteRequest req = buildRequest(phone, email);
      RegisterResponse response = service.register(req);

      assertThat(response).isNotNull();
      assertThat(response.getPhone()).isEqualTo(phone);
      assertThat(response.getKeycloakId()).isEqualTo(keycloakId);
      assertThat(response.getRoles()).contains(UserRole.CLIENT);
      verify(keycloakAdminService).assignRole(keycloakId, UserRole.CLIENT);
      verify(emailService).sendWelcome(eq(email), eq("Moussa"));
    }

    @Test
    @DisplayName("Succès – sans email, pas de bienvenue envoyée")
    void register_success_withoutEmail_noWelcomeEmail() throws Exception {
      String phone = "+2250700000021";
      String keycloakId = UUID.randomUUID().toString();

      when(userRepo.existsByPhone(phone)).thenReturn(false);
      when(keycloakAdminService.createUser(any())).thenReturn(keycloakId);
      User saved = buildSavedUser(phone, null, keycloakId);
      when(userRepo.save(any(User.class))).thenReturn(saved);

      RegisterCompleteRequest req = buildRequest(phone, null);
      service.register(req);

      verify(emailService, never()).sendWelcome(anyString(), anyString());
    }

    @Test
    @DisplayName("Échec – email déjà utilisé")
    void register_emailAlreadyExists_throwsCustomException() throws CustomException {
      String phone = "+2250700000022";
      String email = "taken@example.com";
      when(userRepo.existsByEmail(email)).thenReturn(true);

      RegisterCompleteRequest req = buildRequest(phone, email);

      assertThatThrownBy(() -> service.register(req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);

      verify(keycloakAdminService, never()).createUser(any());
    }

    @Test
    @DisplayName("Échec – numéro de téléphone déjà utilisé")
    void register_phoneAlreadyExists_throwsCustomException() throws CustomException {
      String phone = "+2250700000023";
      String email = "unique@example.com";
      when(userRepo.existsByEmail(email)).thenReturn(false);
      when(userRepo.existsByPhone(phone)).thenReturn(true);

      RegisterCompleteRequest req = buildRequest(phone, email);

      assertThatThrownBy(() -> service.register(req))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);

      verify(keycloakAdminService, never()).createUser(any());
    }

    @Test
    @DisplayName("Échec – erreur Keycloak après createUser → rollback deleteUser appelé")
    void register_keycloakFailureAfterCreate_rollsBackDeleteUser() throws Exception {
      String phone = "+2250700000024";
      String email = "fail@example.com";
      String keycloakId = UUID.randomUUID().toString();

      when(userRepo.existsByEmail(email)).thenReturn(false);
      when(userRepo.existsByPhone(phone)).thenReturn(false);
      when(keycloakAdminService.createUser(any())).thenReturn(keycloakId);
      doThrow(new RuntimeException("Keycloak role assignment failure"))
          .when(keycloakAdminService)
          .assignRole(anyString(), any(UserRole.class));

      RegisterCompleteRequest req = buildRequest(phone, email);

      assertThatThrownBy(() -> service.register(req)).isInstanceOf(RuntimeException.class);

      verify(keycloakAdminService).deleteUser(keycloakId);
    }
  }
}
