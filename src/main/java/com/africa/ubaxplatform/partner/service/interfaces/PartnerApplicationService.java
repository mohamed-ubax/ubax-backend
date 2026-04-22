package com.africa.ubaxplatform.partner.service.interfaces;

import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.partner.codeList.ApplicationStatus;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationRequest;
import com.africa.ubaxplatform.partner.dto.PartnerApplicationResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/** Contrat du service de gestion des demandes d'adhésion partenaire. */
public interface PartnerApplicationService {

  /**
   * Soumet une nouvelle demande d'adhésion partenaire.
   *
   * <p>Crée automatiquement la structure de répertoires MinIO pour le partenaire ({@code
   * partner-documents/{slug}/{legal,logo,contracts}/}) et uploade les fichiers reçus dans les
   * sous-répertoires correspondants.
   *
   * @param request formulaire de demande (champs texte)
   * @param rccm fichier RCCM (optionnel)
   * @param dfe fichier DFE (optionnel)
   * @param bail contrat de bail (optionnel)
   * @param logo logo de l'entreprise (optionnel)
   * @return la demande créée avec statut {@code PENDING}
   */
  PartnerApplicationResponse apply(
      PartnerApplicationRequest request,
      MultipartFile rccm,
      MultipartFile dfe,
      MultipartFile bail,
      MultipartFile logo)
      throws CustomException;

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
   * @param newStatus nouveau statut (à choisir parmi UNDER_REVIEW, APPROVED, REJECTED, INCOMPLETE)
   * @param comment motif obligatoire pour REJECTED et INCOMPLETE
   * @return la demande mise à jour
   */
  PartnerApplicationResponse decide(
      UUID id, String adminKeycloakId, ApplicationStatus newStatus, String comment);
}
