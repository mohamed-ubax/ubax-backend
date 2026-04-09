package com.africa.ubaxplatform.property.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse représentant un document légal associé à un bien immobilier.
 *
 * @param id identifiant unique du document
 * @param propertyId identifiant du bien immobilier associé
 * @param verifiedById identifiant du modérateur ayant vérifié le document, {@code null} si non
 *     vérifié
 * @param verifiedByName nom complet du modérateur, {@code null} si non vérifié
 * @param docType type du document (TITLE_DEED, CADASTRAL_PLAN, INSURANCE, CONFORMITY_CERTIFICATE,
 *     OTHER)
 * @param title intitulé descriptif du document
 * @param fileUrl URL d'accès au document dans MinIO (bucket property-documents), via URL présignée
 * @param fileName nom original du fichier au moment de l'upload
 * @param fileSize taille du fichier en octets
 * @param mimeType type MIME du document (application/pdf, image/jpeg...)
 * @param visibleToPublic {@code true} si le document est accessible à tous les utilisateurs
 *     connectés
 * @param verified {@code true} si le document a été contrôlé et validé par un modérateur
 * @param verifiedAt date et heure de la vérification
 * @param verificationNote note ou commentaire du modérateur suite à la vérification
 * @param createdAt date de création
 * @param updatedAt date de dernière modification
 */
public record PropertyDocumentResponse(
    UUID id,
    UUID propertyId,
    UUID verifiedById,
    String verifiedByName,
    String docType,
    String title,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    boolean visibleToPublic,
    boolean verified,
    LocalDateTime verifiedAt,
    String verificationNote,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
