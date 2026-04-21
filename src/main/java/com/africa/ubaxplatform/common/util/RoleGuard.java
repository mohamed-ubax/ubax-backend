package com.africa.ubaxplatform.common.util;

import com.africa.ubaxplatform.auth.codeList.PartnerRole;
import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitaire centralisé de vérification des rôles utilisateur.
 *
 * <p>Toutes les méthodes sont statiques. Usage type dans un contrôleur :
 *
 * <pre>{@code
 * RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
 * }</pre>
 *
 * <p>Pour récupérer l'utilisateur en même temps que la vérification :
 *
 * <pre>{@code
 * RequestUser user = RoleGuard.requireAdmin(requestHeaderParser, httpRequest);
 * }</pre>
 */
public final class RoleGuard {

  private RoleGuard() {}

  // ── Vérifications combinées (parse + check) ────────────────────

  /**
   * Extrait l'utilisateur du JWT et vérifie qu'il possède le rôle {@code ADMIN} ou {@code
   * SUPER_ADMIN}.
   *
   * @return l'utilisateur extrait du token
   * @throws CustomException 401 si le token est absent/invalide, 403 si le rôle est insuffisant
   */
  public static RequestUser requireAdmin(RequestHeaderParser parser, HttpServletRequest request)
      throws CustomException {
    RequestUser user = extractUser(parser, request);
    checkAdmin(user);
    return user;
  }

  /**
   * Extrait l'utilisateur du JWT et vérifie qu'il possède exclusivement le rôle {@code
   * SUPER_ADMIN}.
   *
   * @return l'utilisateur extrait du token
   * @throws CustomException 401 si le token est absent/invalide, 403 si le rôle est insuffisant
   */
  public static RequestUser requireSuperAdmin(
      RequestHeaderParser parser, HttpServletRequest request) throws CustomException {
    RequestUser user = extractUser(parser, request);
    checkAnyRole(user, UserRole.SUPER_ADMIN);
    return user;
  }

  /**
   * Extrait l'utilisateur du JWT et vérifie qu'il est authentifié (token valide, peu importe le
   * rôle).
   *
   * @return l'utilisateur extrait du token
   * @throws CustomException 401 si le token est absent ou invalide
   */
  public static RequestUser requireAuthenticated(
      RequestHeaderParser parser, HttpServletRequest request) throws CustomException {
    return extractUser(parser, request);
  }

  /**
   * Extrait l'utilisateur du JWT et vérifie qu'il possède au moins l'un des rôles fournis.
   *
   * @param roles liste des rôles acceptés (au moins un requis)
   * @return l'utilisateur extrait du token
   * @throws CustomException 401 si le token est absent/invalide, 403 si aucun rôle ne correspond
   */
  public static RequestUser requireAnyRole(
      RequestHeaderParser parser, HttpServletRequest request, UserRole... roles)
      throws CustomException {
    RequestUser user = extractUser(parser, request);
    checkAnyRole(user, roles);
    return user;
  }

  // ── Vérifications sur un RequestUser déjà extrait ─────────────

  /**
   * Vérifie qu'un {@link RequestUser} possède le rôle {@code ADMIN} ou {@code SUPER_ADMIN}.
   *
   * @throws CustomException 403 si le rôle est insuffisant
   */
  public static void checkAdmin(RequestUser user) throws CustomException {
    checkAnyRole(user, UserRole.ADMIN, UserRole.SUPER_ADMIN);
  }

  /**
   * Vérifie qu'un {@link RequestUser} possède au moins l'un des rôles fournis.
   *
   * @throws CustomException 403 si aucun rôle ne correspond
   */
  public static void checkAnyRole(RequestUser user, UserRole... roles) throws CustomException {
    for (UserRole role : roles) {
      if (user.hasRole(role)) return;
    }
    throw new CustomException(
        new UnAuthorizedException("Accès refusé – rôle insuffisant"),
        ResponseMessageConstants.USER_FORBIDDEN);
  }

  // ── Vérifications du rôle partenaire interne (DB) ─────────────

  /**
   * Vérifie qu'un {@link User} (extrait de la base) possède au moins l'un des {@link PartnerRole}
   * fournis.
   *
   * <p>Usage type dans un contrôleur :
   *
   * <pre>{@code
   * RequestUser caller = RoleGuard.requireAnyRole(parser, request, UserRole.PARTNER);
   * User dbUser = userRepository.findByKeycloakId(caller.getSub()).orElseThrow(...);
   * RoleGuard.checkPartnerRole(dbUser, PartnerRole.DIRECTEUR_AGENCE);
   * }</pre>
   *
   * @param user entité {@code User} chargée depuis la base de données
   * @param roles liste des rôles partenaires acceptés (au moins un requis)
   * @throws CustomException 403 si aucun rôle ne correspond ou si {@code partnerRole} est null
   */
  public static void checkPartnerRole(User user, PartnerRole... roles) throws CustomException {
    PartnerRole actual = user.getPartnerRole();
    if (actual != null) {
      for (PartnerRole role : roles) {
        if (actual == role) return;
      }
    }
    throw new CustomException(
        new UnAuthorizedException("Accès refusé – rôle partenaire insuffisant"),
        ResponseMessageConstants.USER_FORBIDDEN);
  }

  /**
   * Vérifie qu'un {@link User} est bien rattaché à une agence.
   *
   * @param user entité {@code User} chargée depuis la base de données
   * @throws CustomException 403 si l'utilisateur n'est rattaché à aucune agence
   */
  public static void checkHasAgency(User user) throws CustomException {
    if (!user.hasAgency()) {
      throw new CustomException(
          new UnAuthorizedException("Accès refusé – utilisateur non rattaché à une agence"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
  }

  // ── Extraction du token ────────────────────────────────────────

  private static RequestUser extractUser(RequestHeaderParser parser, HttpServletRequest request)
      throws CustomException {
    RequestUser user = parser.parseUserFromRequest(request);
    if (user == null) {
      throw new CustomException(
          new NotFoundException("Utilisateur inconnu"), "Utilisateur inconnu");
    }
    return user;
  }
}
