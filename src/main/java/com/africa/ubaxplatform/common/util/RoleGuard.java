package com.africa.ubaxplatform.common.util;

import com.africa.ubaxplatform.auth.codeList.AgenceRole;
import com.africa.ubaxplatform.auth.codeList.HotelRole;
import com.africa.ubaxplatform.auth.codeList.RoleScope;
import com.africa.ubaxplatform.auth.codeList.UbaxAdminRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.repository.UserSubRoleRepository;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.TokenRetrievalException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitaire centralisé de vérification des rôles utilisateur.
 *
 * <p><b>Deux niveaux de vérification :</b>
 *
 * <ol>
 *   <li><b>Rôle Keycloak (JWT)</b> — via {@link RequestUser} extrait du Bearer token. Méthodes :
 *       {@code require*}, {@code checkAnyRole}.
 *   <li><b>Sous-rôle DB</b> — via l'entité {@link User} chargée en base. Méthodes : {@code
 *       checkSubRole}, {@code checkAgenceRole}, {@code checkHotelRole}, {@code checkAdminSubRole}.
 * </ol>
 *
 * <p><b>Pattern type dans un contrôleur :</b>
 *
 * <pre>{@code
 * // 1. Vérifier le rôle Keycloak
 * RequestUser caller = RoleGuard.requireAnyRole(parser, request, UserRole.PARTNER);
 * // 2. Charger l'entité User en base
 * User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
 * // 3. Vérifier le sous-rôle applicatif
 * RoleGuard.checkAgenceRole(dbUser, subRoleRepo, AgenceRole.DIRECTEUR_AGENCE);
 * }</pre>
 */
public final class RoleGuard {

  private static final Logger log = LoggerFactory.getLogger(RoleGuard.class);

  private RoleGuard() {}

  // ── Niveau 1 : Rôle Keycloak (JWT) ────────────────────────────

  public static RequestUser requireAdmin(RequestHeaderParser parser, HttpServletRequest request)
      throws CustomException {
    RequestUser user = extractUser(parser, request);
    checkAdmin(user);
    return user;
  }

  public static RequestUser requireSuperAdmin(
      RequestHeaderParser parser, HttpServletRequest request) throws CustomException {
    RequestUser user = extractUser(parser, request);
    checkAnyRole(user, UserRole.SUPER_ADMIN);
    return user;
  }

  public static RequestUser requireAuthenticated(
      RequestHeaderParser parser, HttpServletRequest request) throws CustomException {
    return extractUser(parser, request);
  }

  public static RequestUser requireAnyRole(
      RequestHeaderParser parser, HttpServletRequest request, UserRole... roles)
      throws CustomException {
    RequestUser user = extractUser(parser, request);
    checkAnyRole(user, roles);
    return user;
  }

  public static void checkAdmin(RequestUser user) throws CustomException {
    checkAnyRole(user, UserRole.ADMIN, UserRole.SUPER_ADMIN);
  }

  public static void checkAnyRole(RequestUser user, UserRole... roles) throws CustomException {
    for (UserRole role : roles) {
      if (user.hasRole(role)) {
        log.debug("[ROLE] ✔ sub={} | rôle={} autorisé", user.getSub(), role);
        return;
      }
    }
    log.warn(
        "[ROLE] ✗ Accès refusé | sub={} | rôles actuels={} | rôles attendus={}",
        user.getSub(),
        user.getRealmRoles(),
        roles);
    throw new CustomException(
        new UnAuthorizedException("Accès refusé – rôle Keycloak insuffisant"),
        ResponseMessageConstants.USER_FORBIDDEN);
  }

  // ── Niveau 2 : Sous-rôle DB ───────────────────────────────────

  /**
   * Vérifie qu'un utilisateur possède un sous-rôle agence donné dans {@code user_sub_roles}.
   *
   * @throws CustomException 403 si le sous-rôle est absent
   */
  public static void checkAgenceRole(
      User dbUser, UserSubRoleRepository subRoleRepo, AgenceRole... roles) throws CustomException {
    for (AgenceRole role : roles) {
      if (subRoleRepo.existsByUserIdAndRoleAndScope(
          dbUser.getId(), role.name(), RoleScope.AGENCE)) {
        log.debug("[SUB_ROLE] ✔ userId={} | sous-rôle agence={} autorisé", dbUser.getId(), role);
        return;
      }
    }
    log.warn(
        "[SUB_ROLE] ✗ Accès refusé | userId={} | sous-rôles agence attendus={}",
        dbUser.getId(),
        roles);
    throw new CustomException(
        new UnAuthorizedException("Accès refusé – sous-rôle agence insuffisant"),
        ResponseMessageConstants.USER_FORBIDDEN);
  }

  /**
   * Vérifie qu'un utilisateur possède un sous-rôle hôtel donné dans {@code user_sub_roles}.
   *
   * @throws CustomException 403 si le sous-rôle est absent
   */
  public static void checkHotelRole(
      User dbUser, UserSubRoleRepository subRoleRepo, HotelRole... roles) throws CustomException {
    for (HotelRole role : roles) {
      if (subRoleRepo.existsByUserIdAndRoleAndScope(dbUser.getId(), role.name(), RoleScope.HOTEL)) {
        return;
      }
    }
    throw new CustomException(
        new UnAuthorizedException("Accès refusé – sous-rôle hôtel insuffisant"),
        ResponseMessageConstants.USER_FORBIDDEN);
  }

  /**
   * Vérifie qu'un utilisateur possède un sous-rôle admin interne UBAX dans {@code user_sub_roles}.
   *
   * @throws CustomException 403 si le sous-rôle est absent
   */
  public static void checkAdminSubRole(
      User dbUser, UserSubRoleRepository subRoleRepo, UbaxAdminRole... roles)
      throws CustomException {
    for (UbaxAdminRole role : roles) {
      if (subRoleRepo.existsByUserIdAndRoleAndScope(
          dbUser.getId(), role.name(), RoleScope.UBAX_INTERNAL)) {
        return;
      }
    }
    throw new CustomException(
        new UnAuthorizedException("Accès refusé – sous-rôle admin insuffisant"),
        ResponseMessageConstants.USER_FORBIDDEN);
  }

  /**
   * Vérifie qu'un utilisateur possède au moins un sous-rôle dans un scope donné.
   *
   * @throws CustomException 403 si aucun sous-rôle du scope n'est assigné
   */
  public static void checkHasSubRole(
      User dbUser, UserSubRoleRepository subRoleRepo, String role, RoleScope scope)
      throws CustomException {
    if (!subRoleRepo.existsByUserIdAndRoleAndScope(dbUser.getId(), role, scope)) {
      throw new CustomException(
          new UnAuthorizedException(
              "Accès refusé – sous-rôle '" + role + "' (scope " + scope + ") manquant"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  // ── Extraction du token ────────────────────────────────────────

  private static RequestUser extractUser(RequestHeaderParser parser, HttpServletRequest request)
      throws CustomException {
    RequestUser user = parser.parseUserFromRequest(request);
    if (user == null) {
      log.warn(
          "[AUTH] Token absent ou invalide | uri={} method={}",
          request.getRequestURI(),
          request.getMethod());
      throw new TokenRetrievalException("Token JWT absent ou invalide – authentification requise");
    }
    log.debug("[AUTH] Token valide | sub={} | uri={}", user.getSub(), request.getRequestURI());
    return user;
  }
}
