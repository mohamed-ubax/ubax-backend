package com.africa.ubaxplatform.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service d'envoi d'emails transactionnels via JavaMail + Thymeleaf.
 *
 * <p>Tous les envois sont asynchrones ({@code @Async}) pour ne pas bloquer le thread HTTP.
 * Templates HTML situés dans {@code src/main/resources/templates/email/}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value("${ubax.mail.from:no-reply@ubax.io}")
  private String from;

  @Value("${ubax.mail.from-name:UBAX Platform}")
  private String fromName;

  @Async
  public void sendOtp(String to, String otp, long ttlMinutes) {
    Context ctx = new Context();
    ctx.setVariable("otp", otp);
    ctx.setVariable("ttlMinutes", ttlMinutes);
    sendHtml(to, "Votre code de vérification UBAX", "email/otp", ctx);
  }

  @Async
  public void sendWelcome(String to, String firstName) {
    Context ctx = new Context();
    ctx.setVariable("firstName", firstName);
    sendHtml(to, "Bienvenue sur UBAX Platform !", "email/welcome", ctx);
  }

  @Async
  public void sendPasswordReset(String to, String firstName, String resetLink) {
    Context ctx = new Context();
    ctx.setVariable("firstName", firstName);
    ctx.setVariable("resetLink", resetLink);
    sendHtml(to, "Réinitialisation de votre mot de passe UBAX", "email/password-reset", ctx);
  }

  @Async
  public void sendHtml(
      String to, String subject, String templateName, Map<String, Object> variables) {
    Context ctx = new Context();
    variables.forEach(ctx::setVariable);
    sendHtml(to, subject, templateName, ctx);
  }

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
