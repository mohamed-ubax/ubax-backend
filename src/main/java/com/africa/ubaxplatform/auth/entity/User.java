package com.africa.ubaxplatform.auth.entity;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.common.base.BaseEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Compte utilisateur de la plateforme UBAX.
 *
 * <p><b>Identité :</b> l'identité principale est gérée par Keycloak (OAuth2/OIDC).
 * Cette entité duplique les informations essentielles pour les requêtes métier
 * sans appel supplémentaire au serveur Keycloak.</p>
 *
 * <p><b>Rôles :</b> le rôle est synchronisé depuis le claim Keycloak à chaque
 * login et dupliqué ici pour les requêtes de filtrage (ex : lister les agents
 * d'une agence). La vérification d'accès réelle reste assurée par Spring Security.</p>
 *
 * <p><b>Avatar :</b> stocké dans le bucket MinIO {@code users-avatars}.
 * Seule l'URL est persistée dans {@code avatarUrl}.</p>
 *
 * <p><b>Soft delete :</b> un utilisateur n'est jamais supprimé physiquement
 * (contraintes RGPD et traçabilité des contrats). {@code deletedAt} non null
 * = compte supprimé.</p>
 */
@Entity
@Table(
        name = "users",
        schema = "administrative",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_keycloak_id", columnNames = "keycloak_id"),
                @UniqueConstraint(name = "uq_users_email",       columnNames = "email"),
                @UniqueConstraint(name = "uq_users_phone",       columnNames = "phone")
        },
        indexes = {
                @Index(name = "idx_users_role",       columnList = "role"),
                @Index(name = "idx_users_active",     columnList = "is_active"),
                @Index(name = "idx_users_deleted_at", columnList = "deleted_at"),
                @Index(name = "idx_users_agency",     columnList = "agency_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    // =========================================================================
    // KEYCLOAK
    // =========================================================================

    /**
     * Identifiant unique Keycloak (sub du JWT).
     * Non modifiable après création ({@code updatable = false}).
     * Sert de clé de réconciliation entre Keycloak et la base UBAX.
     */
    @Column(name = "keycloak_id", nullable = false, updatable = false, length = 100)
    private String keycloakId;

    /**
     * Rôle fonctionnel dupliqué depuis le realm Keycloak.
     * Utilisé pour les requêtes métier (filtrer les biens par agent, etc.).
     * La vérification d'accès réelle reste assurée par Spring Security via le JWT.
     * Valeurs : {@code CLIENT | OWNER | AGENT | AGENCY | ADMIN}
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private UserRole role = UserRole.CLIENT;

    /**
     * Date et heure de la dernière connexion réussie via Keycloak.
     * Mis à jour à chaque résolution de token dans le filtre JWT.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // =========================================================================
    // IDENTITÉ
    // =========================================================================

    /**
     * Prénom. Synchronisé depuis le claim Keycloak {@code given_name}
     * au premier login et modifiable par l'utilisateur ensuite.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Nom de famille. Synchronisé depuis le claim Keycloak {@code family_name}.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Adresse email principale. Synchronisée depuis le claim Keycloak {@code email}.
     * Utilisée pour les notifications, factures PDF et relances de loyer.
     */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * Numéro de téléphone au format international (ex : {@code +221781234567}).
     * Requis pour les notifications SMS, la vérification OTP et le Mobile Money.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Date de naissance. Utilisée pour les vérifications KYC et pour s'assurer
     * que l'utilisateur est majeur avant une transaction.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Adresse postale complète (rue, numéro, appartement).
     * Utilisée pour la facturation et les documents légaux.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * Ville de résidence. Pré-remplie dans les recherches immobilières.
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * Code pays ISO 3166-1 alpha-2 (ex : {@code SN} pour Sénégal).
     * Défaut : {@code SN}.
     */
    @Column(name = "country", length = 5)
    @Builder.Default
    private String country = "SN";

    /**
     * Code langue ISO 639-1 préféré pour l'interface.
     * Défaut : {@code fr}. Valeurs supportées : {@code fr | en | ar | wo}.
     */
    @Column(name = "language", length = 5)
    @Builder.Default
    private String language = "fr";

    /**
     * URL de l'avatar stocké dans le bucket MinIO {@code users-avatars}.
     * Format : {@code http://minio:9000/users-avatars/{keycloakId}.webp}
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Agence immobilière à laquelle l'utilisateur est rattaché.
     * Non null uniquement pour les rôles {@code AGENT} et {@code AGENCY}.
     * Ignoré pour les rôles {@code CLIENT}, {@code OWNER} et {@code ADMIN}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agency_id",
            foreignKey = @ForeignKey(name = "fk_user_agency")
    )
    private Agency agency;

    /**
     * {@code true} si l'email a été confirmé via le lien Keycloak (VERIFY_EMAIL).
     */
    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * {@code true} si le téléphone a été validé par code OTP SMS.
     */
    @Column(name = "is_phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    /**
     * {@code true} si une vérification KYC (pièce d'identité) a été effectuée.
     * Obligatoire pour les transactions dépassant un seuil défini (loi AML).
     */
    @Column(name = "is_identity_verified", nullable = false)
    @Builder.Default
    private boolean identityVerified = false;

    /**
     * {@code false} si le compte a été suspendu par un administrateur.
     * Un compte inactif est refusé au niveau du filtre JWT même si Keycloak
     * délivre un token valide (double vérification applicative).
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Date et heure de suppression logique du compte (soft delete).
     * Filtrer les comptes actifs : {@code WHERE deleted_at IS NULL}.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Nom complet affiché dans l'interface et les documents générés.
     * Exemple : {@code "Mohamed Ba"}.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * {@code true} si le profil est entièrement vérifié (email + téléphone + KYC).
     * Condition requise pour autoriser les transactions importantes.
     */
    public boolean isFullyVerified() {
        return emailVerified && phoneVerified && identityVerified;
    }

    /**
     * {@code true} si le compte a été supprimé logiquement.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * {@code true} si l'utilisateur est rattaché à une agence.
     * Vrai uniquement pour les rôles AGENT et AGENCY.
     */
    public boolean hasAgency() {
        return agency != null;
    }
}