package com.africa.ubaxplatform.testHelper;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import java.util.UUID;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Builders de fixtures pour les tests unitaires du module partner.
 *
 * <p>Centralise la construction des objets de test pour éviter la duplication entre les classes de
 * test et garantir une cohérence des données de référence.
 *
 * <p>Convention :
 *
 * <ul>
 *   <li>Type par défaut = HOTEL (non-agence) pour les tests génériques.
 *   <li>Utiliser AGENCE_IMMOBILIERE explicitement pour tester le provisionnement agence.
 * </ul>
 */
public final class PartnerTestFixtures {

  private PartnerTestFixtures() {}

  /** Requête avec type HOTEL (non-agence) pour les tests génériques. */
  public static PartnerApplicationRequest buildRequest() {
    return buildRequest(Constants.CodeList.PartnerType.HOTEL);
  }

  public static PartnerApplicationRequest buildRequest(String partnerType) {
    PartnerApplicationRequest req = new PartnerApplicationRequest();
    req.setCompanyName("Acme SARL");
    req.setLegalRepresentative("Jean Dupont");
    req.setEmail("contact@acme.ci");
    req.setPhone("+2250711111111");
    req.setCountry("CI");
    req.setCity("Abidjan");
    req.setPostalAddress("BP 123");
    req.setZone("Zone Nord");
    req.setDescription("Partenaire immobilier");
    req.setLegalStatus("SARL");
    req.setRegistrationNumber("CI-ABJ-2024-001");
    req.setPartnerType(partnerType);
    return req;
  }

  /**
   * Application avec ID et partnerType HOTEL (non-agence) par défaut. Utiliser {@link
   * #buildApplicationWithIdAndType} pour un type explicite.
   */
  public static PartnerApplication buildApplicationWithId(UUID id, ApplicationStatus status) {
    return buildApplicationWithIdAndType(id, status, Constants.CodeList.PartnerType.HOTEL);
  }

  /**
   * Application avec ID et partnerType explicites.
   *
   * @param partnerType {@code Constants.CodeList.PartnerType.AGENCE_IMMOBILIERE} ou {@code HOTEL}
   */
  public static PartnerApplication buildApplicationWithIdAndType(
      UUID id, ApplicationStatus status, String partnerType) {
    PartnerApplication app = buildApplication(status, partnerType);
    injectId(app, id);
    return app;
  }

  public static User buildAdmin(String keycloakId) {
    return User.builder()
        .keycloakId(keycloakId)
        .firstName("Admin")
        .lastName("Ubax")
        .email("admin@ubax.com")
        .build();
  }

  public static MockMultipartFile pdfFile(String name) {
    return new MockMultipartFile(name, name + ".pdf", "application/pdf", new byte[100]);
  }

  public static MockMultipartFile pngFile(String name) {
    return new MockMultipartFile(name, name + ".png", "image/png", new byte[100]);
  }

  private static PartnerApplication buildApplication(ApplicationStatus status, String partnerType) {
    return PartnerApplication.builder()
        .companyName("Acme SARL")
        .legalRepresentative("Jean Dupont")
        .email("contact@acme.ci")
        .phone("+2250711111111")
        .country("CI")
        .city("Abidjan")
        .postalAddress("BP 123")
        .zone("Zone Nord")
        .description("Partenaire immobilier")
        .legalStatus("SARL")
        .registrationNumber("CI-ABJ-2024-001")
        .storageSlug("acme-sarl")
        .partnerType(partnerType)
        .status(status)
        .build();
  }

  private static void injectId(Object entity, UUID id) {
    try {
      var idField = BaseEntity.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException("Impossible d'injecter l'ID dans l'entité : " + e.getMessage(), e);
    }
  }
}
