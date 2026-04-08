package com.africa.ubaxplatform.partner.service.interfaces;

import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.ApplicationDecisionRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/** Contrat du service de gestion des demandes d'adhésion partenaire. */
public interface PartnerApplicationService {

  /**
   * Soumet une nouvelle demande d'adhésion partenaire.
   *
   * @param request formulaire de demande (champs texte)
   * @param rccmUrl URL MinIO du RCCM uploadé par le contrôleur (null si absent)
   * @param dfeUrl URL MinIO du DFE uploadé par le contrôleur (null si absent)
   * @param bailUrl URL MinIO du bail uploadé par le contrôleur (null si absent)
   * @param logoUrl URL MinIO du logo uploadé par le contrôleur (null si absent)
   * @return la demande créée avec statut {@code PENDING}
   */
  PartnerApplicationResponse apply(
      PartnerApplicationRequest request,
      String rccmUrl,
      String dfeUrl,
      String bailUrl,
      String logoUrl);

  /**
   * Récupère les demandes filtrées par statut (endpoint admin paginé).
   *
   * @param status filtre optionnel ; null = toutes les demandes
   * @param pageable pagination
   * @return page de demandes
   */
  Page<PartnerApplicationResponse> listApplications(ApplicationStatus status, Pageable pageable);

  /**
   * Récupère le détail d'une demande avec son historique de statuts.
   *
   * @param id identifiant de la demande
   * @return détail complet incluant l'historique
   */
  PartnerApplicationResponse getApplication(UUID id);

  /**
   * Applique une décision administrative sur une demande.
   *
   * @param id identifiant de la demande
   * @param adminKeycloakId identifiant Keycloak de l'administrateur connecté
   * @param decision objet contenant le nouveau statut et le commentaire
   * @return la demande mise à jour
   */
  PartnerApplicationResponse decide(
      UUID id, String adminKeycloakId, ApplicationDecisionRequest decision);

  String uploadIfPresent(
      MultipartFile file, String docKey, Set<String> allowedTypes, long maxBytes);
}
