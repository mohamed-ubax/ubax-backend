package com.africa.ubaxplatform.document.generator;

import com.africa.ubaxplatform.payment.entity.Payment;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceGenerator {

  private final TemplateEngine templateEngine;

  public byte[] generate(Payment payment) {
    Context ctx = new Context();
    ctx.setVariable("payment", payment);
    ctx.setVariable("contract", payment.getContract());
    ctx.setVariable("property", payment.getProperty());
    ctx.setVariable("tenant", payment.getTenant());
    ctx.setVariable("agency", payment.getAgency());
    ctx.setVariable("today", LocalDate.now());

    String html = templateEngine.process("invoice", ctx);
    return renderToPdf(html);
  }

  private byte[] renderToPdf(String html) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (Exception e) {
      log.error("Erreur génération PDF facture : {}", e.getMessage());
      throw new RuntimeException("Échec de la génération de la facture PDF", e);
    }
  }
}
