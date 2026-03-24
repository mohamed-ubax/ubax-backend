package com.africa.ubaxplatform.property.entity;

import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.property.codeList.MediaType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Média associé à un bien immobilier : photo, vidéo, plan 2D/3D ou visite virtuelle.
 *
 * <p><b>Stockage :</b> fichiers hébergés dans le bucket MinIO {@code properties-media}.
 * Seule l'URL est persistée. Les vidéos lourdes peuvent pointer vers un CDN externe.</p>
 *
 * <p><b>Cycle d'upload :</b>
 * <ol>
 *   <li>Frontend demande URL présignée PUT :
 *       {@code GET /api/storage/presign?bucket=properties-media&key={propertyId}/{uuid}.webp}</li>
 *   <li>Upload direct vers MinIO via l'URL présignée.</li>
 *   <li>Frontend notifie le backend avec l'URL publique finale.</li>
 *   <li>Backend crée l'enregistrement {@code PropertyMedia}.</li>
 * </ol>
 * </p>
 *
 * <p><b>Photo de couverture :</b> une seule entrée par bien peut avoir
 * {@code cover = true}. C'est cette photo qui s'affiche dans les résultats
 * de recherche et le carrousel en premier.</p>
 */
@Entity
@Table(
        name = "property_media",
        schema = "administrative",
        indexes = {
                @Index(name = "idx_property_media_property",   columnList = "property_id"),
                @Index(name = "idx_property_media_type",       columnList = "media_type"),
                @Index(name = "idx_property_media_cover",      columnList = "property_id, is_cover"),
                @Index(name = "idx_property_media_sort_order", columnList = "property_id, sort_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PropertyMedia extends BaseEntity {

    /**
     * Bien immobilier auquel ce média est rattaché.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "property_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_media_property")
    )
    private Property property;

    /**
     * Nature du média uploadé.
     * Valeurs : {@code PHOTO | VIDEO | PLAN_2D | PLAN_3D | VISIT_360}
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    /**
     * URL d'accès au fichier dans MinIO bucket {@code properties-media}.
     * Pour les visites 360°, peut être un lien externe (Matterport, etc.).
     * Format : {@code http://minio:9000/properties-media/{propertyId}/{uuid}.webp}
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
     * Type MIME du fichier, détecté au moment de l'upload.
     * Exemples : {@code image/webp}, {@code video/mp4}, {@code application/pdf}
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * {@code true} si ce média est la photo de couverture de l'annonce.
     * Une seule entrée par bien devrait avoir {@code cover = true}.
     */
    @Column(name = "is_cover", nullable = false)
    @Builder.Default
    private boolean cover = false;

    /**
     * Position d'affichage dans le carrousel (ordre croissant).
     * Modifiable via drag-and-drop dans l'interface de gestion des médias.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /** {@code true} si ce média est une photo affichable dans le carrousel. */
    public boolean isPhoto() {
        return mediaType == MediaType.PHOTO;
    }

    /** {@code true} si ce média est une visite virtuelle 360°. */
    public boolean isVirtualVisit() {
        return mediaType == MediaType.VISIT_360;
    }
}