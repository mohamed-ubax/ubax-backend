package com.africa.ubaxplatform.config;

import com.africa.ubaxplatform.common.util.KeycloakJwtRolesConverter;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
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
    "/auth/login",
    "/auth/logout",
    "/auth/forgot-password"
  };

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String tokenIssuerUrl;

  @Value("${ubax.endpoints.frontend}")
  private String frontEndUrl;

  @Value("${ubax.security.enabled:true}")
  private boolean securityEnabled;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CustomAuthenticationEntryPoint entryPoint,
      CustomAccessDenied accessDenied)
      throws Exception {
    if (securityEnabled) {
      DelegatingJwtGrantedAuthoritiesConverter authoritiesConverter =
          new DelegatingJwtGrantedAuthoritiesConverter(
              new JwtGrantedAuthoritiesConverter(), new KeycloakJwtRolesConverter());

      http.csrf(AbstractHttpConfigurer::disable)
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .exceptionHandling(
              ex ->
                  ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
          .authorizeHttpRequests(
              auth ->
                  auth.requestMatchers(
                          "/api-docs/**",
                          "/swagger-ui/**",
                          "/swagger-ui.html",
                          "/webjars/**",
                          "/v3/api-docs/**",
                          "/actuator/prometheus",
                          "/actuator/health/**",
                          "/actuator/info",
                          "/auth/login",
                          "/auth/logout",
                          "/auth/forgot-password")
                      .permitAll()
                      .anyRequest()
                      .authenticated())
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
    List<String> allowedOrigins =
        Arrays.stream(frontEndUrl.split(",")).map(String::trim).toList();
    log.info("allowedOrigins {}", allowedOrigins);

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(allowedOrigins);
    configuration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
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
    return JwtDecoders.fromIssuerLocation(tokenIssuerUrl);
  }

  @Bean
  GrantedAuthorityDefaults grantedAuthorityDefaults() {
    return new GrantedAuthorityDefaults("");
  }
}
