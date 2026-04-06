package com.africa.ubaxplatform.common.util;

import java.security.SecureRandom;

/** Utilitaires pour la génération de codes OTP. */
public final class OtpUtils {

  private static final SecureRandom RANDOM = new SecureRandom();

  private OtpUtils() {}

  public static String generateOtp() {
    int code = 100000 + RANDOM.nextInt(900000);
    return String.valueOf(code);
  }
}
