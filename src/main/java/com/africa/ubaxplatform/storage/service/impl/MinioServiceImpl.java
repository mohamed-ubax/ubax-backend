package com.africa.ubaxplatform.storage.service.impl;

import com.africa.ubaxplatform.common.exception.StorageException;
import com.africa.ubaxplatform.storage.dto.PresignedUrlResponse;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implémentation du service de stockage objet MinIO.
 *
 * <p>Crée les buckets au démarrage si absents, puis expose les opérations upload / delete / URL.
 */
@Service
@Slf4j
public class MinioServiceImpl implements MinioService {

  private final MinioClient minioClient;
  private static final String BUCKET_PARTNER_DOCS = "partner-documents";
  private static final String[] PARTNER_SUB_DIRS = {"legal", "logo", "contracts"};

  @Value("${minio.endpoint}")
  private String endpoint;

  @Value("${minio.buckets}")
  private List<String> buckets;

  public MinioServiceImpl(MinioClient minioClient) {
    this.minioClient = minioClient;
  }

  /** Crée les buckets déclarés dans application.yml s'ils n'existent pas encore. */
  @PostConstruct
  public void initBuckets() {
    buckets.forEach(
        bucket -> {
          try {
            boolean exists =
                minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
              minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
              log.info("MinIO : bucket '{}' créé", bucket);
            }
          } catch (Exception e) {
            log.warn(
                "MinIO : impossible de vérifier/créer le bucket '{}' : {}", bucket, e.getMessage());
          }
        });
  }

  @Override
  public String uploadFile(
      String bucket, String objectName, InputStream inputStream, long size, String contentType) {
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucket).object(objectName).stream(inputStream, size, -1)
              .contentType(contentType)
              .build());
      return endpoint + "/" + bucket + "/" + objectName;
    } catch (Exception e) {
      throw new StorageException("Erreur lors de l'upload vers MinIO : " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteFile(String bucket, String objectName) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
    } catch (Exception e) {
      log.warn("MinIO : impossible de supprimer {}/{} : {}", bucket, objectName, e.getMessage());
    }
  }

  @Override
  public String getPublicUrl(String bucket, String objectName) {
    return endpoint + "/" + bucket + "/" + objectName;
  }

  @Override
  public String buildPublicUrl(String relativePath) {
    return endpoint + "/" + relativePath;
  }

  @Override
  public PresignedUrlResponse generatePresignedUrl(
      String bucket, String objectName, int expiresInSeconds) {
    try {
      String uploadUrl =
          minioClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.PUT)
                  .bucket(bucket)
                  .object(objectName)
                  .expiry(expiresInSeconds, TimeUnit.SECONDS)
                  .build());

      return PresignedUrlResponse.builder()
          .uploadUrl(uploadUrl)
          .publicUrl(getPublicUrl(bucket, objectName))
          .objectName(objectName)
          .bucket(bucket)
          .expiresInSeconds(expiresInSeconds)
          .build();

    } catch (Exception e) {
      throw new StorageException(
          "Erreur lors de la génération de l'URL présignée : " + e.getMessage(), e);
    }
  }

  // ── Partner documents ───────────────────────────────────────────

  @Override
  public String initPartnerDirectory(String companyName) {
    String slug = slugify(companyName);
    for (String dir : PARTNER_SUB_DIRS) {
      String keepKey = slug + "/" + dir + "/.keep";
      try (InputStream empty = InputStream.nullInputStream()) {
        minioClient.putObject(
            PutObjectArgs.builder().bucket(BUCKET_PARTNER_DOCS).object(keepKey).stream(empty, 0, -1)
                .contentType("application/octet-stream")
                .build());
      } catch (Exception e) {
        log.warn(
            "MinIO : impossible de créer le préfixe {}/{} : {}",
            BUCKET_PARTNER_DOCS,
            keepKey,
            e.getMessage());
      }
    }
    log.info("MinIO : répertoire partenaire initialisé : {}", slug);
    return slug;
  }

  @Override
  public String uploadPartnerLegalDoc(
      String slug, String docKey, InputStream inputStream, long size, String contentType) {
    String objectName = slug + "/legal/" + docKey + "-" + UUID.randomUUID() + extFor(contentType);
    return uploadFile(BUCKET_PARTNER_DOCS, objectName, inputStream, size, contentType);
  }

  @Override
  public String uploadPartnerLogo(
      String slug, InputStream inputStream, long size, String contentType) {
    String objectName = slug + "/logo/logo-" + UUID.randomUUID() + extFor(contentType);
    return uploadFile(BUCKET_PARTNER_DOCS, objectName, inputStream, size, contentType);
  }

  // ── Helpers ────────────────────────────────────────────────────

  private String slugify(String input) {
    if (input == null || input.isBlank()) return "partner-" + UUID.randomUUID();
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized
        .replaceAll("[^\\p{ASCII}]", "")
        .toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
  }

  private String extFor(String contentType) {
    if (contentType == null) return ".bin";
    return switch (contentType) {
      case "application/pdf" -> ".pdf";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }
}
