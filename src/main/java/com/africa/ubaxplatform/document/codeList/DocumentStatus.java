package com.africa.ubaxplatform.document.codeList;

/**
 * Statut de génération d'un document PDF produit par la plateforme UBAX.
 *
 * <p>La génération des documents est traitée de façon asynchrone : un enregistrement
 * {@code Document} est créé immédiatement en statut {@code PENDING}, puis mis à jour par le
 * générateur une fois le PDF produit et uploadé dans MinIO.
 *
 * <pre>
 *  PENDING ──► GENERATED ──► SENT
 *     │
 *     └──► FAILED  (erreur de génération ou d'upload MinIO)
 * </pre>
 *
 * @see com.africa.ubaxplatform.document.entity.Document
 */
public enum DocumentStatus {

  /**
   * En attente de génération.
   *
   * <p>L'enregistrement {@code Document} a été créé en base mais le PDF n'est pas encore produit.
   * La tâche de génération est en file d'attente ou en cours d'exécution. Aucune URL MinIO n'est
   * disponible à ce stade.
   */
  PENDING,

  /**
   * PDF généré et disponible dans MinIO.
   *
   * <p>Le fichier a été produit avec succès et uploadé dans le bucket {@code documents-generated}.
   * L'URL MinIO est renseignée dans {@code Document.fileUrl}. Le document peut être téléchargé
   * via une URL présignée GET générée à la demande.
   */
  GENERATED,

  /**
   * Document envoyé au destinataire.
   *
   * <p>Le lien de téléchargement a été transmis au destinataire (locataire, agence, propriétaire)
   * par email ou SMS via le module {@code notification}. C'est l'état terminal nominal d'un
   * document produit par un événement métier automatique (ex : validation de paiement).
   */
  SENT,

  /**
   * Échec de la génération ou de l'upload.
   *
   * <p>Une erreur technique est survenue lors de la production du PDF (template introuvable,
   * données manquantes) ou lors de l'upload dans MinIO (timeout, bucket inaccessible). Le message
   * d'erreur est stocké dans {@code Document.errorMessage} pour faciliter le débogage. Une
   * re-tentative manuelle ou automatique peut faire repasser le document en {@code PENDING}.
   */
  FAILED
}
