package com.africa.ubaxplatform.storage.dto;

import lombok.Builder;
import lombok.Getter;

/** Réponse retournée lors de la génération d'une URL présignée PUT MinIO. */
@Getter
@Builder
public class PresignedUrlResponse {

  /** URL présignée PUT à utiliser pour uploader le fichier directement vers MinIO. */
  private String uploadUrl;

  /** URL publique finale de l'objet (accessible après upload). */
  private String publicUrl;

  /** Nom de l'objet dans le bucket (à renvoyer au backend après upload). */
  private String objectName;

  /** Bucket cible. */
  private String bucket;

  /** Durée de validité de l'URL en secondes. */
  private int expiresInSeconds;
}
