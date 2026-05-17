package com.africa.ubaxplatform.tenant.mapper;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.tenant.dto.TenantResponse;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import org.springframework.stereotype.Component;

/**
 * Mapper statique entre l'entité {@link Tenant} et son DTO de sortie {@link TenantResponse}.
 *
 * <p>Centralise la conversion {@code Tenant → TenantResponse} pour éviter la duplication dans les
 * services.
 *
 * <p>Usage :
 *
 * <pre>{@code
 * TenantResponse response = TenantMapper.toResponse(tenant);
 * }</pre>
 */
@Component
public class TenantMapper {

  private TenantMapper() {}

  /**
   * Convertit un {@link Tenant} en {@link TenantResponse}.
   *
   * <p>Les informations d'identité (nom complet, email) sont extraites de la relation {@code user}
   * associée au dossier locataire.
   *
   * @param t entité à convertir (non null)
   * @return DTO de réponse complet
   */
  public static TenantResponse toResponse(Tenant t) {
    User u = t.getUser();
    return new TenantResponse(
        t.getId(),
        t.getProperty() != null ? t.getProperty().getId() : null,
        u != null ? u.getId() : null,
        t.getFullName(),
        u != null ? u.getEmail() : null,
        t.getEmploymentStatus(),
        t.getEmployerName(),
        t.getMonthlyIncome(),
        t.isHasGuarantor(),
        t.getGuarantorName(),
        t.getGuarantorPhone(),
        t.getGuarantorEmail(),
        t.getIdDocumentUrl(),
        t.getIdDocumentType(),
        t.getIdDocumentNumber(),
        t.getIdDocumentExpiry(),
        t.getIncomeProofUrl(),
        t.getAddressProofUrl(),
        t.getStatus(),
        t.isQualified(),
        t.getQualifiedAt(),
        t.getRejectionReason(),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
