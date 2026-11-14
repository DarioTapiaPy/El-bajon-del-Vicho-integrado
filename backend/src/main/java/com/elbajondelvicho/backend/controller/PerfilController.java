
package com.elbajondelvicho.backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class PerfilController {

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public Map<String, Object> obtenerPerfil(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Map.of(
                "email", jwt.getSubject(),
                "userId", jwt.getClaim("userId"),
                "roles", jwt.getClaimAsStringList("roles"),
                "mensaje", "Token JWT validado correctamente"
        );
    }
}
