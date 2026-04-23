package com.africa.ubaxplatform.property.scheduler;

import com.africa.ubaxplatform.property.codeList.PropertyStatus;
import com.africa.ubaxplatform.property.entity.Property;
import com.africa.ubaxplatform.property.repository.PropertyRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jobs planifiés liés au cycle de vie des biens immobiliers.
 *
 * <p>Deux tâches sont exécutées chaque nuit à 02h00 :
 *
 * <ol>
 *   <li>{@link #expireBoosts()} — désactive le flag {@code boosted} des biens dont {@code
 *       boostExpiresAt} est dépassé.
 *   <li>{@link #archiveExpiredProperties()} — passe en {@code ARCHIVED} les biens {@code PUBLISHED}
 *       dont {@code expiresAt} est dépassé.
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PropertySchedulerJob {

  private final PropertyRepository propertyRepo;

  /**
   * Désactive le boost des annonces dont la période payante est écoulée.
   *
   * <p>Tourne chaque nuit à 02h00. Remet {@code boosted = false} et {@code boostExpiresAt = null}
   * sur tous les biens dont {@code boostExpiresAt < maintenant}. L'annonce reste visible mais n'est
   * plus propulsée en tête des résultats.
   *
   * <p>Cron : {@code 0 0 2 * * *} — chaque jour à 02:00:00.
   */
  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void expireBoosts() {
    LocalDateTime now = LocalDateTime.now();
    List<Property> expired = propertyRepo.findExpiredBoosts(now);

    if (expired.isEmpty()) {
      log.debug("[PropertyScheduler] expireBoosts – aucun boost expiré.");
      return;
    }

    expired.forEach(
        p -> {
          p.setBoosted(false);
          p.setBoostExpiresAt(null);
        });

    propertyRepo.saveAll(expired);
    log.info("[PropertyScheduler] expireBoosts – {} boost(s) désactivé(s).", expired.size());
  }

  /**
   * Archive les annonces publiées dont la date d'expiration est dépassée.
   *
   * <p>Tourne chaque nuit à 02h05 (décalage de 5 min après {@link #expireBoosts()}). Passe en
   * {@code ARCHIVED} tous les biens en statut {@code PUBLISHED} dont {@code expiresAt <
   * maintenant}. Les biens archivés disparaissent du portail public. Le partenaire peut les
   * remettre en {@code DRAFT} pour les retravailler et republier.
   *
   * <p>Cron : {@code 0 5 2 * * *} — chaque jour à 02:05:00.
   */
  @Scheduled(cron = "0 5 2 * * *")
  @Transactional
  public void archiveExpiredProperties() {
    LocalDateTime now = LocalDateTime.now();
    List<Property> toArchive = propertyRepo.findExpiredPublished(now);

    if (toArchive.isEmpty()) {
      log.debug("[PropertyScheduler] archiveExpiredProperties – aucune annonce expirée.");
      return;
    }

    toArchive.forEach(p -> p.setStatus(PropertyStatus.ARCHIVED));
    propertyRepo.saveAll(toArchive);
    log.info(
        "[PropertyScheduler] archiveExpiredProperties – {} annonce(s) archivée(s).",
        toArchive.size());
  }
}
