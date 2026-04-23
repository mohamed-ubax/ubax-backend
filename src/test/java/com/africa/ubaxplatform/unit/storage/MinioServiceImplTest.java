package com.africa.ubaxplatform.unit.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.africa.ubaxplatform.common.exception.StorageException;
import com.africa.ubaxplatform.storage.dto.PresignedUrlResponse;
import com.africa.ubaxplatform.storage.service.impl.MinioServiceImpl;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MinioServiceImpl – tests unitaires")
class MinioServiceImplTest {

  @Mock private MinioClient minioClient;

  private MinioServiceImpl service;

  private static final String ENDPOINT = "http://minio:9000";
  private static final String BUCKET = "test-bucket";
  private static final String OBJECT = "folder/file.pdf";
  private static final String SLUG = "acme-sarl";

  @BeforeEach
  void setUp() {
    service = new MinioServiceImpl(minioClient);
    ReflectionTestUtils.setField(service, "endpoint", ENDPOINT);
    ReflectionTestUtils.setField(service, "buckets", java.util.List.of(BUCKET));
  }

  @Nested
  @DisplayName("initBuckets()")
  class InitBuckets {

    @Test
    @DisplayName("Succès – bucket inexistant → créé via makeBucket")
    void initBuckets_bucketNotExists_createsBucket() throws Exception {
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

      service.initBuckets();

      verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("Succès – bucket déjà existant → aucune création")
    void initBuckets_bucketExists_skipsCreation() throws Exception {
      when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

      service.initBuckets();

      verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("Résilience – erreur MinIO absorbée silencieusement (log warn)")
    void initBuckets_minioThrows_doesNotPropagateException() throws Exception {
      when(minioClient.bucketExists(any(BucketExistsArgs.class)))
          .thenThrow(new RuntimeException("MinIO indisponible"));

      // Ne doit pas lever d'exception — l'erreur est loguée en warn
      service.initBuckets();

      verify(minioClient, never()).makeBucket(any());
    }
  }

  @Nested
  @DisplayName("uploadFile()")
  class UploadFile {

    @Test
    @DisplayName("Succès – retourne l'URL publique construite depuis endpoint/bucket/object")
    void uploadFile_success_returnsPublicUrl() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[10]);

      String url = service.uploadFile(BUCKET, OBJECT, stream, 10, "application/pdf");

      assertThat(url).isEqualTo(ENDPOINT + "/" + BUCKET + "/" + OBJECT);
      verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Échec – exception MinIO → StorageException")
    void uploadFile_minioThrows_throwsStorageException() throws Exception {
      when(minioClient.putObject(any(PutObjectArgs.class)))
          .thenThrow(new RuntimeException("Erreur réseau MinIO"));

      InputStream stream = new ByteArrayInputStream(new byte[10]);

      assertThatThrownBy(() -> service.uploadFile(BUCKET, OBJECT, stream, 10, "application/pdf"))
          .isInstanceOf(StorageException.class)
          .hasMessageContaining("Erreur lors de l'upload vers MinIO");
    }
  }

  @Nested
  @DisplayName("deleteFile()")
  class DeleteFile {

    @Test
    @DisplayName("Succès – removeObject appelé avec les bons paramètres")
    void deleteFile_success_callsRemoveObject() throws Exception {
      service.deleteFile(BUCKET, OBJECT);

      verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("Résilience – exception MinIO absorbée silencieusement (log warn)")
    void deleteFile_minioThrows_doesNotPropagateException() throws Exception {
      doThrow(new RuntimeException("Objet introuvable"))
          .when(minioClient)
          .removeObject(any(RemoveObjectArgs.class));

      // Ne doit pas lever d'exception
      service.deleteFile(BUCKET, OBJECT);
    }
  }

  @Nested
  @DisplayName("getPublicUrl()")
  class GetPublicUrl {

    @Test
    @DisplayName("Retourne la concaténation endpoint/bucket/object")
    void getPublicUrl_returnsExpectedUrl() {
      String url = service.getPublicUrl(BUCKET, OBJECT);

      assertThat(url).isEqualTo(ENDPOINT + "/" + BUCKET + "/" + OBJECT);
    }
  }

  @Nested
  @DisplayName("generatePresignedUrl()")
  class GeneratePresignedUrl {

