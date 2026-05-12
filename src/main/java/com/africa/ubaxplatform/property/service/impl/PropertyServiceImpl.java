package com.africa.ubaxplatform.property.service.impl;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.dto.PropertyAmenityRequest;
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
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.entity.PropertyAmenity;
import com.africa.ubaxplatform.property.entity.PropertyDocument;
import com.africa.ubaxplatform.property.entity.PropertyMedia;
import com.africa.ubaxplatform.property.mapper.PropertyMapper;
import com.africa.ubaxplatform.property.repository.PropertyAmenityRepository;
import com.africa.ubaxplatform.property.repository.PropertyDocumentRepository;
import com.africa.ubaxplatform.property.repository.PropertyMediaRepository;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import com.africa.ubaxplatform.property.service.interfaces.PropertyService;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyServiceImpl implements PropertyService {

  private final PropertyRepository propertyRepo;
  private final PropertyAmenityRepository amenityRepo;
  private final PropertyMediaRepository mediaRepo;
  private final PropertyDocumentRepository docRepo;
  private final UserRepository userRepo;
  private final MinioService minioService;

  private static final String BUCKET_MEDIA = "properties-media";
  private static final Set<String> ALLOWED_MEDIA_MIMES =
      Set.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime");

  // ── Helpers ────────────────────────────────────────────────────

  private User resolveUser(String keycloakId) throws CustomException {
    return userRepo
        .findByKeycloakId(keycloakId)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Utilisateur introuvable"),
                    ResponseMessageConstants.USER_NOT_FOUND));
  }

  private Property resolveProperty(UUID id) throws CustomException {
    return propertyRepo
        .findById(id)
        .orElseThrow(
            () ->
                new CustomException(
                    new NotFoundException("Bien immobilier introuvable"),
                    ResponseMessageConstants.PROPERTY_GET_FAILURE_NOT_FOUND));
  }

  private String resolveCoverPhotoUrl(UUID propertyId) {
    return mediaRepo
        .findByPropertyIdAndCoverTrue(propertyId)
        .map(PropertyMedia::getFileUrl)
        .orElse(null);
  }

  private List<PropertyAmenity> buildAmenities(
      List<PropertyAmenityRequest> requests, Property property) {
    List<PropertyAmenity> result = new ArrayList<>();
    for (PropertyAmenityRequest req : requests) {
      result.add(
          PropertyAmenity.builder()
              .property(property)
              .code(req.code())
              .customValue(req.customValue())
              .customDescription(req.customDescription())
              .build());
    }
    return result;
  }

  private void requireOwnership(Property property, User caller) throws CustomException {
    boolean isOwner = property.getOwner().getId().equals(caller.getId());
    boolean isAgency =
        property.getAgency() != null
            && caller.getAgency() != null
            && property.getAgency().getId().equals(caller.getAgency().getId());
    if (!isOwner && !isAgency) {
      log.warn(
          "[OWNERSHIP] Accès refusé | propertyId={} callerId={} ownerIdAttendu={}",
          property.getId(),
          caller.getId(),
          property.getOwner().getId());
      throw new CustomException(
          new UnAuthorizedException("Accès refusé – vous n'êtes pas le gestionnaire de ce bien"),
          ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }
  }

  // ── CRUD principal ─────────────────────────────────────────────

  @Override
  @Transactional
  public PropertyResponse create(String callerKeycloakId, PropertyCreateRequest req)
      throws CustomException {
    log.info(
        "[CREATE] caller={} | title='{}' | type={} | tx={} | city={} | price={}",
        callerKeycloakId,
        req.title(),
        req.propertyType(),
        req.transactionType(),
        req.city(),
        req.price());
    log.debug(
        "[CREATE] amenities={} | bedType={} | mealPlan={} | paymentFreq={}",
        req.amenities(),
        req.bedType(),
        req.mealPlan(),
        req.paymentFrequency());

    User caller = resolveUser(callerKeycloakId);

    User owner = caller;
    if (req.ownerId() != null && !req.ownerId().equals(caller.getId())) {
      log.debug("[CREATE] ownerId spécifié → {}", req.ownerId());
      owner =
          userRepo
              .findById(req.ownerId())
              .orElseThrow(
                  () ->
                      new CustomException(
                          new NotFoundException("Propriétaire introuvable"),
                          ResponseMessageConstants.USER_NOT_FOUND));
    }

    Property property =
        Property.builder()
            .owner(owner)
            .agency(caller.getAgency())
            .title(req.title())
            .description(req.description())
            .propertyType(req.propertyType())
            .transactionType(req.transactionType())
            .price(req.price())
            .condition(req.condition())
            .yearBuilt(req.yearBuilt())
            .surfaceTotal(req.surfaceTotal())
            .surfaceLiving(req.surfaceLiving())
            .rooms(req.rooms())
            .bedrooms(req.bedrooms())
            .bathrooms(req.bathrooms())
            .balconies(req.balconies() != null ? req.balconies() : 0)
            .floor(req.floor())
            .totalFloors(req.totalFloors())
            .address(req.address())
            .city(req.city())
            .district(req.district())
            .street(req.street())
            .latitude(req.latitude())
            .longitude(req.longitude())
            .bedType(req.bedType())
            .maxOccupancy(req.maxOccupancy())
            .mealPlan(req.mealPlan())
            .paymentFrequency(req.paymentFrequency())
            .status(PropertyStatus.DRAFT)
            .build();

    Property saved = propertyRepo.save(property);
    log.info(
        "[CREATE] ✔ bien créé | id={} | statut={} | agencyId={}",
        saved.getId(),
        saved.getStatus(),
        saved.getAgency() != null ? saved.getAgency().getId() : "null");

    if (req.amenities() != null && !req.amenities().isEmpty()) {
      List<PropertyAmenity> amenities = buildAmenities(req.amenities(), saved);
      amenityRepo.saveAll(amenities);
      saved.getAmenities().addAll(amenities);
      log.debug("[CREATE] {} commodité(s) sauvegardée(s)", amenities.size());
    }

    PropertyResponse response = PropertyMapper.toResponse(saved);
    log.debug(
        "[CREATE] response → id={} status={} amenities={}",
        response.id(),
        response.status(),
        response.amenities() != null ? response.amenities().size() : 0);
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public PropertyDetailResponse getById(UUID id) throws CustomException {
    Property property = resolveProperty(id);
    List<PropertyMediaResponse> media =
        mediaRepo.findByPropertyIdOrderBySortOrderAsc(id).stream()
            .map(PropertyMapper::toMediaResponse)
            .toList();
    List<PropertyDocumentResponse> documents =
        docRepo.findByPropertyIdOrderByCreatedAtDesc(id).stream()
            .map(PropertyMapper::toDocumentResponse)
            .toList();
    String coverPhotoUrl =
        media.stream()
            .filter(PropertyMediaResponse::cover)
            .map(PropertyMediaResponse::fileUrl)
            .findFirst()
            .orElse(null);
    return new PropertyDetailResponse(
        PropertyMapper.toResponse(property, coverPhotoUrl), media, documents);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PropertyResponse> list(
      PropertyStatus status,
      String city,
      String propertyType,
      String transactionType,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      UUID agencyId,
      UUID ownerId,
      Pageable pageable) {
    log.debug(
        "[LIST] status={} city={} type={} tx={} minPrice={} maxPrice={} page={} size={}",
        status,
        city,
        propertyType,
        transactionType,
        minPrice,
        maxPrice,
        pageable.getPageNumber(),
        pageable.getPageSize());
    return propertyRepo
        .findWithFilters(
            status,
            city != null ? city.toLowerCase() : null,
            propertyType,
            transactionType,
            minPrice,
            maxPrice,
            agencyId,
            ownerId,
            pageable)
        .map(p -> PropertyMapper.toResponse(p, resolveCoverPhotoUrl(p.getId())));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PropertyResponse> listMine(
      String callerKeycloakId, PropertyStatus status, Pageable pageable) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    UUID agencyId = caller.getAgency() != null ? caller.getAgency().getId() : null;
    UUID ownerId = agencyId == null ? caller.getId() : null;
    return propertyRepo
        .findWithFilters(status, null, null, null, null, null, agencyId, ownerId, pageable)
        .map(p -> PropertyMapper.toResponse(p, resolveCoverPhotoUrl(p.getId())));
  }

  @Override
  @Transactional
  public PropertyResponse update(UUID id, String callerKeycloakId, PropertyUpdateRequest req)
      throws CustomException {
    log.info("[UPDATE] propertyId={} | caller={}", id, callerKeycloakId);
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(id);
    requireOwnership(property, caller);

    if (property.getStatus() == PropertyStatus.PUBLISHED
        || property.getStatus() == PropertyStatus.SOLD
        || property.getStatus() == PropertyStatus.ARCHIVED) {
      log.warn(
          "[UPDATE] Modification refusée | propertyId={} | statut={}", id, property.getStatus());
      throw new CustomException(
          new BadRequestException(
              "Un bien publié, vendu ou archivé ne peut pas être modifié directement"),
          ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }

    if (req.title() != null) property.setTitle(req.title());
    if (req.description() != null) property.setDescription(req.description());
    if (req.propertyType() != null) property.setPropertyType(req.propertyType());
    if (req.transactionType() != null) property.setTransactionType(req.transactionType());
    if (req.price() != null) property.setPrice(req.price());
    if (req.condition() != null) property.setCondition(req.condition());
    if (req.yearBuilt() != null) property.setYearBuilt(req.yearBuilt());
    if (req.surfaceTotal() != null) property.setSurfaceTotal(req.surfaceTotal());
    if (req.surfaceLiving() != null) property.setSurfaceLiving(req.surfaceLiving());
    if (req.rooms() != null) property.setRooms(req.rooms());
    if (req.bedrooms() != null) property.setBedrooms(req.bedrooms());
    if (req.bathrooms() != null) property.setBathrooms(req.bathrooms());
    if (req.balconies() != null) property.setBalconies(req.balconies());
    if (req.floor() != null) property.setFloor(req.floor());
    if (req.totalFloors() != null) property.setTotalFloors(req.totalFloors());
    if (req.address() != null) property.setAddress(req.address());
    if (req.city() != null) property.setCity(req.city());
    if (req.district() != null) property.setDistrict(req.district());
    if (req.street() != null) property.setStreet(req.street());
    if (req.latitude() != null) property.setLatitude(req.latitude());
    if (req.longitude() != null) property.setLongitude(req.longitude());
    if (req.amenities() != null) {
      amenityRepo.deleteAllByPropertyId(id);
      property.getAmenities().clear();
      if (!req.amenities().isEmpty()) {
        List<PropertyAmenity> newAmenities = buildAmenities(req.amenities(), property);
        amenityRepo.saveAll(newAmenities);
        property.getAmenities().addAll(newAmenities);
      }
    }
    if (req.bedType() != null) property.setBedType(req.bedType());
    if (req.maxOccupancy() != null) property.setMaxOccupancy(req.maxOccupancy());
    if (req.mealPlan() != null) property.setMealPlan(req.mealPlan());
    if (req.paymentFrequency() != null) property.setPaymentFrequency(req.paymentFrequency());

    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  @Override
  @Transactional
  public PropertyResponse submit(UUID id, String callerKeycloakId) throws CustomException {
    log.info("[SUBMIT] propertyId={} | caller={}", id, callerKeycloakId);
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(id);
    requireOwnership(property, caller);

    if (property.getStatus() != PropertyStatus.DRAFT) {
      log.warn(
          "[SUBMIT] Soumission refusée | propertyId={} | statut={} (attendu: DRAFT)",
          id,
          property.getStatus());
      throw new CustomException(
          new BadRequestException(
              "Seul un bien en brouillon (DRAFT) peut être soumis. Statut actuel : "
                  + property.getStatus()),
          ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }

    property.setStatus(PropertyStatus.PENDING);
    log.info("[SUBMIT] ✔ DRAFT → PENDING | propertyId={}", id);
    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  @Override
  @Transactional
  public void archive(UUID id, String callerKeycloakId) throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(id);
    requireOwnership(property, caller);
    property.setStatus(PropertyStatus.ARCHIVED);
    propertyRepo.save(property);
  }

  @Override
  @Transactional
  public PropertyResponse updateStatus(UUID id, PropertyStatusUpdateRequest req)
      throws CustomException {
    log.info("[STATUS] propertyId={} | {} → {}", id, "?", req.status());
    Property property = resolveProperty(id);
    log.info("[STATUS] propertyId={} | {} → {}", id, property.getStatus(), req.status());

    if (req.status() == PropertyStatus.REJECTED
        && (req.rejectionReason() == null || req.rejectionReason().isBlank())) {
      log.warn("[STATUS] REJECTED sans motif | propertyId={}", id);
      throw new CustomException(
          new BadRequestException("Le motif de refus est obligatoire pour le statut REJECTED"),
          ResponseMessageConstants.PROPERTY_UPDATE_FAILURE);
    }

    property.setStatus(req.status());
    property.setRejectionReason(req.rejectionReason());

    if (req.status() == PropertyStatus.PUBLISHED && property.getPublishedAt() == null) {
      property.setPublishedAt(LocalDateTime.now());
      log.info(
          "[STATUS] ✔ PUBLISHED | propertyId={} | publishedAt={}", id, property.getPublishedAt());
    }

    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  // ── Configuration admin (boost / expiration) ───────────────────

  @Override
  @Transactional
  public PropertyResponse boost(UUID id, PropertyBoostRequest req) throws CustomException {
    Property property = resolveProperty(id);
    property.setBoosted(true);
    property.setBoostExpiresAt(LocalDateTime.now().plusDays(req.boostDays()));
    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  @Override
  @Transactional
  public PropertyResponse removeBoost(UUID id) throws CustomException {
    Property property = resolveProperty(id);
    property.setBoosted(false);
    property.setBoostExpiresAt(null);
    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  @Override
  @Transactional
  public PropertyResponse setExpiration(UUID id, PropertyExpirationRequest req)
      throws CustomException {
    Property property = resolveProperty(id);
    property.setExpiresAt(req.expiresAt());
    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  @Override
  @Transactional
  public PropertyResponse removeExpiration(UUID id) throws CustomException {
    Property property = resolveProperty(id);
    property.setExpiresAt(null);
    return PropertyMapper.toResponse(propertyRepo.save(property));
  }

  // ── Médias ─────────────────────────────────────────────────────

  @Override
  @Transactional
  public PropertyMediaResponse addMedia(
      UUID propertyId, String callerKeycloakId, PropertyMediaAddRequest req)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(propertyId);
    requireOwnership(property, caller);

    if (Boolean.TRUE.equals(req.cover())) {
      mediaRepo.clearCoversForProperty(propertyId);
    }

    PropertyMedia media =
        PropertyMedia.builder()
            .property(property)
            .mediaType(req.mediaType())
            .fileUrl(req.fileUrl())
            .fileName(req.fileName())
            .fileSize(req.fileSize())
            .mimeType(req.mimeType())
            .cover(Boolean.TRUE.equals(req.cover()))
            .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
            .build();

    return PropertyMapper.toMediaResponse(mediaRepo.save(media));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PropertyMediaResponse> listMedia(UUID propertyId) throws CustomException {
    resolveProperty(propertyId);
    return mediaRepo.findByPropertyIdOrderBySortOrderAsc(propertyId).stream()
        .map(PropertyMapper::toMediaResponse)
        .toList();
  }

  @Override
  @Transactional
  public PropertyMediaResponse setCover(UUID propertyId, UUID mediaId, String callerKeycloakId)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(propertyId);
    requireOwnership(property, caller);

    PropertyMedia media =
        mediaRepo
            .findById(mediaId)
            .filter(m -> m.getProperty().getId().equals(propertyId))
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Média introuvable"),
                        ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND));

    mediaRepo.clearCoversForProperty(propertyId);
    media.setCover(true);
    return PropertyMapper.toMediaResponse(mediaRepo.save(media));
  }

  @Override
  @Transactional
  public void deleteMedia(UUID propertyId, UUID mediaId, String callerKeycloakId)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(propertyId);
    requireOwnership(property, caller);

    PropertyMedia media =
        mediaRepo
            .findById(mediaId)
            .filter(m -> m.getProperty().getId().equals(propertyId))
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Média introuvable"),
                        ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND));

    mediaRepo.delete(media);
  }

  @Override
  @Transactional
  public PropertyMediaResponse uploadAndLinkMedia(
      UUID propertyId, String callerKeycloakId, MultipartFile file, String mediaType, boolean cover)
      throws CustomException {

    if (file == null || file.isEmpty()) {
      throw new CustomException(
          new BadRequestException("Le fichier est vide ou absent"),
          ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND);
    }

    String mime = file.getContentType();
    if (mime == null || !ALLOWED_MEDIA_MIMES.contains(mime)) {
      throw new CustomException(
          new BadRequestException("Type MIME non autorisé : " + mime),
          ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND);
    }

    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(propertyId);
    requireOwnership(property, caller);

    String ext =
        switch (mime) {
          case "image/png" -> ".png";
          case "image/webp" -> ".webp";
          case "video/mp4" -> ".mp4";
          case "video/quicktime" -> ".mov";
          default -> ".jpg";
        };

    String objectName = propertyId + "/" + UUID.randomUUID() + ext;
    String fileUrl;
    try {
      fileUrl =
          minioService.uploadFile(
              BUCKET_MEDIA, objectName, file.getInputStream(), file.getSize(), mime);
    } catch (IOException e) {
      throw new CustomException(
          new BadRequestException("Lecture du fichier impossible : " + e.getMessage()),
          ResponseMessageConstants.PROPERTY_MEDIA_NOT_FOUND);
    }

    if (cover) {
      mediaRepo.clearCoversForProperty(propertyId);
    }

    PropertyMedia media =
        PropertyMedia.builder()
            .property(property)
            .mediaType(mediaType != null ? mediaType : "PHOTO")
            .fileUrl(fileUrl)
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .mimeType(mime)
            .cover(cover)
            .sortOrder(0)
            .build();

    return PropertyMapper.toMediaResponse(mediaRepo.save(media));
  }

  // ── Documents ──────────────────────────────────────────────────

  @Override
  @Transactional
  public PropertyDocumentResponse addDocument(
      UUID propertyId, String callerKeycloakId, PropertyDocumentAddRequest req)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(propertyId);
    requireOwnership(property, caller);

    PropertyDocument doc =
        PropertyDocument.builder()
            .property(property)
            .docType(req.docType())
            .title(req.title())
            .fileUrl(req.fileUrl())
            .fileName(req.fileName())
            .fileSize(req.fileSize())
            .mimeType(req.mimeType())
            .visibleToPublic(Boolean.TRUE.equals(req.visibleToPublic()))
            .build();

    return PropertyMapper.toDocumentResponse(docRepo.save(doc));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PropertyDocumentResponse> listDocuments(UUID propertyId, String callerKeycloakId)
      throws CustomException {
    resolveProperty(propertyId);
    User caller = resolveUser(callerKeycloakId);

    // Les documents privés (non visibles au public) ne sont renvoyés qu'au gestionnaire du bien
    Property property = resolveProperty(propertyId);
    boolean isManager;
    try {
      requireOwnership(property, caller);
      isManager = true;
    } catch (CustomException e) {
      isManager = false;
    }

    List<PropertyDocument> docs =
        isManager
            ? docRepo.findByPropertyIdOrderByCreatedAtDesc(propertyId)
            : docRepo.findByPropertyIdAndVisibleToPublicTrue(propertyId);

    return docs.stream().map(PropertyMapper::toDocumentResponse).toList();
  }

  @Override
  @Transactional
  public PropertyDocumentResponse verifyDocument(
      UUID propertyId, UUID docId, String callerKeycloakId, String note) throws CustomException {
    resolveProperty(propertyId);
    User verifier = resolveUser(callerKeycloakId);

    PropertyDocument doc =
        docRepo
            .findById(docId)
            .filter(d -> d.getProperty().getId().equals(propertyId))
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Document introuvable"),
                        ResponseMessageConstants.PROPERTY_DOCUMENT_NOT_FOUND));

    doc.setVerified(true);
    doc.setVerifiedAt(LocalDateTime.now());
    doc.setVerifiedBy(verifier);
    doc.setVerificationNote(note);

    return PropertyMapper.toDocumentResponse(docRepo.save(doc));
  }

  @Override
  @Transactional
  public void deleteDocument(UUID propertyId, UUID docId, String callerKeycloakId)
      throws CustomException {
    User caller = resolveUser(callerKeycloakId);
    Property property = resolveProperty(propertyId);
    requireOwnership(property, caller);

    PropertyDocument doc =
        docRepo
            .findById(docId)
            .filter(d -> d.getProperty().getId().equals(propertyId))
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Document introuvable"),
                        ResponseMessageConstants.PROPERTY_DOCUMENT_NOT_FOUND));

    docRepo.delete(doc);
  }
}
