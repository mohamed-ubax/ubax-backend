package com.africa.ubaxplatform.property.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.dto.PropertyBoostRequest;
import com.africa.ubaxplatform.property.dto.PropertyCreateRequest;
import com.africa.ubaxplatform.property.dto.PropertyDetailResponse;
import com.africa.ubaxplatform.property.dto.PropertyDocumentAddRequest;
import com.africa.ubaxplatform.property.dto.PropertyDocumentResponse;
import com.africa.ubaxplatform.property.dto.PropertyExpirationRequest;
import com.africa.ubaxplatform.property.dto.PropertyMediaAddRequest;
import com.africa.ubaxplatform.property.dto.PropertyMediaResponse;
import com.africa.ubaxplatform.property.dto.PropertyResponse;
import com.africa.ubaxplatform.property.dto.PropertyStatusUpdateRequest;
import com.africa.ubaxplatform.property.dto.PropertyUpdateRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface PropertyService {

  // ── CRUD principal ─────────────────────────────────────────────

  PropertyResponse create(String callerKeycloakId, PropertyCreateRequest request)
      throws CustomException;

  PropertyDetailResponse getById(UUID id) throws CustomException;

  Page<PropertyResponse> list(
      PropertyStatus status,
      String city,
      String propertyType,
      String transactionType,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      UUID agencyId,
      UUID ownerId,
      UUID hotelId,
      Pageable pageable);

  /** Liste les biens de l'agence/propriétaire authentifié, tous statuts. */
  Page<PropertyResponse> listMine(String callerKeycloakId, PropertyStatus status, Pageable pageable)
      throws CustomException;

  /**
   * Admin : tous les biens liés à un hôtel (hotel IS NOT NULL), filtrable par statut et hotelId.
   */
  Page<PropertyResponse> listHotelProperties(
      PropertyStatus status, UUID hotelId, Pageable pageable);

  /**
   * Admin : tous les biens liés à une agence (agency IS NOT NULL), filtrable par statut et
   * agencyId.
   */
  Page<PropertyResponse> listAgencyProperties(
      PropertyStatus status, UUID agencyId, Pageable pageable);

  PropertyResponse update(UUID id, String callerKeycloakId, PropertyUpdateRequest request)
      throws CustomException;

  /** Soumet un brouillon pour modération (DRAFT → PENDING). */
  PropertyResponse submit(UUID id, String callerKeycloakId) throws CustomException;

  /** Archive un bien (statut → ARCHIVED). Appelé par le propriétaire. */
  void archive(UUID id, String callerKeycloakId) throws CustomException;

  /** Modération admin : met à jour le statut (PUBLISHED, REJECTED, etc.). */
  PropertyResponse updateStatus(UUID id, PropertyStatusUpdateRequest request)
      throws CustomException;

  /** Admin : active ou prolonge le boost d'une annonce pour {@code boostDays} jours. */
  PropertyResponse boost(UUID id, PropertyBoostRequest request) throws CustomException;

  /** Admin : retire le boost d'une annonce immédiatement. */
  PropertyResponse removeBoost(UUID id) throws CustomException;

  /** Admin : définit la date d'expiration automatique d'une annonce publiée. */
  PropertyResponse setExpiration(UUID id, PropertyExpirationRequest request) throws CustomException;

  /** Admin : supprime la date d'expiration d'une annonce (l'annonce ne s'archivera pas). */
  PropertyResponse removeExpiration(UUID id) throws CustomException;

  // ── Médias ─────────────────────────────────────────────────────

  PropertyMediaResponse addMedia(
      UUID propertyId, String callerKeycloakId, PropertyMediaAddRequest request)
      throws CustomException;

  /**
   * Upload multipart + lien atomique : upload vers MinIO ET création de PropertyMedia en une
   * transaction.
   */
  PropertyMediaResponse uploadAndLinkMedia(
      UUID propertyId, String callerKeycloakId, MultipartFile file, String mediaType, boolean cover)
      throws CustomException;

  List<PropertyMediaResponse> listMedia(UUID propertyId) throws CustomException;

  PropertyMediaResponse setCover(UUID propertyId, UUID mediaId, String callerKeycloakId)
      throws CustomException;

  void deleteMedia(UUID propertyId, UUID mediaId, String callerKeycloakId) throws CustomException;

  // ── Documents ──────────────────────────────────────────────────

  PropertyDocumentResponse addDocument(
      UUID propertyId, String callerKeycloakId, PropertyDocumentAddRequest request)
      throws CustomException;

  List<PropertyDocumentResponse> listDocuments(UUID propertyId, String callerKeycloakId)
      throws CustomException;

  PropertyDocumentResponse verifyDocument(
      UUID propertyId, UUID docId, String callerKeycloakId, String note) throws CustomException;

  void deleteDocument(UUID propertyId, UUID docId, String callerKeycloakId) throws CustomException;
}
