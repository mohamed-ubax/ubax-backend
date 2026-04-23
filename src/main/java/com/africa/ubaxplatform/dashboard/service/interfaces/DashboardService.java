package com.africa.ubaxplatform.dashboard.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.dashboard.dto.AgencyDashboardResponse;
import java.time.LocalDate;

public interface DashboardService {

  /**
   * Retourne les KPIs analytiques de l'agence connectée pour la période donnée.
   *
   * <p>Si {@code from} ou {@code to} sont {@code null}, la période par défaut est le mois courant.
   *
   * @param callerKeycloakId identifiant Keycloak de l'appelant
   * @param from début de période (inclus) – {@code null} = 1er du mois courant
   * @param to fin de période (inclus) – {@code null} = dernier jour du mois courant
   */
  AgencyDashboardResponse getAgencyDashboard(String callerKeycloakId, LocalDate from, LocalDate to)
      throws CustomException;
}
