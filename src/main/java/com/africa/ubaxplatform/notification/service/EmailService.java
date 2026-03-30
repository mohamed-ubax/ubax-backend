package com.africa.ubaxplatform.notification.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails transactionnels.
 *
 * <p>Désactivé temporairement (spring-boot-starter-mail et thymeleaf commentés dans pom.xml).
 * Réactiver les dépendances + restaurer l'implémentation complète pour la mise en production.
 */
@Service
@Slf4j
public class EmailService {

  @Async
  public void sendOtp(String to, String otp, long ttlMinutes) {
    log.info("[EMAIL DISABLED] OTP {} pour {} (valide {} min)", otp, to, ttlMinutes);
  }

  @Async
  public void sendWelcome(String to, String firstName) {
    log.info("[EMAIL DISABLED] Bienvenue {} — {}", firstName, to);
  }

  @Async
  public void sendPasswordReset(String to, String firstName, String resetLink) {
    log.info("[EMAIL DISABLED] Reset password pour {} — {}", firstName, to);
  }

  @Async
  public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
    log.info("[EMAIL DISABLED] sendHtml template={} to={} subject={}", templateName, to, subject);
  }
}


/**
 * Service d'envoi d'emails transactionnels via JavaMail + Thymeleaf.
 *
 * <p>Tous les envois sont asynchrones ({@code @Async}) pour ne pas bloquer le thread HTTP.
 * Templates HTML situés dans {@code src/main/resources/templates/email/}.
 */
/*
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value("${ubax.mail.from}")
  private String from;

  @Value("${ubax.mail.from-name}")
  private String fromName;

  // ── Méthodes métier ────────────────────────────────────────────

  */
/**
   * Envoie un code OTP par email.
   *
   * @param to adresse email destinataire
   * @param otp code à 6 chiffres
   * @param ttlMinutes durée de validité en minutes
   *//*

  @Async
  public void sendOtp(String to, String otp, long ttlMinutes) {
    Context ctx = new Context();
    ctx.setVariable("otp", otp);
    ctx.setVariable("ttlMinutes", ttlMinutes);
    sendHtml(to, "Votre code de vérification UBAX", "email/otp", ctx);
  }

  */
/**
   * Envoie un email de bienvenue après inscription.
   *
   * @param to adresse email
   * @param firstName prénom de l'utilisateur
   *//*

  @Async
  public void sendWelcome(String to, String firstName) {
    Context ctx = new Context();
    ctx.setVariable("firstName", firstName);
    sendHtml(to, "Bienvenue sur UBAX Platform !", "email/welcome", ctx);
  }

  */
/**
   * Envoie un email de réinitialisation de mot de passe (lien généré via Keycloak).
   *
   * @param to adresse email
   * @param firstName prénom
   * @param resetLink lien de réinitialisation Keycloak
   *//*

  @Async
  public void sendPasswordReset(String to, String firstName, String resetLink) {
    Context ctx = new Context();
    ctx.setVariable("firstName", firstName);
    ctx.setVariable("resetLink", resetLink);
    sendHtml(to, "Réinitialisation de votre mot de passe UBAX", "email/password-reset", ctx);
  }

  */
/**
   * Envoie un email HTML générique avec des variables Thymeleaf.
   *
   * @param to destinataire
   * @param subject objet du mail
   * @param templateName nom du template sans extension (ex: "email/otp")
   * @param variables variables à injecter dans le template
   *//*

  @Async
  public void sendHtml(
      String to, String subject, String templateName, Map<String, Object> variables) {
    Context ctx = new Context();
    variables.forEach(ctx::setVariable);
    sendHtml(to, subject, templateName, ctx);
  }

  // ── Méthode interne ────────────────────────────────────────────

  private void sendHtml(String to, String subject, String templateName, Context ctx) {
    try {
      String html = templateEngine.process(templateName, ctx);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(
              message,
              MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
              StandardCharsets.UTF_8.name());

      helper.setFrom(from, fromName);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(html, true);

      mailSender.send(message);
      log.info("Email envoyé à {} ({})", to, subject);
    } catch (MessagingException | java.io.UnsupportedEncodingException e) {
      log.error("Échec envoi email à {} : {}", to, e.getMessage());
      throw new RuntimeException("Échec de l'envoi de l'email", e);
    }
  }
}
*/
