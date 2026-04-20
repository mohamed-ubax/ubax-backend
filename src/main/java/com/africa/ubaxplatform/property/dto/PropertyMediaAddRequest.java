package com.africa.ubaxplatform.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête d'ajout d'un média à un bien. Le fichier doit être préalablement uploadé dans MinIO via
 * une presigned URL (bucket {@code properties-media}), puis l'URL résultante est fournie ici.
 */
public record PropertyMediaAddRequest(
    @NotBlank String mediaType,
    @NotBlank String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    @NotNull Boolean cover,
    Integer sortOrder) {}
