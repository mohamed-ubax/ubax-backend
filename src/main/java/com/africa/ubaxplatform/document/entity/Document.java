package com.africa.ubaxplatform.document.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.document.codeList.DocumentStatus;
import com.africa.ubaxplatform.document.codeList.DocumentType;
import com.africa.ubaxplatform.document.codeList.RefType;
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

import java.util.UUID;

/**
 * Document généré par la plateforme UBAX (facture, reçu, contrat, rapport...).
 *
 * <p>Cette entité est le registre central de tous les PDF produits par le module
 * {@code document}. Elle ne stocke pas le fichier physique mais uniquement
 * la référence vers MinIO bucket {@code documents-generated}.</p>
 *
 * <p><b>Découplage via refId + refType :</b> plutôt que de multiplier les FK
 * (une vers Payment, une vers Contract, une vers Ticket...), on utilise
 * un pattern de référence générique :
 * <ul>
 *   <li>{@code refId} — UUID de l'entité source.</li>
 *   <li>{@code refType} — type de l'entité source.</li>
 * </ul>
 * Exemple : un reçu de paiement aura {@code refType = PAYMENT} et
 * {@code refId = payment.getId()}. Cela permet au module document de rester
 * indépendant des autres modules sans créer de couplage fort.</p>
 *
 * <p><b>Génération :</b> les PDF sont produits par les classes du package
 * {@code generator} (ex : {@code InvoiceGenerator}, {@code ContractGenerator})
 * à partir des templates du package {@code template} (Thymeleaf / JasperReports).
 * Le fichier est ensuite uploadé dans MinIO et l'URL persistée ici.</p>
 *
 * <p><b>Cycle de vie :</b>
 * <ol>
 *   <li>Un événement métier déclenche la génération (ex : paiement validé).</li>
 *   <li>Le {@code DocumentService} délègue au bon {@code Generator}.</li>
 *   <li>Le PDF est généré en mémoire et uploadé dans MinIO.</li>
 *   <li>Un enregistrement {@code Document} est créé avec l'URL MinIO.</li>
 *   <li>Le {@code NotificationService} envoie le lien par email/SMS.</li>
 * </ol>
 * </p>
 */
@Entity
@Table(
        name = "documents",
        schema = "administrative",
        indexes = {
                @Index(name = "idx_documents_ref",         columnList = "ref_type, ref_id"),
                @Index(name = "idx_documents_doc_type",    columnList = "doc_type"),
                @Index(name = "idx_documents_generated_by",columnList = "generated_by"),
                @Index(name = "idx_documents_status",      columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Document extends BaseEntity {

    /**
     * UUID de l'entité source ayant déclenché la génération de ce document.
     * Utilisé conjointement avec {@code refType} pour retrouver l'objet origine
     * sans FK stricte (découplage inter-modules).
     *
     * <p>Exemples :
     * <ul>
     *   <li>Reçu de loyer → {@code payment.getId()}</li>
     *   <li>Contrat de bail → {@code contract.getId()}</li>
     *   <li>Rapport ticket → {@code ticket.getId()}</li>
     * </ul>
     * </p>
     */
    @Column(name = "ref_id", columnDefinition = "uuid")
    private UUID refId;

    /**
     * Type de l'entité source, utilisé pour résoudre {@code refId}
     * dans le bon repository sans FK stricte.
     * Valeurs : {@code PAYMENT | CONTRACT | TICKET | PROPERTY | OFFER}
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 30)
    private RefType refType;

    /**
     * Catégorie fonctionnelle du document généré.
     *
     * <ul>
     *   <li>{@code INVOICE} — facture émise (loyer, commission, abonnement).</li>
     *   <li>{@code RECEIPT} — reçu de paiement remis au payeur après validation.</li>
     *   <li>{@code CONTRACT} — contrat de location ou de vente généré depuis un template.</li>
     *   <li>{@code LEASE} — bail de location spécifique (sous-type de CONTRACT).</li>
     *   <li>{@code INVENTORY} — état des lieux d'entrée ou de sortie.</li>
     *   <li>{@code REPORT} — rapport comptable, analytique ou d'activité.</li>
     *   <li>{@code STATEMENT} — relevé de compte ou historique de transactions.</li>
     *   <li>{@code OTHER} — tout autre document généré hors catégorie.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private DocumentType docType;

    /**
     * Intitulé du document affiché dans l'interface utilisateur.
     * Exemple : {@code "Facture loyer - Décembre 2025"}
     * Généré automatiquement par le {@code Generator} selon le type.
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * Numéro de référence unique du document, lisible par l'utilisateur.
     * Généré selon le format : {@code DOC-{YEAR}-{TYPE}-{SEQ}}
     * Exemple : {@code DOC-2025-INV-000123}
     * Affiché sur le document PDF et dans l'interface.
     */
    @Column(name = "reference_number", unique = true, length = 100)
    private String referenceNumber;

    /**
     * URL du fichier PDF dans le bucket MinIO {@code documents-generated}.
     * L'accès se fait toujours via une URL présignée GET générée à la demande,
     * jamais exposée directement au client.
     * Format : {@code http://minio:9000/documents-generated/{refType}/{refId}/{uuid}.pdf}
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /**
     * Nom du fichier PDF tel qu'il sera proposé au téléchargement.
     * Exemple : {@code "facture-loyer-decembre-2025.pdf"}
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * Taille du fichier PDF en octets.
     * Affiché dans l'interface pour informer l'utilisateur avant téléchargement.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Statut de génération du document.
     *
     * <ul>
     *   <li>{@code PENDING} — en attente de génération (tâche asynchrone en cours).</li>
     *   <li>{@code GENERATED} — PDF généré et disponible dans MinIO.</li>
     *   <li>{@code SENT} — document envoyé au destinataire par email ou SMS.</li>
     *   <li>{@code FAILED} — erreur lors de la génération (voir {@code errorMessage}).</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    /**
     * Message d'erreur technique en cas d'échec de génération.
     * Utile pour le debugging et les alertes monitoring.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Utilisateur ou service ayant déclenché la génération du document.
     * {@code null} si la génération a été déclenchée automatiquement
     * par un job planifié (ex : facture mensuelle automatique).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "generated_by",
            foreignKey = @ForeignKey(name = "fk_document_generated_by")
    )
    private User generatedBy;

    /**
     * {@code true} si le PDF est disponible et prêt à être téléchargé.
     */
    public boolean isAvailable() {
        return status == DocumentStatus.GENERATED || status == DocumentStatus.SENT;
    }

    /**
     * {@code true} si la génération a échoué et nécessite une re-tentative.
     */
    public boolean hasFailed() {
        return status == DocumentStatus.FAILED;
    }

    /**
     * {@code true} si le document a été généré automatiquement (sans action utilisateur).
     */
    public boolean isAutoGenerated() {
        return generatedBy == null;
    }
}