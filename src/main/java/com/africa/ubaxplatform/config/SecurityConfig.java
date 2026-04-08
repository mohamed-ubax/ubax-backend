package com.africa.ubaxplatform.config;

import com.africa.ubaxplatform.common.util.KeycloakJwtRolesConverter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Slf4j
public class SecurityConfig {

  private static final String[] WHITELIST = {
    "/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/webjars/**",
    "/v3/api-docs/**",
    "/actuator/prometheus",
    "/actuator/health/**",
    "/actuator/info",
    "/v1/auth/login",
    "/v1/auth/login/phone",
    "/v1/auth/logout",
    "/v1/auth/forgot-password",
    "/v1/auth/forgot-password/send-otp",
    "/v1/auth/forgot-password/verify-otp",
    "/v1/auth/forgot-password/reset",
    "/v1/auth/register/**",
    "/v1/partner/apply"
  };

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String tokenIssuerUrl;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  @Value("${ubax.endpoints.frontend}")
  private String frontEndUrl;

  @Value("${ubax.security.enabled:true}")
  private boolean securityEnabled;

  // ── Chaîne publique (ordre 1) – pas de validation JWT ─────────

  /**
   * Filter chain pour les routes publiques. Prioritaire sur la chaîne sécurisée. Aucun
   * BearerTokenAuthenticationFilter n'est enregistré ici : un token invalide ou expiré est
   * simplement ignoré au lieu de provoquer un 401.
   */
  @Bean
  @Order(1)
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(WHITELIST)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  // ── Chaîne sécurisée (ordre 2) – JWT requis ────────────────────

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, CustomAuthenticationEntryPoint entryPoint, CustomAccessDenied accessDenied)
      throws Exception {
    if (securityEnabled) {
      DelegatingJwtGrantedAuthoritiesConverter authoritiesConverter =
          new DelegatingJwtGrantedAuthoritiesConverter(
              new JwtGrantedAuthoritiesConverter(), new KeycloakJwtRolesConverter());

      http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .exceptionHandling(
              ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
          .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
          .oauth2ResourceServer(
              oauth2 ->
                  oauth2.jwt(
                      jwt ->
                          jwt.jwtAuthenticationConverter(
                              token ->
                                  new JwtAuthenticationToken(
                                      token, authoritiesConverter.convert(token)))));
    } else {
      http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .csrf(AbstractHttpConfigurer::disable);
    }
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    List<String> allowedOrigins = Arrays.stream(frontEndUrl.split(",")).map(String::trim).toList();
    log.info("allowedOrigins {}", allowedOrigins);

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Cache-Control", "Content-Type", "X-JWT-Assertion"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    log.info("tokenIssuerUrl {}", tokenIssuerUrl);
    log.info("jwkSetUri {}", jwkSetUri);
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }

  @Bean
  GrantedAuthorityDefaults grantedAuthorityDefaults() {
    return new GrantedAuthorityDefaults("");
  }
}
