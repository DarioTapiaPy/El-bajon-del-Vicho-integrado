
package com.elbajondelvicho.backend.controller;

import com.elbajondelvicho.backend.dto.*;
import com.elbajondelvicho.backend.entity.Usuario;
import com.elbajondelvicho.backend.repository.UsuarioRepository;
import com.elbajondelvicho.backend.service.JwtService;
import com.elbajondelvicho.backend.service.SesionService;
import com.elbajondelvicho.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_RENOVACION = "vicho_refresh";
    private static final String RUTA_COOKIE = "/api/v1/auth";
    private static final Duration DURACION_RENOVACION =
            Duration.ofDays(7);

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SesionService sesionService;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(
            UsuarioService usuarioService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SesionService sesionService
    ) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sesionService = sesionService;
    }

    // REGISTRO PUBLICO
    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(
            @Valid @RequestBody RegistroRequest datos
    ) {
        UsuarioDTO usuario = usuarioService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }

    // LOGIN: JWT + COOKIE DE RENOVACION
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest datos
    ) {
        String email = datos.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(this::credencialesInvalidas);

        if (!Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(
                datos.password(),
                usuario.getPassword()
        )) {
            throw credencialesInvalidas();
        }

        String accessToken = jwtService.generarToken(usuario);
        String refreshToken = sesionService.crearSesion(usuario);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        crearCookie(
                                refreshToken,
                                DURACION_RENOVACION
                        ).toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(crearRespuesta(usuario, accessToken));
    }

    // REFRESH: RENOVAR JWT Y ROTAR TOKEN DE SESION
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(
                    name = COOKIE_RENOVACION,
                    required = false
            ) String refreshToken,
            @RequestHeader(
                    name = "X-Session-Request",
                    required = false
            ) String encabezado
    ) {
        verificarEncabezado(encabezado);

        SesionService.Renovacion renovacion =
                sesionService.renovarSesion(refreshToken);

        Usuario usuario = renovacion.usuario();

        String nuevoAccessToken =
                jwtService.generarToken(usuario);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        crearCookie(
                                renovacion.tokenRenovacion(),
                                DURACION_RENOVACION
                        ).toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(crearRespuesta(usuario, nuevoAccessToken));
    }

    // LOGOUT: REVOCAR SESION Y BORRAR COOKIE
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = COOKIE_RENOVACION,
                    required = false
            ) String refreshToken,
            @RequestHeader(
                    name = "X-Session-Request",
                    required = false
            ) String encabezado
    ) {
        verificarEncabezado(encabezado);

        sesionService.revocarSesion(refreshToken);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        crearCookie("", Duration.ZERO).toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    // CONSTRUIR RESPUESTA COMPATIBLE CON REACT
    private LoginResponse crearRespuesta(
            Usuario usuario,
            String accessToken
    ) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                3600,
                UsuarioDTO.desdeEntidad(usuario)
        );
    }

    // CONSTRUIR COOKIE HTTPONLY
    private ResponseCookie crearCookie(
            String token,
            Duration duracion
    ) {
        return ResponseCookie.from(
                        COOKIE_RENOVACION,
                        token
                )
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(RUTA_COOKIE)
                .maxAge(duracion)
                .build();
    }

    // PROTECCION PARA ENDPOINTS BASADOS EN COOKIE
    private void verificarEncabezado(String encabezado) {
        if (!"1".equals(encabezado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Encabezado de solicitud requerido"
            );
        }
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas"
        );
    }
}
