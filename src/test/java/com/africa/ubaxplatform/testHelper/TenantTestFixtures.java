package com.africa.ubaxplatform.testHelper;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.dto.TenantCreateRequest;
import com.africa.ubaxplatform.tenant.dto.TenantUpdateRequest;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Builders de fixtures pour les tests unitaires du module tenant.
 *
 * <p>Convention :
 *
 * <ul>
 *   <li>Un dossier "minimal" contient uniquement les champs obligatoires.
 *   <li>Un dossier "complet" contient tous les champs nécessaires à {@code isDossierComplete()}.
 * </ul>
 */
public final class TenantTestFixtures {

  private TenantTestFixtures() {}

  public static final String KEYCLOAK_ID = UUID.randomUUID().toString();
  public static final UUID USER_ID = UUID.randomUUID();
  public static final UUID TENANT_ID = UUID.randomUUID();

  public static User buildUser() {
    return buildUser(KEYCLOAK_ID, USER_ID);
  }

  public static User buildUser(String keycloakId, UUID userId) {
    User user =
        User.builder()
            .keycloakId(keycloakId)
            .firstName("Amara")
            .lastName("Koné")
            .email("amara.kone@example.ci")
            .phone("+2250711223344")
            .build();
    injectId(user, userId);
    return user;
  }

  /** Dossier INCOMPLETE minimal — champs obligatoires uniquement. */
  public static Tenant buildTenant(UUID id, TenantStatus status, User user) {
    Tenant tenant =
        Tenant.builder()
            .user(user)
            .employmentStatus("EMPLOYEE")
            .employerName("Société Ivoirienne")
            .monthlyIncome(BigDecimal.valueOf(500_000))
            .hasGuarantor(false)
            .status(status)
            .build();
    injectId(tenant, id);
    return tenant;
  }

  /**
   * Dossier complet — satisfait {@code isDossierComplete()} et peut passer à PENDING_REVIEW.
   * Contient idDocumentUrl, idDocumentNumber, idDocumentExpiry et incomeProofUrl.
   */
  public static Tenant buildCompleteTenant(UUID id, TenantStatus status, User user) {
    Tenant tenant =
        Tenant.builder()
            .user(user)
            .employmentStatus("EMPLOYEE")
            .employerName("Société Ivoirienne")
            .monthlyIncome(BigDecimal.valueOf(500_000))
            .hasGuarantor(false)
            .idDocumentUrl("https://minio/tenant-documents/id-card.pdf")
            .idDocumentType("CNI")
            .idDocumentNumber("CI-2024-001")
            .idDocumentExpiry(LocalDate.now().plusYears(2))
            .incomeProofUrl("https://minio/tenant-documents/income.pdf")
            .addressProofUrl("https://minio/tenant-documents/address.pdf")
            .status(status)
            .build();
    injectId(tenant, id);
    return tenant;
  }

  public static TenantCreateRequest buildCreateRequest() {
    return new TenantCreateRequest(
        "EMPLOYEE", "Société Ivoirienne", BigDecimal.valueOf(500_000), false, null, null, null);
  }

  public static TenantCreateRequest buildCreateRequestWithGuarantor() {
    return new TenantCreateRequest(
        "STUDENT",
        null,
        BigDecimal.valueOf(150_000),
        true,
        "Papa Koné",
        "+2250722334455",
        "papa.kone@example.ci");
  }

  /** Mise à jour partielle — uniquement les champs KYC pour compléter le dossier. */
  public static TenantUpdateRequest buildCompleteUpdateRequest() {
    return new TenantUpdateRequest(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "https://minio/tenant-documents/id-card.pdf",
        "CNI",
        "CI-2024-001",
        LocalDate.now().plusYears(2).toString(),
        "https://minio/tenant-documents/income.pdf",
        "https://minio/tenant-documents/address.pdf");
  }

  /** Mise à jour partielle — uniquement la situation professionnelle. */
  public static TenantUpdateRequest buildPartialUpdateRequest() {
    return new TenantUpdateRequest(
        "SELF_EMPLOYED",
        "Mon Entreprise",
        BigDecimal.valueOf(750_000),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static void injectId(Object entity, UUID id) {
    try {
      var idField = BaseEntity.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException("Impossible d'injecter l'ID : " + e.getMessage(), e);
    }
  }
}
