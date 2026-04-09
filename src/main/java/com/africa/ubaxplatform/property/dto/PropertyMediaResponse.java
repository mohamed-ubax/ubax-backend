package com.africa.ubaxplatform.property.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant un média (photo, vidéo, plan 3D) associé à un bien immobilier.
 *
 * @param id identifiant unique du média
 * @param propertyId identifiant du bien immobilier associé
 * @param mediaType type du média (PHOTO, VIDEO, FLOOR_PLAN, VISIT_360)
 * @param fileUrl URL d'accès au fichier dans MinIO (bucket properties-media)
 * @param fileName nom original du fichier au moment de l'upload
 * @param fileSize taille du fichier en octets
 * @param mimeType type MIME détecté à l'upload (image/webp, video/mp4...)
 * @param cover {@code true} si ce média est la photo de couverture de l'annonce
 * @param sortOrder position d'affichage dans le carrousel (ordre croissant)
 * @param createdAt date de création
 * @param updatedAt date de dernière modification
 */
public record PropertyMediaResponse(
    UUID id,
    UUID propertyId,
    String mediaType,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    boolean cover,
    int sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
