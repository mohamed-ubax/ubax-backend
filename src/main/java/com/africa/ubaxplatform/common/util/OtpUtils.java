package com.africa.ubaxplatform.common.util;

import java.security.SecureRandom;

/** Utilitaires pour la génération de codes OTP et mots de passe temporaires. */
public final class OtpUtils {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String TEMP_PASSWORD_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

  private OtpUtils() {}

  public static String generateOtp() {
    int code = 100000 + RANDOM.nextInt(900000);
    return String.valueOf(code);
  }

  /**
   * Génère un mot de passe temporaire de 8 caractères alphanumériques (sans caractères ambigus).
   */
  public static String generateTempPassword() {
    StringBuilder sb = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
      sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }
}
