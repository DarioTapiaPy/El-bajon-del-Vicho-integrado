
package com.elbajondelvicho.reservas.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
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
                // API REST autenticada con Bearer JWT
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Documentacion Swagger
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Crear reservas: CLIENTE o ADMIN
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/reservas"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // Consultar reservas propias
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/reservas/mis-reservas"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // Consultar todas: solo ADMIN
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/reservas"
                        ).hasRole("ADMIN")

                        // Consultar una reserva concreta
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/reservas/{id}"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        // Actualizar: solo ADMIN
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/reservas/{id}"
                        ).hasRole("ADMIN")

                        // Cancelar: solo ADMIN
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/reservas/{id}"
                        ).hasRole("ADMIN")

                        // Bloquear cualquier ruta no definida
                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtConverter)
                        )
                );

        return http.build();
    }
}
