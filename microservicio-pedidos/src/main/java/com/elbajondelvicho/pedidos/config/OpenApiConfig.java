package com.elbajondelvicho.pedidos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion Swagger/OpenAPI para el microservicio de pedidos.
 * El boton Authorize de Swagger recibe el JWT de acceso emitido por el backend 8081.
 * No pegar aqui el valor de JWT_SECRET ni contrasenas reales.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "El Bajon del Vicho - API de Pedidos",
                version = "v1",
                description = "Microservicio de pedidos (puerto 8083). Creacion e historial para CLIENTE; consulta global y actualizacion de estados para VENDEDOR. ADMIN conserva los permisos vigentes."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Pegar el token de acceso JWT obtenido al iniciar sesion en el backend principal (:8081). No ingresar el secreto JWT_SECRET."
)
public class OpenApiConfig {
}
