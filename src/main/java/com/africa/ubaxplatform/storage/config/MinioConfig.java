package com.africa.ubaxplatform.storage.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Configuration Spring qui expose le bean {@link MinioClient}. */
@Configuration
public class MinioConfig {

  @Value("${minio.endpoint}")
  private String endpoint;

  @Value("${minio.public-endpoint}")
  private String publicEndpoint;

  @Value("${minio.access-key}")
  private String accessKey;

  @Value("${minio.secret-key}")
  private String secretKey;

  /** Client interne — utilisé pour les opérations directes (upload, delete, init buckets). */
  @Bean
  @Primary
  public MinioClient minioClient() {
    return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
  }

  /**
   * Client public — utilisé exclusivement pour générer les URLs présignées. L'endpoint public
   * garantit que la signature AWS V4 est calculée avec le hostname public, sans remplacement de
   * texte post-signature qui invaliderait la signature.
   */
  @Bean("minioPresignClient")
  public MinioClient minioPresignClient() {
    return MinioClient.builder().endpoint(publicEndpoint).credentials(accessKey, secretKey).build();
  }
}
