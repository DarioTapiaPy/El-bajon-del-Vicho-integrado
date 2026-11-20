package com.elbajondelvicho.pedidos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Configuration
public class JwtConfig {

    private static final String EMISOR =
            "el-bajon-del-vicho";

    private static final Set<String> ROLES_VALIDOS =
            Set.of("ADMIN", "VENDEDOR", "CLIENTE");

    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}") String secret
    ) {
        byte[] bytes = Base64.getDecoder().decode(secret);

        if (bytes.length < 32) {
            throw new IllegalArgumentException(
                    "La clave JWT debe tener al menos 32 bytes"
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(key)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        OAuth2TokenValidator<Jwt> validarClaims = jwt -> {

            String correo = jwt.getSubject();

            List<String> roles =
                    jwt.getClaimAsStringList("roles");

            Object usuarioId = jwt.getClaim("userId");

            boolean correoValido =
                    correo != null && !correo.isBlank();

            boolean rolValido =
                    roles != null
                            && roles.size() == 1
                            && ROLES_VALIDOS.contains(roles.get(0));

            boolean idValido =
                    usuarioId instanceof Number
                            && ((Number) usuarioId).longValue() > 0;

            if (!correoValido || !rolValido || !idValido) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error(
                                "invalid_token",
                                "El JWT no contiene una identidad valida",
                                null
                        )
                );
            }

            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(EMISOR),
                        validarClaims
                )
        );

        return decoder;
    }
}
