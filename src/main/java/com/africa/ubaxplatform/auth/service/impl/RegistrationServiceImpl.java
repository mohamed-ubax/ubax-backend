package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.auth.entity.OtpVerification;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.OtpVerificationRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.KeycloakAdminService;
import com.africa.ubaxplatform.auth.service.interfaces.RegistrationService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.util.OtpUtils;
import com.africa.ubaxplatform.notification.service.EmailService;
import com.africa.ubaxplatform.notification.service.SmsService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'inscription mobile via OTP SMS.
 *
 * <p>Orchestration du flux en 3 étapes : envoi OTP → vérification OTP → inscription complète
 * (Keycloak + PostgreSQL + email de bienvenue).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

  private static final long OTP_TTL_MINUTES = 5;

  private final OtpVerificationRepository otpRepo;
  private final UserRepository userRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final SmsService smsService;
  private final EmailService emailService;

  // ── Étape 1 : Envoi OTP ────────────────────────────────────────

  @Override
  @Transactional
  public void sendOtp(String phone) throws CustomException {
    if (userRepo.existsByPhone(phone)) {
      throw new CustomException(
          new IllegalArgumentException("Ce numéro de téléphone est déjà enregistré"),
          ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
    }

    otpRepo
        .findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(phone, LocalDateTime.now())
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
            .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
            .build();
    otpRepo.save(otp);

    smsService.sendOtp(phone, code);
    log.info("OTP envoyé au numéro {}", phone);
  }

  // ── Étape 2 : Vérification OTP ─────────────────────────────────

  @Override
  @Transactional
  public void verifyOtp(String phone, String code) throws CustomException {
    OtpVerification otp =
        otpRepo
            .findTopByPhoneAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                phone, LocalDateTime.now())
            .orElseThrow(
                () ->
                    new CustomException(
                        new IllegalArgumentException("Aucun OTP valide trouvé pour " + phone),
                        ResponseMessageConstants.OTP_NOT_FOUND));

    if (!otp.getCode().equals(code)) {
      throw new CustomException(
          new IllegalArgumentException("Code OTP incorrect"), ResponseMessageConstants.OTP_INVALID);
    }

    otp.setUsed(true);
    otpRepo.save(otp);
    log.info("OTP vérifié avec succès pour le numéro {}", phone);
  }

  // ── Étape 3 : Inscription complète ─────────────────────────────

  @Override
  @Transactional
  public RegisterResponse register(RegisterCompleteRequest request) throws CustomException {
    if (request.getEmail() != null
        && !request.getEmail().isBlank()
        && userRepo.existsByEmail(request.getEmail())) {
      throw new CustomException(
          new IllegalArgumentException("Cet email est déjà utilisé"),
          ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
    }
    if (userRepo.existsByPhone(request.getPhone())) {
      throw new CustomException(
          new IllegalArgumentException("Ce numéro de téléphone est déjà enregistré"),
          ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
    }

    String keycloakId = keycloakAdminService.createUser(request);
    log.info("Utilisateur créé dans Keycloak: {}", keycloakId);

    try {
      keycloakAdminService.assignRole(keycloakId, UserRole.CLIENT);

      User.UserBuilder<?, ?> userBuilder =
          User.builder()
              .keycloakId(keycloakId)
              .firstName(request.getFirstName())
              .lastName(request.getLastName())
              .phone(request.getPhone())
              .roles(new HashSet<>(Set.of(UserRole.CLIENT)))
              .phoneVerified(true)
              .emailVerified(false);
      if (request.getEmail() != null && !request.getEmail().isBlank()) {
        userBuilder.email(request.getEmail());
      }
      User user = userBuilder.build();
      user = userRepo.save(user);
      log.info("Utilisateur persisté en DB: {}", user.getId());

      if (user.getEmail() != null && !user.getEmail().isBlank()) {
        emailService.sendWelcome(user.getEmail(), request.getFirstName());
      }

      return RegisterResponse.builder()
          .userId(user.getId())
          .keycloakId(keycloakId)
          .email(user.getEmail())
          .phone(user.getPhone())
          .firstName(user.getFirstName())
          .lastName(user.getLastName())
          .roles(user.getRoles())
          .build();

    } catch (Exception e) {
      log.error(
          "Échec inscription DB pour keycloakId={}. Rollback Keycloak en cours...", keycloakId);
      keycloakAdminService.deleteUser(keycloakId);
      throw e;
    }
  }
}
