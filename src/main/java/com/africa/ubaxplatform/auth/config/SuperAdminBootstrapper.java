package com.africa.ubaxplatform.auth.config;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.CreateAdminRequest;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.AdminManagementService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initialise le premier compte Super Administrateur au démarrage de l'application lorsque la base
 * de données ne contient encore aucun administrateur.
 *
 * <p><b>Comportement :</b>
 *
 * <ol>
 *   <li>Si {@code ubax.bootstrap.enabled = false} (valeur par défaut) → pas d'action.
 *   <li>Si un {@code SUPER_ADMIN} existe déjà en base → pas d'action (idempotent).
 *   <li>Sinon → crée le compte Keycloak, attribue le rôle {@code SUPER_ADMIN}, persiste l'entité
 *       {@link com.africa.ubaxplatform.auth.entity.User} et envoie un email de définition du mot de
 *       passe à l'adresse configurée.
 * </ol>
 *
 * <p><b>Usage :</b> définir les variables d'environnement (ou surcharger dans {@code
 * application-local.yml}) :
 *
 * <pre>
 * BOOTSTRAP_ENABLED=true
 * BOOTSTRAP_SUPER_ADMIN_EMAIL=admin@ubax.io
 * BOOTSTRAP_SUPER_ADMIN_FIRST_NAME=Super
 * BOOTSTRAP_SUPER_ADMIN_LAST_NAME=Admin
 * BOOTSTRAP_SUPER_ADMIN_PHONE=+22600000000
 * </pre>
 *
 * <p>Une fois le compte créé, repasser {@code BOOTSTRAP_ENABLED=false} pour les démarrages
 * suivants.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminBootstrapper implements ApplicationRunner {

  private final SuperAdminBootstrapProperties props;
  private final UserRepository userRepository;
  private final AdminManagementService adminManagementService;

  @Override
  public void run(ApplicationArguments args) {
    if (!props.isEnabled()) {
      log.debug("[Bootstrap] Bootstrap désactivé (ubax.bootstrap.enabled=false) — ignoré.");
      return;
    }

    long existingCount = userRepository.findAdminUsers(Set.of(UserRole.SUPER_ADMIN)).size();
    if (existingCount > 0) {
      log.info(
          "[Bootstrap] {} SUPER_ADMIN détecté(s) en base — initialisation ignorée.", existingCount);
      return;
    }

    log.info(
        "[Bootstrap] Aucun SUPER_ADMIN en base. Création du compte bootstrap : email={}",
        props.getEmail());

    try {
      CreateAdminRequest request = new CreateAdminRequest();
      request.setEmail(props.getEmail());
      request.setFirstName(props.getFirstName());
      request.setLastName(props.getLastName());
      request.setPhone(props.getPhone());
      request.setRole(UserRole.SUPER_ADMIN);

      adminManagementService.createAdmin(request);

      log.info("═══════════════════════════════════════════════════════════");
      log.info("[Bootstrap] Compte SUPER_ADMIN créé avec succès.");
      log.info("[Bootstrap]   Email : {}", props.getEmail());
      log.info("[Bootstrap]   Un lien de définition du mot de passe a été envoyé par email.");
      log.info("[Bootstrap]   → Pensez à désactiver BOOTSTRAP_ENABLED après connexion.");
      log.info("═══════════════════════════════════════════════════════════");

    } catch (Exception e) {
      log.error("═══════════════════════════════════════════════════════════");
      log.error("[Bootstrap] ÉCHEC de la création du compte SUPER_ADMIN.");
      log.error("[Bootstrap]   Cause : {}", e.getMessage());
      log.error(
          "[Bootstrap]   → Vérifiez que le realm Keycloak '{}' existe et que le SMTP est configuré.",
          "ubax-plateform");
      log.error(
          "[Bootstrap]   → L'application continue — relancez avec BOOTSTRAP_ENABLED=true après correction.");
      log.error("═══════════════════════════════════════════════════════════");
    }
  }
}
