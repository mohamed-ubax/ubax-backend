package com.africa.ubaxplatform.ticketing.entity;

import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.base.BaseEntity;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.ticketing.codeList.CostImputedTo;
import com.africa.ubaxplatform.ticketing.codeList.TicketCategory;
import com.africa.ubaxplatform.ticketing.codeList.TicketPriority;
import com.africa.ubaxplatform.ticketing.codeList.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ticket de maintenance ou SAV déclaré par un locataire dans le cadre d'un contrat actif.
 *
 * <p>Le cycle de vie d'un ticket suit le workflow suivant :
 * {@code OPEN → IN_ANALYSIS → TECHNICIAN_SENT → RESOLVED → CLOSED}
 * À chaque transition, une notification est envoyée au locataire
 * via le module {@code notification}.</p>
 *
 * <p><b>Pièces jointes :</b> le locataire peut joindre des photos ou vidéos
 * de l'incident lors de la déclaration. Ces fichiers sont stockés dans
 * MinIO bucket {@code ticket-attachments} et référencés dans
 * {@link TicketAttachment} (entité dédiée pour gérer plusieurs fichiers).</p>
 *
 * <p><b>Messagerie interne :</b> les échanges entre le locataire, l'agent SAV
 * et le prestataire sont gérés dans {@link TicketMessage}.</p>
 *
 * <p><b>Coût de réparation :</b> l'agent SAV enregistre le coût après
 * résolution pour alimenter le tableau de bord comptable et décider
 * si la charge est imputable au locataire ou au propriétaire.</p>
 */
