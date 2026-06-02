package com.africa.ubaxplatform.payment.scheduler;

import com.africa.ubaxplatform.common.constants.Constants;
import com.africa.ubaxplatform.contract.codeList.ContractStatus;
import com.africa.ubaxplatform.contract.entity.Contract;
import com.africa.ubaxplatform.contract.repository.ContractRepository;
import com.africa.ubaxplatform.payment.codeList.PaymentStatus;
import com.africa.ubaxplatform.payment.codeList.PaymentType;
import com.africa.ubaxplatform.payment.entity.Payment;
import com.africa.ubaxplatform.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jobs planifiés liés au cycle de vie des paiements.
 *
 * <p>Deux tâches quotidiennes :
 *
 * <ol>
 *   <li>{@link #markOverduePayments()} — 01h00 : marque {@code LATE} les paiements en retard.
 *   <li>{@link #generateUpcomingRentPayments()} — 06h00 : génère les loyers dont l'échéance tombe
 *       dans les 35 prochains jours et qui n'ont pas encore d'entrée créée. Cette fenêtre couvre
 *       tous les cas sans dépendre d'une date fixe dans le mois.
 * </ol>
 *
 * <p><b>Logique du premier loyer :</b> le premier loyer est créé par {@code
 * ContractServiceImpl.activate()} au moment de la signature. Ce scheduler gère uniquement les
 * loyers récurrents (2ème mois et au-delà).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSchedulerJob {

  /**
   * Fenêtre de génération anticipée en jours. Si l'échéance tombe dans les 35 prochains jours et
   * qu'aucune entrée n'existe, le paiement est créé.
   */
  private static final int LOOK_AHEAD_DAYS = 35;

  private final ContractRepository contractRepo;
  private final PaymentRepository paymentRepo;

  /**
   * Marque {@code LATE} les paiements {@code PENDING}/{@code PARTIAL} dont l'échéance est passée.
   */
  @Scheduled(cron = "0 0 1 * * *")
  @Transactional
  public void markOverduePayments() {
    List<Payment> overdue =
        paymentRepo.findOverdueByStatuses(
            Set.of(PaymentStatus.PENDING, PaymentStatus.PARTIAL), LocalDate.now());

    if (overdue.isEmpty()) {
      log.debug("[PaymentScheduler] markOverdue – aucun paiement en retard.");
      return;
    }

    overdue.forEach(p -> p.setStatus(PaymentStatus.LATE));
    paymentRepo.saveAll(overdue);
    log.info("[PaymentScheduler] markOverdue – {} paiement(s) marqué(s) LATE.", overdue.size());
  }

  /**
   * Génère les loyers des baux actifs dont l'échéance tombe dans les {@value LOOK_AHEAD_DAYS}
   * prochains jours. Idempotent : un paiement existant pour la même échéance n'est pas recréé.
   *
   * <p><b>Exemple :</b> aujourd'hui = 10 mai, paymentDay = 15 → génère le loyer dû le 15 mai si
   * absent. Si paymentDay = 5 → la prochaine échéance est le 5 juin (dans 26 jours) → générée
   * aussi.
   */
  @Scheduled(cron = "0 0 6 * * *")
  @Transactional
  public void generateUpcomingRentPayments() {
    LocalDate today = LocalDate.now();
    LocalDate horizon = today.plusDays(LOOK_AHEAD_DAYS);

    List<Contract> activeLeases =
        contractRepo.findAllByStatusAndContractType(
            ContractStatus.ACTIVE, Constants.CodeList.ContractType.LEASE);

    if (activeLeases.isEmpty()) {
      log.debug("[PaymentScheduler] generateUpcoming – aucun bail actif.");
      return;
    }

    int created = 0;

    for (Contract contract : activeLeases) {
      LocalDate dueDate = nextDueDate(contract, today);

      // La prochaine échéance est hors de la fenêtre → trop tôt pour générer
      if (dueDate.isAfter(horizon)) {
        continue;
      }

      if (paymentRepo.existsByContractIdAndDueDateAndDeletedAtIsNull(contract.getId(), dueDate)) {
        continue;
      }

      // Vérifier que le contrat est encore actif à la date d'échéance
      if (contract.getEndDate() != null && dueDate.isAfter(contract.getEndDate())) {
        log.debug(
            "[PaymentScheduler] contractId={} – bail expiré avant l'échéance {}, ignoré.",
            contract.getId(),
            dueDate);
        continue;
      }

      BigDecimal amount = resolveAmount(contract);
      if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        log.warn(
            "[PaymentScheduler] contractId={} – loyer nul, paiement ignoré.", contract.getId());
        continue;
      }

      String reference =
          "PAY-%d-%02d-RENT-%s"
              .formatted(
                  dueDate.getYear(),
                  dueDate.getMonthValue(),
                  contract.getId().toString().substring(0, 8).toUpperCase());

      Payment payment =
          Payment.builder()
              .contract(contract)
              .tenant(contract.getTenant())
              .property(contract.getProperty())
              .agency(contract.getProperty() != null ? contract.getProperty().getAgency() : null)
              .paymentType(PaymentType.RENT)
              .status(PaymentStatus.PENDING)
              .amount(amount)
              .dueDate(dueDate)
              .periodLabel(buildPeriodLabel(dueDate))
              .reference(reference)
              .build();

      paymentRepo.save(payment);
      created++;
    }

    log.info(
        "[PaymentScheduler] generateUpcoming – {} paiement(s) créé(s) (horizon : {}).",
        created,
        horizon);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Calcule la prochaine échéance pour un contrat à partir d'une date de référence.
   *
   * <p>Si le {@code paymentDay} de ce mois-ci est déjà passé ou correspond à aujourd'hui, on passe
   * au mois suivant.
   */
  private LocalDate nextDueDate(Contract contract, LocalDate from) {
    int payDay = contract.getPaymentDay();

    LocalDate candidate = from.withDayOfMonth(Math.min(payDay, from.lengthOfMonth()));
    if (!candidate.isAfter(from)) {
      LocalDate next = from.plusMonths(1);
      candidate = next.withDayOfMonth(Math.min(payDay, next.lengthOfMonth()));
    }
    return candidate;
  }

  private BigDecimal resolveAmount(Contract contract) {
    BigDecimal total = contract.getTotalMonthlyAmount();
    return total != null ? total : contract.getMonthlyRent();
  }

  private String buildPeriodLabel(LocalDate date) {
    String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
    String label = Character.toUpperCase(month.charAt(0)) + month.substring(1);
    return label + " " + date.getYear();
  }
}
