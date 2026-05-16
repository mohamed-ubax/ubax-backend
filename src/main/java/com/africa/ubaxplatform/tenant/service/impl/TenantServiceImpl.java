package com.africa.ubaxplatform.tenant.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.tenant.codeList.TenantStatus;
import com.africa.ubaxplatform.tenant.dto.TenantCreateRequest;
import com.africa.ubaxplatform.tenant.dto.TenantResponse;
import com.africa.ubaxplatform.tenant.dto.TenantUpdateRequest;
import com.africa.ubaxplatform.tenant.entity.Tenant;
import com.africa.ubaxplatform.tenant.mapper.TenantMapper;
import com.africa.ubaxplatform.tenant.repository.TenantRepository;
import com.africa.ubaxplatform.tenant.service.interfaces.TenantService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

  private final TenantRepository tenantRepo;
  private final UserRepository userRepo;

  // Helpers

  private User resolveUser(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private Tenant resolveTenant(UUID id) throws CustomException {
    return tenantRepo
        .findById(id)
        .filter(t -> t.getDeletedAt() == null)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Dossier locataire introuvable"),
                    ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND));
  }

  private TenantResponse toResponse(Tenant t) {
    return TenantMapper.toResponse(t);
  }

  // CRUD locataire

  @Override
  @Transactional
  public TenantResponse create(String keycloakId, TenantCreateRequest request)
      throws CustomException {
    User user = resolveUser(keycloakId);
    if (tenantRepo.existsByUserId(user.getId())) {
      throw new CustomException(
          new ConflictException("Un dossier locataire existe déjà pour cet utilisateur"),
          ResponseMessageConstants.TENANT_CREATE_FAILURE);
    }

    Tenant tenant =
        Tenant.builder()
            .user(user)
            .employmentStatus(request.employmentStatus())
            .employerName(request.employerName())
            .monthlyIncome(request.monthlyIncome())
            .hasGuarantor(Boolean.TRUE.equals(request.hasGuarantor()))
            .guarantorName(request.guarantorName())
            .guarantorPhone(request.guarantorPhone())
            .guarantorEmail(request.guarantorEmail())
            .idDocumentUrl(request.idDocumentUrl())
            .idDocumentType(request.idDocumentType())
            .idDocumentNumber(request.idDocumentNumber())
            .idDocumentExpiry(
                request.idDocumentExpiry() != null
                    ? LocalDate.parse(request.idDocumentExpiry())
                    : null)
            .incomeProofUrl(request.incomeProofUrl())
            .addressProofUrl(request.addressProofUrl())
            .status(TenantStatus.INCOMPLETE)
            .build();

    if (isDossierComplete(tenant)) {
      tenant.setStatus(TenantStatus.PENDING_REVIEW);
    }

    return toResponse(tenantRepo.save(tenant));
  }

  @Override
  @Transactional(readOnly = true)
  public TenantResponse getMyProfile(String keycloakId) throws CustomException {
    User user = resolveUser(keycloakId);
    Tenant tenant =
        tenantRepo
            .findByUserId(user.getId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Dossier locataire introuvable"),
                        ResponseMessageConstants.TENANT_GET_FAILURE_NOT_FOUND));
    return toResponse(tenant);
  }

  @Override
  @Transactional
  public TenantResponse updateMyProfile(String keycloakId, TenantUpdateRequest request)
      throws CustomException {
    User user = resolveUser(keycloakId);
    Tenant tenant =
        tenantRepo
            .findByUserId(user.getId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Dossier locataire introuvable"),
                        ResponseMessageConstants.TENANT_UPDATE_FAILURE_NOT_FOUND));

    if (request.employmentStatus() != null) tenant.setEmploymentStatus(request.employmentStatus());
    if (request.employerName() != null) tenant.setEmployerName(request.employerName());
    if (request.monthlyIncome() != null) tenant.setMonthlyIncome(request.monthlyIncome());
    if (request.hasGuarantor() != null) tenant.setHasGuarantor(request.hasGuarantor());
    if (request.guarantorName() != null) tenant.setGuarantorName(request.guarantorName());
    if (request.guarantorPhone() != null) tenant.setGuarantorPhone(request.guarantorPhone());
    if (request.guarantorEmail() != null) tenant.setGuarantorEmail(request.guarantorEmail());
    if (request.idDocumentUrl() != null) tenant.setIdDocumentUrl(request.idDocumentUrl());
    if (request.idDocumentType() != null) tenant.setIdDocumentType(request.idDocumentType());
    if (request.idDocumentNumber() != null) tenant.setIdDocumentNumber(request.idDocumentNumber());
    if (request.idDocumentExpiry() != null)
      tenant.setIdDocumentExpiry(LocalDate.parse(request.idDocumentExpiry()));
    if (request.incomeProofUrl() != null) tenant.setIncomeProofUrl(request.incomeProofUrl());
    if (request.addressProofUrl() != null) tenant.setAddressProofUrl(request.addressProofUrl());

    // Réévalue le statut : INCOMPLETE ou REJECTED → PENDING_REVIEW si le dossier est complet
    if ((tenant.getStatus() == TenantStatus.INCOMPLETE
            || tenant.getStatus() == TenantStatus.REJECTED)
        && isDossierComplete(tenant)) {
      tenant.setStatus(TenantStatus.PENDING_REVIEW);
    }

    return toResponse(tenantRepo.save(tenant));
  }

  @Override
  @Transactional(readOnly = true)
  public TenantResponse getById(String keycloakId, UUID id) throws CustomException {
    User caller = resolveUser(keycloakId);
    if (caller.getHotel() != null && caller.getAgency() == null) {
      throw new UnAuthorizedException(
          "Les partenaires hôteliers n'ont pas accès aux dossiers locataires");
    }
    return toResponse(resolveTenant(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TenantResponse> list(String keycloakId, TenantStatus status, Pageable pageable)
      throws CustomException {
    User caller = resolveUser(keycloakId);

    // Partenaire hôtel → accès refusé (pas de dossiers KYC pour les hôtels)
    if (caller.getHotel() != null && caller.getAgency() == null) {
      throw new UnAuthorizedException(
          "Les partenaires hôteliers n'ont pas accès aux dossiers locataires");
    }

    // Partenaire agence → uniquement les locataires liés à leurs contrats
    if (caller.getAgency() != null) {
      UUID agencyId = caller.getAgency().getId();
      if (status != null) {
        return tenantRepo.findByAgencyIdAndStatus(agencyId, status, pageable).map(this::toResponse);
      }
      return tenantRepo.findByAgencyId(agencyId, pageable).map(this::toResponse);
    }

    // Admin / Super Admin → tous les dossiers non archivés
    if (status != null) {
      return tenantRepo.findByStatusAndDeletedAtIsNull(status, pageable).map(this::toResponse);
    }
    return tenantRepo.findByDeletedAtIsNull(pageable).map(this::toResponse);
  }

  @Override
  @Transactional
  public TenantResponse qualify(UUID id, String reviewerKeycloakId) throws CustomException {
    User reviewer = resolveUser(reviewerKeycloakId);
    Tenant tenant = resolveTenant(id);

    if (tenant.getStatus() != TenantStatus.PENDING_REVIEW) {
      throw new CustomException(
          new BadRequestException(
              "Seul un dossier en PENDING_REVIEW peut être qualifié. Statut actuel : "
                  + tenant.getStatus()),
          ResponseMessageConstants.TENANT_UPDATE_FAILURE);
    }

    tenant.setStatus(TenantStatus.QUALIFIED);
    tenant.setQualified(true);
    tenant.setQualifiedAt(LocalDateTime.now());
    tenant.setQualifiedBy(reviewer);
    tenant.setRejectionReason(null);

    return toResponse(tenantRepo.save(tenant));
  }

  @Override
  @Transactional
  public TenantResponse reject(UUID id, String reason) throws CustomException {
    Tenant tenant = resolveTenant(id);

    if (reason == null || reason.isBlank()) {
      throw new CustomException(
          new BadRequestException("Le motif de refus est obligatoire"),
          ResponseMessageConstants.TENANT_UPDATE_FAILURE);
    }

    tenant.setStatus(TenantStatus.REJECTED);
    tenant.setQualified(false);
    tenant.setRejectionReason(reason);

    return toResponse(tenantRepo.save(tenant));
  }

  @Override
  @Transactional
  public void delete(UUID id) throws CustomException {
    Tenant tenant = resolveTenant(id);
    tenant.setDeletedAt(LocalDateTime.now());
    tenantRepo.save(tenant);
  }

  // Seule la pièce d'identité est requise — le justificatif de revenus reste optionnel
  // (marché africain : beaucoup de travailleurs informels sans bulletin de salaire)
  private boolean isDossierComplete(Tenant t) {
    return t.getIdDocumentUrl() != null
        && t.getIdDocumentNumber() != null
        && t.getIdDocumentExpiry() != null;
  }
}
