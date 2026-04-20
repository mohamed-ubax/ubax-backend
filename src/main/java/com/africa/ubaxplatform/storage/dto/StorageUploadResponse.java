package com.africa.ubaxplatform.storage.dto;

/** Résultat d'un upload direct (multipart) vers MinIO. */
public record StorageUploadResponse(
    /** URL publique finale de l'objet stocké dans MinIO. */
    String fileUrl,
    /** Chemin relatif dans le bucket (ex : {@code properties-media/uuid.jpg}). */
    String objectName,
    /** Bucket cible. */
    String bucket,
    /** Nom original du fichier uploadé. */
    String fileName,
    /** Taille en octets. */
    Long fileSize,
    /** Type MIME détecté. */
    String mimeType) {}
