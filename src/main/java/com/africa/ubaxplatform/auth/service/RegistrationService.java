package com.africa.ubaxplatform.auth.service;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RegisterCompleteRequest;
import com.africa.ubaxplatform.auth.dto.RegisterResponse;
import com.africa.ubaxplatform.auth.entity.OtpVerification;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.OtpVerificationRepository;
import com.africa.ubaxplatform.auth.repository.UserRepository;
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
 * Orchestration de l'inscription mobile via numéro de téléphone.
 *
 * <p>Flux complet :
 *
 * <ol>
 *   <li>Envoi OTP → {@link #sendOtp(String)}
 *   <li>Vérification OTP → {@link #verifyOtp(String, String)}
 *   <li>Completion → {@link #register(RegisterCompleteRequest)} : Keycloak d'abord, puis PostgreSQL
 *       avec rollback automatique en cas d'échec DB.
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

  private static final long OTP_TTL_MINUTES = 5;

  private final OtpVerificationRepository otpRepo;
  private final UserRepository userRepo;
  private final KeycloakAdminService keycloakAdminService;
  private final SmsService smsService;
  private final EmailService emailService;

  // ── Étape 1 : Envoi OTP ────────────────────────────────────────

  /**
   * Génère un code OTP à 6 chiffres, le persiste et l'envoie par SMS au numéro fourni.
   *
   * @param phone numéro de téléphone au format international
   */
  @Transactional
  public void sendOtp(String phone) throws CustomException {
    if (userRepo.existsByPhone(phone)) {
      throw new CustomException(
          new IllegalArgumentException("Ce numéro de téléphone est déjà enregistré"),
          ResponseMessageConstants.USER_CREATE_FAILURE_ALREADY_EXISTS);
    }

    // Invalider les anciens OTP non utilisés pour ce numéro
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

  /**
   * Vérifie le code OTP soumis par l'utilisateur.
   *
   * @param phone numéro de téléphone
   * @param code code OTP reçu par SMS
   * @throws CustomException si le code est invalide, expiré ou déjà utilisé
   */
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

  /**
   * Finalise l'inscription :
   *
   * <ol>
   *   <li>Crée l'utilisateur dans Keycloak.
   *   <li>Si succès, persiste dans PostgreSQL.
   *   <li>Assigne le rôle {@code UBAX_CLIENT} dans Keycloak.
   *   <li>Envoie un email de bienvenue.
   * </ol>
   *
   * <p>En cas d'échec après création Keycloak, le compte Keycloak est supprimé (rollback manuel).
   *
   * @param request informations complètes d'inscription
   * @return données de l'utilisateur créé
   */
  @Transactional
  public RegisterResponse register(RegisterCompleteRequest request) throws CustomException {
    // Vérifications de doublons
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

    // 1. Créer dans Keycloak
    String keycloakId = keycloakAdminService.createUser(request);
    log.info("Utilisateur créé dans Keycloak: {}", keycloakId);

    try {
      // 2. Assigner le rôle CLIENT dans Keycloak
      keycloakAdminService.assignRole(keycloakId, UserRole.CLIENT);

      // 3. Persister dans PostgreSQL
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

      // 4. Email de bienvenue (asynchrone – ne bloque pas)
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
      // Rollback Keycloak si la persistence DB échoue
      log.error(
          "Échec inscription DB pour keycloakId={}. Rollback Keycloak en cours...", keycloakId);
      keycloakAdminService.deleteUser(keycloakId);
      throw e;
    }
  }
}
