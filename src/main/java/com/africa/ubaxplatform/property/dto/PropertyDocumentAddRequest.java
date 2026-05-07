package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description =
        "Requête d'ajout d'un document légal à un bien. Le fichier doit être préalablement uploadé dans MinIO via l'URL presignée.")
public record PropertyDocumentAddRequest(
    @Schema(
            description =
                "Type de document — valeurs depuis GET /v1/code-list/type/PROPERTY_DOCUMENT_TYPE :"
                    + " TITLE_DEED | BUILDING_PERMIT | DIAGNOSTIC | CADASTRAL_PLAN |"
                    + " INSURANCE | CONFORMITY_CERTIFICATE | OTHER",
            example = "TITLE_DEED")
        @NotBlank
        String docType,
    @Schema(
            description = "Titre descriptif du document",
            example = "Titre foncier - Villa Sacré-Cœur 3",
            nullable = true)
        String title,
    @Schema(
            description = "URL publique du fichier dans MinIO (retournée par l'endpoint presign)",
            example = "http://localhost:9000/property-documents/uuid/doc-uuid.pdf")
        @NotBlank
        String fileUrl,
    @Schema(description = "Nom original du fichier", example = "titre-foncier.pdf", nullable = true)
        String fileName,
    @Schema(description = "Taille du fichier en octets", example = "204800", nullable = true)
        Long fileSize,
    @Schema(description = "Type MIME du fichier", example = "application/pdf", nullable = true)
        String mimeType,
    @Schema(
            description = "Rendre le document visible aux acheteurs/locataires potentiels",
            example = "false",
            nullable = true)
        Boolean visibleToPublic) {}
