package com.africa.ubaxplatform.auth.service.interfaces;

import com.africa.ubaxplatform.auth.dto.ClientUserResponse;
import com.africa.ubaxplatform.common.exception.CustomException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientListingService {

  /** Tous les clients (UBAX_CLIENT) actifs de la plateforme — réservé admin. */
  Page<ClientUserResponse> listAllClients(Pageable pageable);

  /** Clients distincts d'un hôtel, identifiés via leurs réservations. */
  Page<ClientUserResponse> listClientsByHotel(UUID hotelId, Pageable pageable)
      throws CustomException;

  /** Clients distincts d'une agence, identifiés via leurs contrats. */
  Page<ClientUserResponse> listClientsByAgency(UUID agencyId, Pageable pageable)
      throws CustomException;

  /** Clients de l'agence du PARTNER appelant (résolution automatique via keycloakId). */
  Page<ClientUserResponse> listClientsByCallerAgency(String callerKeycloakId, Pageable pageable)
      throws CustomException;

  /** Clients de l'hôtel du PARTNER appelant (résolution automatique via keycloakId). */
  Page<ClientUserResponse> listClientsByCallerHotel(String callerKeycloakId, Pageable pageable)
      throws CustomException;
}
