package com.africa.ubaxplatform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre HTTP de logging — visible sur Dozzle en dev. À retirer ou désactiver via logging.level en
 * production.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    long start = System.currentTimeMillis();
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String query = request.getQueryString();
    String ip = resolveClientIp(request);

    log.info("→ {} {}{} [{}]", method, uri, query != null ? "?" + query : "", ip);

    try {
      chain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - start;
      int status = response.getStatus();
      if (status >= 500) {
        log.error("← {} {} {} ({}ms)", status, method, uri, duration);
      } else if (status >= 400) {
        log.warn("← {} {} {} ({}ms)", status, method, uri, duration);
      } else {
        log.info("← {} {} {} ({}ms)", status, method, uri, duration);
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.contains("/actuator") || uri.contains("/swagger") || uri.contains("/api-docs");
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
