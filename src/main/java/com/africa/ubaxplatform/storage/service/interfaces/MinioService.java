package com.africa.ubaxplatform.storage.service.interfaces;

import com.africa.ubaxplatform.common.exception.StorageException;
import com.africa.ubaxplatform.storage.dto.PresignedUrlResponse;
import java.io.InputStream;

/**
 * Contrat du service de stockage objet MinIO.
 *
 * <p>Expose les opérations de base sur les objets stockés dans les buckets MinIO de la plateforme
 * UBAX. Les buckets déclarés dans {@code application.yml} (propriété {@code minio.buckets}) sont
 * créés automatiquement au démarrage de l'application si absents.
 *
 * <p>Buckets disponibles :
 *
 * <ul>
 *   <li>{@code users-avatars} – photos de profil des utilisateurs
 *   <li>{@code agencies-logos} – logos des agences immobilières
 *   <li>{@code properties-media} – photos et vidéos de biens immobiliers
 *   <li>{@code property-documents} – documents attachés aux biens (diagnostics, DPE…)
 *   <li>{@code tenant-documents} – pièces justificatives des locataires
 *   <li>{@code documents-generated} – contrats et factures générés par la plateforme
 *   <li>{@code ticket-attachments} – pièces jointes des tickets de support
 *   <li>{@code partner-documents} – documents légaux et logos des partenaires, organisés par slug
 * </ul>
 */
public interface MinioService {
  /**
   * Upload un fichier dans un bucket MinIO et retourne son URL directe.
   *
   * <p>Si un objet portant le même {@code objectName} existe déjà dans le bucket, il est
   * <b>remplacé</b> silencieusement (comportement natif MinIO).
   *
   * @param bucket nom du bucket cible (doit exister ou avoir été créé au démarrage)
   * @param objectName chemin et nom de l'objet dans le bucket (ex : {@code keycloakId.jpg})
   * @param inputStream flux binaire du fichier à uploader
   * @param size taille en octets du fichier ({@code -1} si inconnue — MinIO utilisera le mode
   *     chunked automatiquement)
   * @param contentType type MIME du fichier (ex : {@code image/jpeg}, {@code application/pdf})
   * @return URL d'accès direct à l'objet : {@code {endpoint}/{bucket}/{objectName}}
   * @throws StorageException si l'upload échoue (erreur réseau, bucket inexistant, etc.)
   */
  String uploadFile(
      String bucket, String objectName, InputStream inputStream, long size, String contentType);

  /**
   * Supprime un objet d'un bucket MinIO.
   *
   * <p>Les erreurs sont loguées sans être propagées afin de ne pas bloquer les opérations métier en
   * cas de fichier déjà absent ou de problème réseau transitoire.
   *
   * @param bucket nom du bucket contenant l'objet
   * @param objectName nom de l'objet à supprimer
   */
  void deleteFile(String bucket, String objectName);

  /**
   * Retourne l'URL d'accès direct à un objet MinIO sans effectuer d'appel réseau.
   *
   * <p>L'URL est construite à partir de l'endpoint configuré : {@code
   * {endpoint}/{bucket}/{objectName}}. Pour que l'URL soit accessible publiquement, le bucket doit
   * être configuré en lecture publique.
   *
   * @param bucket nom du bucket
   * @param objectName nom de l'objet
   * @return URL directe de l'objet
   */
  String getPublicUrl(String bucket, String objectName);

  // ── Partner documents ──────────────────────────────────────────────

  /**
   * Génère une URL présignée PUT permettant au frontend d'uploader un fichier directement vers
   * MinIO, sans transiter par le backend.
   *
   * <p>Le frontend utilise cette URL pour un {@code PUT} HTTP direct vers MinIO. Après l'upload, il
   * notifie le backend avec l'{@code objectName} retourné.
   *
   * @param bucket nom du bucket cible (doit être dans la liste des buckets autorisés)
   * @param objectName chemin et nom de l'objet dans le bucket (ex : {@code propertyId/uuid.webp})
   * @param expiresInSeconds durée de validité de l'URL en secondes (max 604800 = 7 jours)
   * @return {@link PresignedUrlResponse} contenant l'URL d'upload, l'URL publique finale et les
   *     métadonnées
   * @throws StorageException si la génération échoue
   */
  PresignedUrlResponse generatePresignedUrl(String bucket, String objectName, int expiresInSeconds);

  // ── Partner documents ──────────────────────────────────────────────

  /**
   * Crée la structure de répertoires pour un partenaire dans le bucket {@code partner-documents}.
   *
   * <p>Préfixes créés (via un fichier sentinelle {@code .keep}) :
   *
   * <ul>
   *   <li>{@code {slug}/legal/} – RCCM, DFE, bail
   *   <li>{@code {slug}/logo/} – logo de l'entreprise
   *   <li>{@code {slug}/contracts/} – contrats signés (usage futur)
   * </ul>
   *
   * @param companyName raison sociale du partenaire (sera slugifiée)
   * @return le slug généré (ex : {@code hotel-la-teranga})
   */
  String initPartnerDirectory(String companyName);

  /**
   * Upload un document légal dans {@code partner-documents/{slug}/legal/}.
   *
   * @param slug répertoire du partenaire (retourné par {@link #initPartnerDirectory})
   * @param docKey clé du document (ex : {@code rccm}, {@code dfe}, {@code bail})
   * @param inputStream flux binaire du fichier
   * @param size taille en octets
   * @param contentType type MIME
   * @return URL directe du fichier uploadé
   */
  String uploadPartnerLegalDoc(
      String slug, String docKey, InputStream inputStream, long size, String contentType);

  /**
   * Upload le logo d'un partenaire dans {@code partner-documents/{slug}/logo/}.
   *
   * @param slug répertoire du partenaire
   * @param inputStream flux de l'image
   * @param size taille en octets
   * @param contentType type MIME
   * @return URL directe du logo uploadé
   */
  String uploadPartnerLogo(String slug, InputStream inputStream, long size, String contentType);
}
