package com.africa.ubaxplatform.property.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête d'ajout d'un document légal à un bien. Le fichier doit être préalablement uploadé dans
 * MinIO (bucket {@code property-documents}), puis l'URL fournie ici.
 */
public record PropertyDocumentAddRequest(
    @NotBlank String docType,
    String title,
    @NotBlank String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    Boolean visibleToPublic) {}
