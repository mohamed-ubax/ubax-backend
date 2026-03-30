package com.africa.ubaxplatform.common.util;

import com.africa.ubaxplatform.auth.codeList.UserRole;
import com.africa.ubaxplatform.auth.dto.RequestUser;
import com.africa.ubaxplatform.common.constants.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Extrait et parse l'utilisateur courant à partir du JWT Bearer présent dans la requête HTTP.
 *
 * <p>Le JWT est déjà validé par Spring Security (OAuth2 Resource Server) avant d'atteindre les
 * contrôleurs. Ce service décode simplement le payload pour récupérer les claims métier (sub,
 * email, rôles, etc.) sans revalider la signature.
 *
 * <p><b>Usage type dans un contrôleur :</b>
 *
 * <pre>{@code
 * RequestUser user = requestHeaderParser.parseUserFromRequest(request);
 * if (user == null) throw new CustomException(new UnAuthorizedException("Unknown user"), "Unknown user");
 * }</pre>
 */
@Service
@Slf4j
public class RequestHeaderParser {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  // ── Public API ─────────────────────────────────────────────────

  /**
   * Extrait l'utilisateur depuis le header {@code Authorization: Bearer <token>} de la requête.
   *
   * @param request la requête HTTP entrante
   * @return {@link RequestUser} peuplé depuis le JWT, ou {@code null} si absent/malformé
   */
  public RequestUser parseUserFromRequest(HttpServletRequest request) {
    String token = extractBearerToken(request);
    if (token == null) {
      log.warn("No Bearer token found in Authorization header");
      return null;
    }

    String payload = decodePayload(token);
    if (payload == null) return null;

    return parseUserFromPayload(payload);
  }

  // ── Private helpers ────────────────────────────────────────────

  /**
   * Extrait le token brut depuis le header {@code Authorization: Bearer <token>}. Retourne
   * {@code null} si le header est absent ou ne commence pas par {@code Bearer }.
   */
  private String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader(Constants.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      return header.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  /**
   * Décode la partie payload (index 1) d'un JWT en Base64URL et retourne le JSON brut. Retourne
   * {@code null} si le token est malformé (pas 3 parties).
   */
  private String decodePayload(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      log.error("Malformed JWT: expected 3 parts, got {}", parts.length);
      return null;
    }
    try {
      byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
      String payload = new String(decodedBytes);
      log.debug("JWT payload decoded: {}", payload);
      return payload;
    } catch (IllegalArgumentException e) {
      log.error("Failed to Base64-decode JWT payload: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Désérialise le JSON du payload en {@link RequestUser} et déduit le rôle UBAX principal.
   * Retourne {@code null} si la désérialisation échoue.
   */
  private RequestUser parseUserFromPayload(String payload) {
    try {
      RequestUser user = MAPPER.readValue(payload, RequestUser.class);
      user.setRole(resolveRole(user));
      log.debug(
          "Parsed RequestUser: sub={}, email={}, role={}", user.getSub(), user.getEmail(), user.getRole());
      return user;
    } catch (Exception e) {
      log.error("Failed to parse JWT payload into RequestUser: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Identifie le rôle UBAX principal en cherchant, dans l'ordre de priorité, parmi les rôles du
   * realm ({@code UBAX_SUPER_ADMIN > UBAX_ADMIN > UBAX_AGENCY > UBAX_AGENT > UBAX_OWNER >
   * UBAX_CLIENT}).
   *
   * <p>Retourne {@code UserRole.CLIENT} par défaut si aucun rôle UBAX n'est trouvé.
   */
  private UserRole resolveRole(RequestUser user) {
    // Ordre de priorité décroissante
    UserRole[] priority = {
      UserRole.SUPER_ADMIN,
      UserRole.ADMIN,
      UserRole.AGENCY,
      UserRole.AGENT,
      UserRole.OWNER,
      UserRole.CLIENT
    };

    for (UserRole candidate : priority) {
      if (user.hasRole(candidate)) return candidate;
    }

    log.warn("No UBAX role found in JWT for sub={}, defaulting to CLIENT", user.getSub());
    return UserRole.CLIENT;
  }
}
