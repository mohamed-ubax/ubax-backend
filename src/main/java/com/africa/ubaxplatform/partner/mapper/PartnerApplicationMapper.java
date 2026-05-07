package com.africa.ubaxplatform.partner.mapper;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.ApplicationStatusLogResponse;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import com.africa.ubaxplatform.partner.entity.ApplicationStatusLog;
import com.africa.ubaxplatform.partner.entity.PartnerApplication;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PartnerApplicationMapper {

  private static String minioPublicEndpoint = "";

  @Value("${minio.public-endpoint}")
  public void setMinioPublicEndpoint(String value) {
    PartnerApplicationMapper.minioPublicEndpoint = value;
  }

  private static String resolveUrl(String url) {
    if (url == null) return null;
    if (url.startsWith("http")) {
      // Remplace tout hostname MinIO interne par le public endpoint
      return url.replaceFirst(
          "https?://[^/]+(?=/partner-documents|/agencies-logos|/users-avatars|/properties-media|/property-documents|/tenant-documents|/ticket-attachments|/documents-generated)",
          minioPublicEndpoint);
    }
    String path = url.startsWith("/") ? url.substring(1) : url;
    return minioPublicEndpoint + "/" + path;
  }

  public PartnerApplication toEntity(
      PartnerApplicationRequest request,
      String slug,
      String rccmUrl,
      String dfeUrl,
      String bailUrl,
      String logoUrl,
      LocalDateTime submittedAt) {

    return PartnerApplication.builder()
        .partnerType(request.getPartnerType())
        .companyName(request.getCompanyName())
        .legalRepFirstName(request.getLegalRepFirstName())
        .legalRepLastName(request.getLegalRepLastName())
        .phone(request.getPhone())
        .email(request.getEmail())
        .country(request.getCountry())
        .city(request.getCity())
        .postalAddress(request.getPostalAddress())
        .zone(request.getZone())
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .description(request.getDescription())
        .legalStatus(request.getLegalStatus())
        .registrationNumber(request.getRegistrationNumber())
        .storageSlug(slug)
        .rccmUrl(rccmUrl)
        .dfeUrl(dfeUrl)
        .bailUrl(bailUrl)
        .logoUrl(logoUrl)
        .status(ApplicationStatus.PENDING)
        .submittedAt(submittedAt)
        .build();
  }

  public PartnerApplicationResponse toResponse(
      PartnerApplication app, List<ApplicationStatusLog> logs) {

    PartnerApplicationResponse.PartnerApplicationResponseBuilder builder =
        PartnerApplicationResponse.builder()
            .id(app.getId())
            .partnerType(app.getPartnerType())
            .companyName(app.getCompanyName())
            .legalRepFirstName(app.getLegalRepFirstName())
            .legalRepLastName(app.getLegalRepLastName())
            .phone(app.getPhone())
            .email(app.getEmail())
            .country(app.getCountry())
            .city(app.getCity())
            .postalAddress(app.getPostalAddress())
            .zone(app.getZone())
            .latitude(app.getLatitude())
            .longitude(app.getLongitude())
            .description(app.getDescription())
            .legalStatus(app.getLegalStatus())
            .registrationNumber(app.getRegistrationNumber())
            .rccmUrl(resolveUrl(app.getRccmUrl()))
            .dfeUrl(resolveUrl(app.getDfeUrl()))
            .bailUrl(resolveUrl(app.getBailUrl()))
            .logoUrl(resolveUrl(app.getLogoUrl()))
            .status(app.getStatus())
            .submittedAt(app.getSubmittedAt())
            .reviewedAt(app.getReviewedAt())
            .rejectionReason(app.getRejectionReason())
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt());

    if (app.getReviewedBy() != null) {
      builder.reviewedByName(
          app.getReviewedBy().getFirstName() + " " + app.getReviewedBy().getLastName());
    }

    if (logs != null) {
      builder.statusHistory(logs.stream().map(this::toLogResponse).toList());
    }

    return builder.build();
  }

  public ApplicationStatusLogResponse toLogResponse(ApplicationStatusLog log) {
    return ApplicationStatusLogResponse.builder()
        .id(log.getId())
        .previousStatus(log.getPreviousStatus())
        .newStatus(log.getNewStatus())
        .changedByName(
            log.getChangedBy() != null
                ? log.getChangedBy().getFirstName() + " " + log.getChangedBy().getLastName()
                : "Système")
        .comment(log.getComment())
        .changedAt(log.getChangedAt())
        .build();
  }

  public ApplicationStatusLog toStatusLog(
      PartnerApplication application,
      ApplicationStatus previousStatus,
      ApplicationStatus newStatus,
      User changedBy,
      String comment,
      LocalDateTime changedAt) {
    return ApplicationStatusLog.builder()
        .application(application)
        .previousStatus(previousStatus)
        .newStatus(newStatus)
        .changedBy(changedBy)
        .comment(comment)
        .changedAt(changedAt)
        .build();
  }
}
