package com.africa.ubaxplatform.common.util;

import com.africa.ubaxplatform.common.constants.Constants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrait les rôles Keycloak du claim {@code realm_access.roles} et les expose comme {@link
 * GrantedAuthority} dans Spring Security.
 *
 * <p>Seuls les rôles préfixés {@code UBAX_} sont conservés (ex: {@code UBAX_ADMIN}, {@code
 * UBAX_AGENT}). Les rôles techniques Keycloak ({@code offline_access}, {@code uma_authorization},
 * etc.) sont ignorés.
 *
 * <p>Avec {@link org.springframework.security.config.core.GrantedAuthorityDefaults
 * GrantedAuthorityDefaults("")}, les contrôleurs utilisent directement :
 *
 * <pre>{@code @PreAuthorize("hasAuthority('UBAX_ADMIN')")}</pre>
 */
@Slf4j
public class KeycloakJwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String CLAIM_REALM_ACCESS = "realm_access";
  private static final String CLAIM_ROLES = "roles";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Collection<String>> realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);
    Collection<GrantedAuthority> authorities = new ArrayList<>();

    if (realmAccess == null || realmAccess.isEmpty()) {
      return authorities;
    }

    Collection<String> roles = realmAccess.get(CLAIM_ROLES);
    if (roles == null || roles.isEmpty()) {
      return authorities;
    }

    roles.stream()
        .filter(role -> role.startsWith(Constants.KEYCLOAK_ROLE_PREFIX))
        .forEach(
            role -> {
              log.debug("Mapping Keycloak role '{}' to GrantedAuthority", role);
              authorities.add(new SimpleGrantedAuthority(role));
            });

    return authorities;
  }
}