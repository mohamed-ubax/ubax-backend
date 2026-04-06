package com.africa.ubaxplatform.auth.service;

import com.africa.ubaxplatform.auth.codeList.OtpPurpose;
import com.africa.ubaxplatform.auth.entity.OtpVerification;
import com.africa.ubaxplatform.auth.repository.OtpVerificationRepository;
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
 * Orchestration du flow de récupération de mot de passe via OTP SMS.
 *
 * <ol>
 *   <li>Envoi OTP → {@link #sendResetOtp(String)}
 *   <li>Vérification OTP → {@link #verifyResetOtp(String, String)}
 *   <li>Réinitialisation → {@link #resetPassword(String, String, String)}
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

  private static final long OTP_TTL_MINUTES = 5;

  private final OtpVerificationRepository otpRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final SmsService smsService;

  // ── Étape 1 : Envoi OTP ────────────────────────────────────────

  /**
   * Vérifie que le téléphone est enregistré, génère un OTP PASSWORD_RESET et l'envoie par SMS.
   *
   * @param phone numéro de téléphone au format international
   */
  @Transactional
  public void sendResetOtp(String phone) throws CustomException {
    // Vérifie que l'utilisateur existe dans Keycloak (attribut phone)
    try {
      keycloakAdminService.findUserIdByPhone(phone);
    } catch (NotFoundException e) {
      throw new CustomException(e, ResponseMessageConstants.USER_NOT_FOUND);
    }

    // Invalider les anciens OTP de reset non utilisés pour ce numéro
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

  /**
   * Vérifie le code OTP sans le consommer (permet à l'UI de passer à l'étape 3).
   *
   * @param phone numéro de téléphone
   * @param code code OTP reçu par SMS
   */
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

  /**
   * Vérifie le code OTP (le consomme), puis réinitialise le mot de passe via Keycloak.
   *
   * @param phone numéro de téléphone
   * @param code code OTP reçu par SMS
   * @param newPassword nouveau mot de passe en clair
   */
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

    // Consommer l'OTP
    otp.setUsed(true);
    otpRepo.save(otp);

    // Récupérer le keycloakId via l'attribut phone dans Keycloak
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
