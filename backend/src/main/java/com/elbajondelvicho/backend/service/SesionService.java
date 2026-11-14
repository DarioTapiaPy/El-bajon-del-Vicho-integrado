
package com.elbajondelvicho.backend.service;

import com.elbajondelvicho.backend.entity.Sesion;
import com.elbajondelvicho.backend.entity.Usuario;
import com.elbajondelvicho.backend.repository.SesionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class SesionService {

    private static final int DIAS_DURACION = 7;

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final SesionRepository sesionRepository;

    public SesionService(SesionRepository sesionRepository) {
        this.sesionRepository = sesionRepository;
    }

    // Crear una nueva sesion
    public String crearSesion(Usuario usuario) {

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw sesionInvalida();
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);

        Sesion sesion = new Sesion();
        sesion.setUsuario(usuario);
        sesion.setTokenHash(calcularHash(token));
        sesion.setFechaExpiracion(
                ahora.plusDays(DIAS_DURACION)
        );

        sesionRepository.save(sesion);

        return token;
    }

    // Renovar una sesion existente
    public Renovacion renovarSesion(String tokenActual) {

        Sesion sesion = obtenerSesionValida(tokenActual);

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);

        // Invalidar el token anterior
        sesion.setFechaRevocacion(ahora);

        Usuario usuario = sesion.getUsuario();

        // Crear un nuevo token de renovacion
        String nuevoToken = crearSesion(usuario);

        return new Renovacion(usuario, nuevoToken);
    }

    // Revocar una sesion al cerrar sesion
    public void revocarSesion(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        sesionRepository.findByTokenHash(calcularHash(token))
                .ifPresent(sesion -> {

                    if (sesion.getFechaRevocacion() == null) {
                        sesion.setFechaRevocacion(
                                LocalDateTime.now(ZoneOffset.UTC)
                        );
                    }
                });
    }

    // Validar el token recibido
    private Sesion obtenerSesionValida(String token) {

        if (token == null || token.isBlank()) {
            throw sesionInvalida();
        }

        Sesion sesion = sesionRepository
                .findByTokenHash(calcularHash(token))
                .orElseThrow(this::sesionInvalida);

        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);

        if (sesion.getFechaRevocacion() != null
                || !sesion.getFechaExpiracion().isAfter(ahora)
                || !Boolean.TRUE.equals(
                sesion.getUsuario().getActivo()
        )) {
            throw sesionInvalida();
        }

        return sesion;
    }

    // Convertir token a hash SHA-256
    private String calcularHash(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 no disponible", e
            );
        }
    }

    private ResponseStatusException sesionInvalida() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Sesion invalida o expirada"
        );
    }

    public record Renovacion(
            Usuario usuario,
            String tokenRenovacion
    ) {}
}
