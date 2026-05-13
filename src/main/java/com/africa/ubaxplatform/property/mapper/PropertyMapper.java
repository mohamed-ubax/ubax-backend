package com.africa.ubaxplatform.property.mapper;

import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.Hotel;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.property.dto.PropertyAmenityResponse;
import com.africa.ubaxplatform.property.dto.PropertyDocumentResponse;
import com.africa.ubaxplatform.property.dto.PropertyMediaResponse;
import com.africa.ubaxplatform.property.dto.PropertyResponse;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.entity.PropertyAmenity;
import com.africa.ubaxplatform.property.entity.PropertyDocument;
import com.africa.ubaxplatform.property.entity.PropertyMedia;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper statique entre les entités du module {@code property} et leurs DTOs de sortie.
 *
 * <p>Centralise toutes les conversions pour éviter la duplication dans les services.
 *
 * <p>Usage :
 *
 * <pre>{@code
 * PropertyResponse         response = PropertyMapper.toResponse(property);
 * PropertyMediaResponse    media    = PropertyMapper.toMediaResponse(propertyMedia);
 * PropertyDocumentResponse doc      = PropertyMapper.toDocumentResponse(propertyDocument);
 * }</pre>
 */
@Component
public class PropertyMapper {

  private PropertyMapper() {}

  /**
   * Convertit un {@link Property} en {@link PropertyResponse}.
   *
   * <p>Les informations du propriétaire et de l'agence sont extraites des relations lazy associées.
   *
   * @param p entité à convertir (non null)
   * @return DTO de réponse complet
   */
  public static PropertyResponse toResponse(Property p) {
    return toResponse(p, null);
  }

  public static PropertyResponse toResponse(Property p, String coverPhotoUrl) {
    User owner = p.getOwner();
    Agency agency = p.getAgency();
    Hotel hotel = p.getHotel();
    List<PropertyAmenityResponse> amenities =
        p.getAmenities().stream().map(PropertyMapper::toAmenityResponse).toList();
    return new PropertyResponse(
        p.getId(),
        owner != null ? owner.getId() : null,
        owner != null ? owner.getFullName() : null,
        agency != null ? agency.getId() : null,
        agency != null ? agency.getName() : null,
        hotel != null ? hotel.getId() : null,
        hotel != null ? hotel.getName() : null,
        p.getTitle(),
        p.getDescription(),
        p.getPropertyType(),
        p.getTransactionType(),
        p.getPrice(),
        p.getCondition(),
        p.getYearBuilt(),
        p.getSurfaceTotal(),
        p.getSurfaceLiving(),
        p.getRooms(),
        p.getBedrooms(),
        p.getBathrooms(),
        p.getBalconies(),
        p.getFloor(),
        p.getTotalFloors(),
        p.getAddress(),
        p.getCity(),
        p.getDistrict(),
        p.getStreet(),
        p.getLatitude(),
        p.getLongitude(),
        amenities,
        p.isBoosted(),
        p.getBoostExpiresAt(),
        p.getStatus(),
        p.getRejectionReason(),
        p.getPublishedAt(),
        p.getBedType(),
        p.getMaxOccupancy(),
        p.getMealPlan(),
        p.getPaymentFrequency(),
        coverPhotoUrl,
        p.getCreatedAt(),
        p.getUpdatedAt());
  }

  public static PropertyAmenityResponse toAmenityResponse(PropertyAmenity a) {
    return new PropertyAmenityResponse(
        a.getId(), a.getCode(), a.getCustomValue(), a.getCustomDescription());
  }

  /**
   * Convertit un {@link PropertyMedia} en {@link PropertyMediaResponse}.
   *
   * @param m entité à convertir (non null)
   * @return DTO de réponse du média
   */
  public static PropertyMediaResponse toMediaResponse(PropertyMedia m) {
    return new PropertyMediaResponse(
        m.getId(),
        m.getProperty() != null ? m.getProperty().getId() : null,
        m.getMediaType(),
        m.getFileUrl(),
        m.getFileName(),
        m.getFileSize(),
        m.getMimeType(),
        m.isCover(),
        m.getSortOrder(),
        m.getCreatedAt(),
        m.getUpdatedAt());
  }

  /**
   * Convertit un {@link PropertyDocument} en {@link PropertyDocumentResponse}.
   *
   * <p>Les informations du modérateur ayant vérifié le document sont extraites si disponibles.
   *
   * @param d entité à convertir (non null)
   * @return DTO de réponse du document
   */
  public static PropertyDocumentResponse toDocumentResponse(PropertyDocument d) {
    User verifiedBy = d.getVerifiedBy();
    return new PropertyDocumentResponse(
        d.getId(),
        d.getProperty() != null ? d.getProperty().getId() : null,
        verifiedBy != null ? verifiedBy.getId() : null,
        verifiedBy != null ? verifiedBy.getFullName() : null,
        d.getDocType(),
        d.getTitle(),
        d.getFileUrl(),
        d.getFileName(),
        d.getFileSize(),
        d.getMimeType(),
        d.isVisibleToPublic(),
        d.isVerified(),
        d.getVerifiedAt(),
        d.getVerificationNote(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
