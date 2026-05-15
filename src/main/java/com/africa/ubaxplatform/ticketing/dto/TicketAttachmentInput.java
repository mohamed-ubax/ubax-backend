package com.africa.ubaxplatform.ticketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketAttachmentInput {

  @NotBlank(message = "L'URL du fichier est obligatoire")
  @Schema(
      description = "URL MinIO du fichier pré-uploadé (bucket ticket-attachments)",
      example = "http://minio:9000/ticket-attachments/mon-fichier.jpg")
  private String fileUrl;

  @Schema(description = "Nom original du fichier", example = "photo_fuite.jpg")
  private String fileName;

  @Schema(description = "Taille en octets", example = "204800")
  private Long fileSize;

  @Schema(description = "Type MIME", example = "image/jpeg")
  private String mimeType;

  @Schema(
      description = "Catégorie (défaut : INCIDENT_PHOTO)",
      allowableValues = {
        "INCIDENT_PHOTO",
        "INCIDENT_VIDEO",
        "INTERVENTION_REPORT",
        "INVOICE",
        "OTHER"
      },
      example = "INCIDENT_PHOTO")
  private String attachmentType;

  @Schema(description = "Légende courte", example = "Vue de la fuite sous le lavabo")
  private String caption;
}
