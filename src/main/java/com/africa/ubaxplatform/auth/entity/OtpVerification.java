package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.auth.codeList.OtpPurpose;
import com.africa.ubaxplatform.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Code OTP généré pour la vérification du numéro de téléphone lors de l'inscription mobile ou de la
 * récupération de mot de passe.
 *
 * <p>Un OTP est valide pendant {@code expiresAt} et ne peut être utilisé qu'une seule fois ({@code
 * used = true} après vérification). Les codes expirés ou utilisés sont rejetés.
 */
@Entity
@Table(name = "otp_verifications", schema = "administrative")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification extends BaseEntity {

  /** Numéro de téléphone au format international (ex : {@code +2250712345678}). */
  @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  /** But de l'OTP : inscription ou récupération de mot de passe. */
  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 20)
  @lombok.Builder.Default
  private OtpPurpose purpose = OtpPurpose.REGISTRATION;

  /** Code OTP à 6 chiffres. */
  @Column(name = "code", nullable = false, length = 6)
  private String code;

  /** Date et heure d'expiration du code (généralement +5 minutes). */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  /** {@code true} si le code a déjà été validé (usage unique). */
  @Column(name = "used", nullable = false)
  @lombok.Builder.Default
  private boolean used = false;

  /** Retourne {@code true} si le code est encore valide (non expiré, non utilisé). */
  public boolean isValid() {
    return !used && LocalDateTime.now().isBefore(expiresAt);
  }
}
