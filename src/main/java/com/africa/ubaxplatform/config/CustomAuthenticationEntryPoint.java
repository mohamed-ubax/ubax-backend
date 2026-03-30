package com.africa.ubaxplatform.config;

import com.africa.ubaxplatform.common.response.CustomResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    CustomResponse errorResponse =
        CustomResponse.builder()
            .status("UNAUTHORIZED")
            .statusCode(401)
            .message("Veuillez fournir le token après authentification!")
            .build();

    new ObjectMapper().writeValue(response.getWriter(), errorResponse);
  }
}
