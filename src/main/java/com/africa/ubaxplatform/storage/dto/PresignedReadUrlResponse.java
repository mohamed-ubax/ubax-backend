package com.africa.ubaxplatform.storage.dto;

import lombok.Builder;
import lombok.Getter;

/** Réponse retournée lors de la génération d'une URL présignée GET MinIO (lecture temporaire). */
@Getter
@Builder
public class PresignedReadUrlResponse {

  /** URL présignée GET à usage unique — valide pour {@code expiresInSeconds} secondes. */
  private String readUrl;

  /** Nom de l'objet dans le bucket. */
  private String objectName;

  /** Bucket source. */
  private String bucket;

  /** Durée de validité en secondes. */
  private int expiresInSeconds;
}
