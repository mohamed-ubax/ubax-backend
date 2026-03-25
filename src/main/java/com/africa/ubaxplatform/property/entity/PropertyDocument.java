package com.africa.ubaxplatform.property.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.property.codeList.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Document légal attaché à un bien immobilier.
 *
 * <p>Regroupe tous les justificatifs obligatoires ou complémentaires associés à un bien : titre
 * foncier, permis de construire, diagnostics, plan cadastral, assurances, certificats de
 * conformité, etc.
 *
 * <p><b>Stockage :</b> les fichiers sont hébergés dans le bucket MinIO {@code property-documents}
 * (bucket privé, accès restreint). Ces documents ne sont <b>jamais publics</b> : l'accès est
 * conditionné au statut de l'acheteur (visite confirmée, offre acceptée, ou validation admin).
 *
 * <p><b>Vérification :</b> un modérateur peut marquer un document comme vérifié ({@code verified =
 * true}), alimentant le badge "Documents vérifiés" sur la fiche bien.
 *
 * <p><b>Cycle d'upload :</b>
 *
 * <ol>
 *   <li>Demande URL présignée PUT : {@code GET
 *       /api/storage/presign?bucket=property-documents&key={propertyId}/{uuid}.pdf}
 *   <li>Upload direct vers MinIO via l'URL présignée.
 *   <li>Notification backend : {@code POST /api/properties/{id}/documents}
 *   <li>Backend crée l'enregistrement {@code PropertyDocument}.
 * </ol>
 */
@Entity
@Table(
    name = "property_documents",
    schema = "administrative",
    indexes = {
      @Index(name = "idx_prop_doc_property", columnList = "property_id"),
      @Index(name = "idx_prop_doc_type", columnList = "doc_type"),
      @Index(name = "idx_prop_doc_verified", columnList = "is_verified")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PropertyDocument extends BaseEntity {

  /** Bien immobilier auquel ce document est rattaché. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "property_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_prop_doc_property"))
  private Property property;

  /**
   * Administrateur ou modérateur ayant vérifié le document. {@code null} si le document n'a pas
   * encore été contrôlé.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by", foreignKey = @ForeignKey(name = "fk_prop_doc_verified_by"))
  private User verifiedBy;

  /**
   * Catégorie du document légal. Valeurs : {@code TITLE_DEED | BUILDING_PERMIT | DIAGNOSTIC |
   * CADASTRAL_PLAN | INSURANCE | CONFORMITY_CERTIFICATE | OTHER}
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "doc_type", nullable = false, length = 50)
  private DocumentType docType;

  /** Intitulé descriptif du document. Exemple : {@code "Titre foncier N°1234 - Dakar Plateau"} */
  @Column(name = "title", length = 255)
  private String title;

  /**
   * URL d'accès au document dans MinIO bucket {@code property-documents}. Toujours via URL
   * présignée GET générée à la demande, jamais exposée directement. Format : {@code
   * http://minio:9000/property-documents/{propertyId}/{uuid}.pdf}
   */
  @Column(name = "file_url", nullable = false, length = 500)
  private String fileUrl;

  /** Nom original du fichier au moment de l'upload. */
  @Column(name = "file_name", length = 255)
  private String fileName;

  /** Taille du fichier en octets. */
  @Column(name = "file_size")
  private Long fileSize;

  /**
   * Type MIME du document. Valeurs attendues : {@code application/pdf}, {@code image/jpeg}, {@code
   * image/png}
   */
  @Column(name = "mime_type", length = 100)
  private String mimeType;

  /**
   * {@code true} si le document est accessible à tous les utilisateurs connectés. {@code false} =
   * accès restreint aux acheteurs ayant déposé une offre. Défaut : {@code false} (privé par
   * défaut).
   */
  @Column(name = "is_visible_to_public", nullable = false)
  @Builder.Default
  private boolean visibleToPublic = false;

  /**
   * {@code true} si le document a été contrôlé et validé par un modérateur. Active le badge
   * "Documents vérifiés" sur la fiche annonce.
   */
  @Column(name = "is_verified", nullable = false)
  @Builder.Default
  private boolean verified = false;

  /** Date et heure de la vérification par le modérateur. */
  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  /**
   * Note ou commentaire du modérateur suite à la vérification. Exemple : {@code "Document
   * authentique, titre foncier conforme."}
   */
  @Column(name = "verification_note", columnDefinition = "TEXT")
  private String verificationNote;

  /**
   * {@code true} si le document est un titre foncier. Condition vérifiée avant de passer un bien en
   * statut {@code PUBLISHED}.
   */
  public boolean isTitleDeed() {
    return docType == DocumentType.TITLE_DEED;
  }

  /** {@code true} si le document est prêt à être partagé avec un acheteur (vérifié + visible). */
  public boolean isReadyToShare() {
    return verified && visibleToPublic;
  }
}
