package com.africa.ubaxplatform.document.generator;

import com.africa.ubaxplatform.contract.entity.Contract;
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
public class ContractGenerator {

  private final TemplateEngine templateEngine;

  public byte[] generate(Contract contract) {
    Context ctx = new Context();
    ctx.setVariable("contract", contract);
    ctx.setVariable("property", contract.getProperty());
    ctx.setVariable("owner", contract.getOwner());
    ctx.setVariable("tenant", contract.getTenant());
    ctx.setVariable("createdBy", contract.getCreatedBy());
    ctx.setVariable("today", LocalDate.now());

    String html = templateEngine.process("contract", ctx);
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
      log.error("Erreur génération PDF contrat : {}", e.getMessage());
      throw new RuntimeException("Échec de la génération du contrat PDF", e);
    }
  }
}
