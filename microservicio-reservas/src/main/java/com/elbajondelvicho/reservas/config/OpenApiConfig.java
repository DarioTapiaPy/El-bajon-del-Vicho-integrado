
package com.elbajondelvicho.reservas.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "El Bajon del Vicho - API de Reservas",
                version = "1.0.0",
                description = """
                        Microservicio independiente de reservas.

                        Permite crear, consultar, actualizar
                        y cancelar reservas.

                        Utiliza autenticacion JWT y permisos
                        diferenciados para CLIENTE y ADMIN.
                        """
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT generado por el backend principal"
)
public class OpenApiConfig {
}
