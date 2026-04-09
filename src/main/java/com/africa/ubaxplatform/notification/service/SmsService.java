package com.africa.ubaxplatform.notification.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service d'envoi de SMS via l'API LAfricaMobile (LAMPUSH).
 *
 * <p>Utilise le format JSON disponible sur {@code POST https://lamsms.lafricamobile.com/api}.
 */
@Service
@Slf4j
public class SmsService {

  @Value("${lam.sms.api-url}")
  private String apiUrl;

  @Value("${lam.sms.account-id}")
  private String accountId;

  @Value("${lam.sms.password}")
  private String password;

  @Value("${lam.sms.sender}")
  private String sender;

  private final RestClient restClient;

  public SmsService(@Qualifier("smsRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * Envoie un SMS à un seul destinataire.
   *
   * @param to numéro de téléphone au format international (ex : {@code +2250712345678})
   * @param text contenu du message SMS
   */
  public void sendSms(String to, String text) {
    // LAM LAMPUSH: to must be an array of objects {key: phoneNumber}
    // Phone number can use international format (+221XXXXXXXXX)
    Map<String, Object> payload =
        Map.of(
            "accountid", accountId,
            "password", password,
            "sender", sender,
            "to", List.of(Map.of("1", to)),
            "text", text);
    log.info("SMS sending to {}", payload);

    try {
      String response =
          restClient
              .post()
              .uri(apiUrl)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(String.class);
      log.info("SMS envoyé à {} – réponse LAM: {}", to, response);
    } catch (Exception e) {
      log.error("Échec envoi SMS à {}: {}", to, e.getMessage());
      throw new RuntimeException("Échec d'envoi du SMS : " + e.getMessage(), e);
    }
  }

  /**
   * Envoie un code OTP par SMS.
   *
   * @param to numéro de téléphone du destinataire
   * @param otp code OTP à 6 chiffres
   */
  public void sendOtp(String to, String otp) {
    String message =
        "Votre code de vérification UBAX est : "
            + otp
            + ". Ce code expire dans 5 minutes. Ne le partagez pas.";
    sendSms(to, message);
  }
}
