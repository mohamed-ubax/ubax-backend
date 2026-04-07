package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.OtpPurpose;
import com.africa.ubaxplatform.auth.entity.OtpVerification;
import com.africa.ubaxplatform.auth.repository.OtpVerificationRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.interfaces.PasswordResetService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.util.OtpUtils;
import com.africa.ubaxplatform.notification.service.SmsService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de réinitialisation de mot de passe via OTP SMS.
 *
 * <p>Orchestration du flow en 3 étapes : envoi OTP → vérification OTP → réinitialisation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final long OTP_TTL_MINUTES = 5;

  private final OtpVerificationRepository otpRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final SmsService smsService;

  // ── Étape 1 : Envoi OTP ────────────────────────────────────────

  @Override
  @Transactional
  public void sendResetOtp(String phone) throws CustomException {
    try {
      keycloakAdminService.findUserIdByPhone(phone);
    } catch (NotFoundException e) {
      throw new CustomException(e, ResponseMessageConstants.USER_NOT_FOUND);
    }

    otpRepo
        .findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            phone, OtpPurpose.PASSWORD_RESET, LocalDateTime.now())
        .ifPresent(
            old -> {
              old.setUsed(true);
              otpRepo.save(old);
            });

    String code = OtpUtils.generateOtp();
    OtpVerification otp =
        OtpVerification.builder()
            .phone(phone)
            .code(code)
            .purpose(OtpPurpose.PASSWORD_RESET)
            .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
            .build();
    otpRepo.save(otp);

    smsService.sendOtp(phone, code);
    log.info("OTP reset mot de passe envoyé au numéro {}", phone);
  }

  // ── Étape 2 : Vérification OTP ─────────────────────────────────

  @Override
  public void verifyResetOtp(String phone, String code) throws CustomException {
    OtpVerification otp =
        otpRepo
            .findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                phone, OtpPurpose.PASSWORD_RESET, LocalDateTime.now())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Aucun OTP valide trouvé pour " + phone),
                        ResponseMessageConstants.OTP_NOT_FOUND));

    if (!otp.getCode().equals(code)) {
      throw new CustomException(
          new IllegalArgumentException("Code OTP incorrect"), ResponseMessageConstants.OTP_INVALID);
    }

    log.info("OTP reset vérifié (non consommé) pour {}", phone);
  }

  // ── Étape 3 : Réinitialisation du mot de passe ─────────────────

  @Override
  @Transactional
  public void resetPassword(String phone, String code, String newPassword) throws CustomException {
    OtpVerification otp =
        otpRepo
            .findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                phone, OtpPurpose.PASSWORD_RESET, LocalDateTime.now())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Aucun OTP valide trouvé pour " + phone),
                        ResponseMessageConstants.OTP_NOT_FOUND));

    if (!otp.getCode().equals(code)) {
      throw new CustomException(
          new IllegalArgumentException("Code OTP incorrect"), ResponseMessageConstants.OTP_INVALID);
    }

    otp.setUsed(true);
    otpRepo.save(otp);

    String keycloakId;
    try {
      keycloakId = keycloakAdminService.findUserIdByPhone(phone);
    } catch (NotFoundException e) {
      throw new CustomException(e, ResponseMessageConstants.USER_NOT_FOUND);
    }

    keycloakAdminService.resetPassword(keycloakId, newPassword, false);
    log.info("Mot de passe réinitialisé avec succès pour le numéro {}", phone);
  }
}
