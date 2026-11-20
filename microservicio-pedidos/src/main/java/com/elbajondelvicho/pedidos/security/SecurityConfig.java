
package com.elbajondelvicho.pedidos.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtConverter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth

                        // Permitir errores internos de Spring
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Crear pedidos: CLIENTE o ADMIN
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/pedidos"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // Consultar pedidos propios
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/pedidos/mis-pedidos"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // Consultar todos los pedidos:
                        // ADMIN o VENDEDOR
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/pedidos"
                        ).hasAnyRole("ADMIN", "VENDEDOR")

                        // NUEVO: actualizar estado del pedido
                        // ADMIN o VENDEDOR
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/pedidos/*/estado"
                        ).hasAnyRole("ADMIN", "VENDEDOR")

                        // Bloquear cualquier otra ruta
                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtConverter
                                )
                        )
                );

        return http.build();
    }
}