    @Test
    @DisplayName("Succès – retourne PresignedUrlResponse avec uploadUrl et publicUrl")
    void generatePresignedUrl_success_returnsResponse() throws Exception {
      String presignedUrl = "http://minio:9000/presigned/upload-token-xyz";
      when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
          .thenReturn(presignedUrl);

      PresignedUrlResponse response = service.generatePresignedUrl(BUCKET, OBJECT, 3600);

      assertThat(response.getUploadUrl()).isEqualTo(presignedUrl);
      assertThat(response.getPublicUrl()).isEqualTo(ENDPOINT + "/" + BUCKET + "/" + OBJECT);
      assertThat(response.getObjectName()).isEqualTo(OBJECT);
      assertThat(response.getBucket()).isEqualTo(BUCKET);
      assertThat(response.getExpiresInSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Échec – exception MinIO → StorageException")
    void generatePresignedUrl_minioThrows_throwsStorageException() throws Exception {
      when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
          .thenThrow(new RuntimeException("Erreur présignée"));

      assertThatThrownBy(() -> service.generatePresignedUrl(BUCKET, OBJECT, 3600))
          .isInstanceOf(StorageException.class)
          .hasMessageContaining("Erreur lors de la génération de l'URL présignée");
    }
  }

  @Nested
  @DisplayName("initPartnerDirectory()")
  class InitPartnerDirectory {

    @Test
    @DisplayName("Succès – retourne le slug slugifié du nom de société")
    void initPartnerDirectory_success_returnsSlug() throws Exception {
      String slug = service.initPartnerDirectory("Acme SARL");

      assertThat(slug).isEqualTo("acme-sarl");
      // 3 sous-répertoires (legal, logo, contracts) → 3 appels putObject
      verify(minioClient, times(3)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Succès – nom avec accents slugifié correctement")
    void initPartnerDirectory_withAccents_slugifiesCorrectly() throws Exception {
      String slug = service.initPartnerDirectory("Société Côte d'Ivoire");

      assertThat(slug).isEqualTo("societe-cote-d-ivoire");
    }

    @Test
    @DisplayName("Succès – nom null → slug générique avec UUID")
    void initPartnerDirectory_nullName_returnsGenericSlug() throws Exception {
      String slug = service.initPartnerDirectory(null);

      assertThat(slug).startsWith("partner-");
    }

    @Test
    @DisplayName("Succès – nom vide → slug générique avec UUID")
    void initPartnerDirectory_blankName_returnsGenericSlug() throws Exception {
      String slug = service.initPartnerDirectory("   ");

      assertThat(slug).startsWith("partner-");
    }

    @Test
    @DisplayName("Résilience – erreur MinIO sur .keep absorbée silencieusement")
    void initPartnerDirectory_minioThrows_doesNotPropagateException() throws Exception {
      when(minioClient.putObject(any(PutObjectArgs.class)))
          .thenThrow(new RuntimeException("MinIO indisponible"));

      // Ne doit pas lever d'exception — erreurs loguées en warn
      String slug = service.initPartnerDirectory("Acme SARL");

      assertThat(slug).isEqualTo("acme-sarl");
    }
  }

  @Nested
  @DisplayName("uploadPartnerLegalDoc()")
  class UploadPartnerLegalDoc {

    @Test
    @DisplayName("Succès – URL contient le slug, le docKey et l'extension .pdf")
    void uploadPartnerLegalDoc_pdf_returnsCorrectUrl() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      String url = service.uploadPartnerLegalDoc(SLUG, "rccm", stream, 100, "application/pdf");

      assertThat(url).contains(SLUG + "/legal/rccm");
      assertThat(url).endsWith(".pdf");
      verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Succès – fichier image JPEG → extension .jpg")
    void uploadPartnerLegalDoc_jpeg_returnsJpgExtension() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      String url = service.uploadPartnerLegalDoc(SLUG, "dfe", stream, 100, "image/jpeg");

      assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("Succès – fichier PNG → extension .png")
    void uploadPartnerLegalDoc_png_returnsPngExtension() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      String url = service.uploadPartnerLegalDoc(SLUG, "bail", stream, 100, "image/png");

      assertThat(url).endsWith(".png");
    }

    @Test
    @DisplayName("Échec – erreur MinIO → StorageException")
    void uploadPartnerLegalDoc_minioThrows_throwsStorageException() throws Exception {
      when(minioClient.putObject(any(PutObjectArgs.class)))
          .thenThrow(new RuntimeException("Erreur upload"));

      assertThatThrownBy(
              () ->
                  service.uploadPartnerLegalDoc(
                      SLUG, "rccm", new ByteArrayInputStream(new byte[10]), 10, "application/pdf"))
          .isInstanceOf(StorageException.class);
    }
  }

  @Nested
  @DisplayName("uploadPartnerLogo()")
  class UploadPartnerLogo {

    @Test
    @DisplayName("Succès – URL contient le slug et logo, extension .png")
    void uploadPartnerLogo_png_returnsCorrectUrl() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      String url = service.uploadPartnerLogo(SLUG, stream, 100, "image/png");

      assertThat(url).contains(SLUG + "/logo/logo-");
      assertThat(url).endsWith(".png");
      verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Succès – logo WebP → extension .webp")
    void uploadPartnerLogo_webp_returnsWebpExtension() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      String url = service.uploadPartnerLogo(SLUG, stream, 100, "image/webp");

      assertThat(url).endsWith(".webp");
    }

    @Test
    @DisplayName("Succès – logo JPEG → extension .jpg (cas default du switch)")
    void uploadPartnerLogo_jpeg_returnsJpgExtension() throws Exception {
      InputStream stream = new ByteArrayInputStream(new byte[100]);

      String url = service.uploadPartnerLogo(SLUG, stream, 100, "image/jpeg");

      assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("Échec – erreur MinIO → StorageException")
    void uploadPartnerLogo_minioThrows_throwsStorageException() throws Exception {
      when(minioClient.putObject(any(PutObjectArgs.class)))
          .thenThrow(new RuntimeException("Erreur upload logo"));

      assertThatThrownBy(
              () ->
                  service.uploadPartnerLogo(
                      SLUG, new ByteArrayInputStream(new byte[10]), 10, "image/png"))
          .isInstanceOf(StorageException.class);
    }
  }
}