@Entity
@Table(
        name = "tickets",
        schema = "administrative",
        indexes = {
                @Index(name = "idx_ticket_contract",    columnList = "contract_id"),
                @Index(name = "idx_ticket_reporter",    columnList = "reporter_id"),
                @Index(name = "idx_ticket_assigned_to", columnList = "assigned_to"),
                @Index(name = "idx_ticket_status",      columnList = "status"),
                @Index(name = "idx_ticket_priority",    columnList = "priority"),
                @Index(name = "idx_ticket_category",    columnList = "category")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Ticket extends BaseEntity {

    /**
     * Contrat de location dans le cadre duquel l'incident est signalé.
     * Permet de retrouver automatiquement le bien concerné et le propriétaire
     * à notifier sans jointure supplémentaire.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "contract_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_contract")
    )
    private Contract contract;

    /**
     * Locataire ayant déclaré l'incident.
     * Toujours notifié lors de chaque changement de statut du ticket.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reporter_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ticket_reporter")
    )
    private User reporter;

    /**
     * Agent SAV ou prestataire technique chargé de traiter le ticket.
     * {@code null} tant que le ticket n'a pas été assigné.
     * L'assignation déclenche une notification à l'agent et au locataire.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_to",
            foreignKey = @ForeignKey(name = "fk_ticket_assigned_to")
    )
    private User assignedTo;

    /**
     * Catégorie principale de l'incident signalé.
     * Utilisée pour router le ticket vers le bon prestataire
     * et alimenter les statistiques de maintenance.
     *
     * <ul>
     *   <li>{@code LEAK} — fuite d'eau (robinet, tuyau, toiture...).</li>
     *   <li>{@code ELECTRICAL} — panne électrique (prise, disjoncteur, éclairage...).</li>
     *   <li>{@code LOCK} — problème de serrure, clé, accès.</li>
     *   <li>{@code PLUMBING} — problème de plomberie hors fuite (WC, évier...).</li>
     *   <li>{@code APPLIANCE} — panne d'équipement fourni (clim, chauffe-eau, four...).</li>
     *   <li>{@code STRUCTURE} — problème structurel (fissure, humidité, toiture...).</li>
     *   <li>{@code PEST} — nuisibles (insectes, rongeurs...).</li>
     *   <li>{@code COMMON_AREA} — problème dans les parties communes.</li>
     *   <li>{@code OTHER} — tout autre incident non catégorisé.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private TicketCategory category;

    /**
     * Titre court de l'incident, saisi par le locataire.
     * Affiché dans la liste des tickets du tableau de bord SAV.
     * Exemple : {@code "Fuite sous l'évier de la cuisine"}
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Description détaillée de l'incident par le locataire.
     * Contient le contexte, les circonstances et l'impact sur la vie quotidienne.
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Niveau d'urgence estimé par le locataire lors de la déclaration.
     * L'agent SAV peut modifier la priorité après analyse.
     *
     * <ul>
     *   <li>{@code LOW} — incident mineur, sans impact immédiat.</li>
     *   <li>{@code NORMAL} — incident gênant, à traiter dans les délais normaux.</li>
     *   <li>{@code HIGH} — incident impactant le confort, traitement prioritaire.</li>
     *   <li>{@code URGENT} — incident bloquant ou dangereux (fuite majeure, panne électrique).</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.NORMAL;

    /**
     * Statut du ticket dans son cycle de résolution.
     *
     * <ul>
     *   <li>{@code OPEN} — déclaré par le locataire, pas encore pris en charge.</li>
     *   <li>{@code IN_ANALYSIS} — l'agent SAV analyse le problème.</li>
     *   <li>{@code TECHNICIAN_SENT} — un prestataire a été mandaté et envoyé.</li>
     *   <li>{@code RESOLVED} — incident résolu, en attente de confirmation du locataire.</li>
     *   <li>{@code CLOSED} — clôturé après confirmation du locataire ou délai dépassé.</li>
     *   <li>{@code CANCELLED} — annulé (fausse alerte ou problème résolu seul).</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    /**
     * Nom du prestataire ou technicien externe mandaté pour l'intervention.
     * Renseigné par l'agent SAV lors du passage au statut {@code TECHNICIAN_SENT}.
     */
    @Column(name = "technician_name", length = 200)
    private String technicianName;

    /**
     * Numéro de téléphone du prestataire pour contact direct par le locataire.
     */
    @Column(name = "technician_phone", length = 20)
    private String technicianPhone;

    /**
     * Date et heure planifiées pour l'intervention du prestataire.
     * Communiquée au locataire par notification SMS/email.
     */
    @Column(name = "intervention_scheduled_at")
    private LocalDateTime interventionScheduledAt;

    /**
     * Date et heure de résolution effective de l'incident.
     * Valorisée lors du passage au statut {@code RESOLVED}.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Date et heure de clôture définitive du ticket.
     * Valorisée lors du passage au statut {@code CLOSED}.
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Coût total de la réparation enregistré par l'agent SAV après résolution (XOF).
     * Alimenté le tableau de bord comptable et décide de l'imputation
     * (propriétaire ou locataire selon le contrat).
     */
    @Column(name = "repair_cost", precision = 12, scale = 2)
    private BigDecimal repairCost;

    /**
     * Partie imputable pour le coût de réparation.
     * Valeurs : {@code OWNER | TENANT | SHARED}
     * {@code null} tant que le coût n'a pas été arbitré.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_imputed_to", length = 10)
    private CostImputedTo costImputedTo;

    /**
     * Note de résolution rédigée par l'agent SAV.
     * Décrit les travaux effectués, les pièces remplacées, etc.
     * Visible par le locataire et le propriétaire.
     */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /**
     * Évaluation de la résolution par le locataire (1 à 5 étoiles).
     * Renseignée lors de la clôture du ticket par le locataire.
     * {@code null} si le ticket a été clôturé automatiquement.
     */
    @Column(name = "rating")
    private Integer rating;

    /**
     * Commentaire libre du locataire lors de la clôture.
     */
    @Column(name = "rating_comment", columnDefinition = "TEXT")
    private String ratingComment;

    /**
     * {@code true} si le ticket est encore ouvert et nécessite une action.
     */
    public boolean isOpen() {
        return status != TicketStatus.CLOSED && status != TicketStatus.CANCELLED;
    }

    /**
     * {@code true} si le ticket est urgent et doit être traité immédiatement.
     */
    public boolean isUrgent() {
        return priority == TicketPriority.URGENT || priority == TicketPriority.HIGH;
    }

    /**
     * {@code true} si un prestataire a été assigné et l'intervention planifiée.
     */
    public boolean hasTechnicianAssigned() {
        return technicianName != null && interventionScheduledAt != null;
    }

    /**
     * {@code true} si le locataire a laissé une évaluation à la clôture.
     */
    public boolean hasRating() {
        return rating != null;
    }

    /**
     * {@code true} si le coût de réparation a été renseigné et arbitré.
     */
    public boolean isCostSettled() {
        return repairCost != null && costImputedTo != null;
    }
}