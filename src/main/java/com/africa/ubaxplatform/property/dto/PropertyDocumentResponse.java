package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Document légal associé à un bien immobilier")
public record PropertyDocumentResponse(
    @Schema(description = "Identifiant unique du document") UUID id,
    @Schema(description = "Identifiant du bien associé") UUID propertyId,
    @Schema(description = "Identifiant du modérateur ayant vérifié le document", nullable = true)
        UUID verifiedById,
    @Schema(description = "Nom du modérateur", nullable = true) String verifiedByName,
    @Schema(
            description =
                "Type de document : TITRE_FONCIER | PERMIS_CONSTRUIRE | DIAGNOSTIC |"
                    + " CONTRAT_BAIL | AUTRE",
            example = "TITRE_FONCIER")
        String docType,
    @Schema(description = "Titre descriptif du document", nullable = true) String title,
    @Schema(
            description = "URL publique du fichier dans MinIO",
            example = "http://localhost:9000/property-documents/uuid/doc-uuid.pdf")
        String fileUrl,
    @Schema(description = "Nom original du fichier", example = "titre-foncier.pdf", nullable = true)
        String fileName,
    @Schema(description = "Taille du fichier en octets", nullable = true) Long fileSize,
    @Schema(description = "Type MIME", example = "application/pdf", nullable = true)
        String mimeType,
    @Schema(description = "Document visible aux acheteurs/locataires potentiels")
        boolean visibleToPublic,
    @Schema(description = "Document vérifié par un modérateur") boolean verified,
    @Schema(description = "Date de vérification", nullable = true) LocalDateTime verifiedAt,
    @Schema(description = "Note du modérateur", nullable = true) String verificationNote,
    @Schema(description = "Date de création") LocalDateTime createdAt,
    @Schema(description = "Date de dernière modification") LocalDateTime updatedAt) {}
