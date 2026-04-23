package com.africa.ubaxplatform.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.auth.codeList.OtpPurpose;
import com.africa.ubaxplatform.auth.entity.OtpVerification;
import com.africa.ubaxplatform.auth.repository.OtpVerificationRepository;
import com.africa.ubaxplatform.auth.service.impl.PasswordResetServiceImpl;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.notification.service.SmsService;
import java.time.LocalDateTime;
import java.util.Optional;
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
@DisplayName("PasswordResetServiceImpl – tests unitaires")
class PasswordResetServiceImplTest {

  @Mock private OtpVerificationRepository otpRepo;
  @Mock private KeycloakAdminService keycloakAdminService;
  @Mock private SmsService smsService;

  @InjectMocks private PasswordResetServiceImpl service;

  private static final String PHONE = "+2250711111111";
  private static final String KEYCLOAK_ID = UUID.randomUUID().toString();

  private OtpVerification validOtp(String code) {
    return OtpVerification.builder()
        .phone(PHONE)
        .code(code)
        .purpose(OtpPurpose.PASSWORD_RESET)
        .expiresAt(LocalDateTime.now().plusMinutes(4))
        .build();
  }

  @Nested
  @DisplayName("sendResetOtp()")
  class SendResetOtp {

    @Test
    @DisplayName("Succès – OTP sauvegardé et SMS envoyé")
    void sendResetOtp_success_savesOtpAndSendsSms() throws CustomException {
      when(keycloakAdminService.findUserIdByPhone(PHONE)).thenReturn(KEYCLOAK_ID);
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.empty());
      when(otpRepo.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

      service.sendResetOtp(PHONE);

      ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
      verify(otpRepo).save(captor.capture());
      OtpVerification saved = captor.getValue();
      assertThat(saved.getPhone()).isEqualTo(PHONE);
      assertThat(saved.getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
      assertThat(saved.getCode()).hasSize(6);
      assertThat(saved.isUsed()).isFalse();

      verify(smsService).sendOtp(eq(PHONE), eq(saved.getCode()));
    }

    @Test
    @DisplayName("Échec – utilisateur introuvable dans Keycloak")
    void sendResetOtp_userNotFound_throwsCustomException() {
      when(keycloakAdminService.findUserIdByPhone(PHONE))
          .thenThrow(new NotFoundException("Aucun utilisateur"));

      assertThatThrownBy(() -> service.sendResetOtp(PHONE))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);

      verify(otpRepo, never()).save(any());
      verify(smsService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("OTP précédent encore actif – marqué utilisé avant création du nouveau")
    void sendResetOtp_existingActiveOtp_marksOldUsed() throws CustomException {
      OtpVerification oldOtp = validOtp("555555");
      when(keycloakAdminService.findUserIdByPhone(PHONE)).thenReturn(KEYCLOAK_ID);
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.of(oldOtp));
      when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.sendResetOtp(PHONE);

      assertThat(oldOtp.isUsed()).isTrue();
      verify(otpRepo).save(oldOtp);
      verify(smsService).sendOtp(eq(PHONE), anyString());
    }
  }

  @Nested
  @DisplayName("verifyResetOtp()")
  class VerifyResetOtp {

    @Test
    @DisplayName("Succès – OTP valide et code correct")
    void verifyResetOtp_success() throws CustomException {
      String code = "123456";
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.of(validOtp(code)));

      // Ne doit pas lancer d'exception
      service.verifyResetOtp(PHONE, code);
    }

    @Test
    @DisplayName("Échec – aucun OTP valide trouvé")
    void verifyResetOtp_noOtpFound_throwsCustomException() {
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.verifyResetOtp(PHONE, "000000"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.OTP_NOT_FOUND);
    }

    @Test
    @DisplayName("Échec – code OTP incorrect")
    void verifyResetOtp_wrongCode_throwsCustomException() {
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.of(validOtp("999999")));

      assertThatThrownBy(() -> service.verifyResetOtp(PHONE, "000000"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.OTP_INVALID);
    }
  }

  @Nested
  @DisplayName("resetPassword()")
  class ResetPassword {

    @Test
    @DisplayName("Succès – OTP correct, mot de passe réinitialisé, OTP marqué utilisé")
    void resetPassword_success() throws CustomException {
      String code = "456789";
      String newPassword = "NewPass123!";
      OtpVerification otp = validOtp(code);

      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.of(otp));
      when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(keycloakAdminService.findUserIdByPhone(PHONE)).thenReturn(KEYCLOAK_ID);

      service.resetPassword(PHONE, code, newPassword);

      assertThat(otp.isUsed()).isTrue();
      verify(otpRepo).save(otp);
      verify(keycloakAdminService).resetPassword(KEYCLOAK_ID, newPassword, false);
    }

    @Test
    @DisplayName("Échec – aucun OTP valide trouvé")
    void resetPassword_noOtpFound_throwsCustomException() throws CustomException {
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.resetPassword(PHONE, "000000", "NewPass123!"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.OTP_NOT_FOUND);

      verify(keycloakAdminService, never()).resetPassword(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Échec – code OTP incorrect")
    void resetPassword_wrongCode_throwsCustomException() throws CustomException {
      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.of(validOtp("777777")));

      assertThatThrownBy(() -> service.resetPassword(PHONE, "000000", "NewPass123!"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.OTP_INVALID);

      // anyBoolean() pour les primitifs boolean — any() retourne null et cause un NPE
      verify(keycloakAdminService, never()).resetPassword(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Échec – utilisateur introuvable dans Keycloak après OTP valide")
    void resetPassword_userNotFoundInKeycloak_throwsCustomException() throws CustomException {
      String code = "321654";
      OtpVerification otp = validOtp(code);

      when(otpRepo.findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
              eq(PHONE), eq(OtpPurpose.PASSWORD_RESET), any(LocalDateTime.class)))
          .thenReturn(Optional.of(otp));
      when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(keycloakAdminService.findUserIdByPhone(PHONE))
          .thenThrow(new NotFoundException("Aucun utilisateur"));

      assertThatThrownBy(() -> service.resetPassword(PHONE, code, "NewPass123!"))
          .isInstanceOf(CustomException.class)
          .hasMessageContaining(ResponseMessageConstants.USER_NOT_FOUND);

      verify(keycloakAdminService, never())
          .resetPassword(anyString(), anyString(), any(Boolean.class));
    }
  }
}
