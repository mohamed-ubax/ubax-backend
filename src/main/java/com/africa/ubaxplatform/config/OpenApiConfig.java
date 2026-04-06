package com.africa.ubaxplatform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "UBAX Platform API",
            version = "1.0",
            description = "API de la plateforme immobilière UBAX"),
    tags = {
      @Tag(
          name = "Authentication",
          description = "Connexion, déconnexion, mot de passe oublié (email)"),
      @Tag(
          name = "Mobile",
          description = "Inscription et authentification via téléphone (OTP SMS)"),
      @Tag(
          name = "Admin",
          description = "Gestion des utilisateurs et des rôles – accès ADMIN requis")
    })
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Insérer le token JWT obtenu via /auth/login")
public class OpenApiConfig {}
