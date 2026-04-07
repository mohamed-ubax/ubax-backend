package com.africa.ubaxplatform.storage.service.impl;

import com.africa.ubaxplatform.common.exception.StorageException;
import com.africa.ubaxplatform.storage.service.interfaces.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
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
}
