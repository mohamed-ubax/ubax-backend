package com.africa.ubaxplatform.property.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Requête de configuration de la date d'expiration automatique d'une annonce publiée.
 *
 * <p>À la date {@code expiresAt}, le scheduler {@code PropertySchedulerJob} passe automatiquement
 * l'annonce en statut {@code ARCHIVED}. Le partenaire est notifié et peut remettre l'annonce en
 * {@code DRAFT} pour la republier.
 *
 * <p>Pour supprimer une expiration existante, utiliser {@code DELETE
 * /v1/properties/{id}/expiration}.
 */
@Schema(description = "Date d'expiration automatique de l'annonce")
public record PropertyExpirationRequest(
    @Schema(
            description = "Date et heure d'expiration de l'annonce. Doit être dans le futur.",
            example = "2026-12-31T23:59:59")
        @NotNull(message = "expiresAt est obligatoire")
        @Future(message = "La date d'expiration doit être dans le futur")
        LocalDateTime expiresAt) {}
