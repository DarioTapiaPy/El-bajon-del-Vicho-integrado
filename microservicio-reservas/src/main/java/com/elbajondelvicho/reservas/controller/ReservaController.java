
package com.elbajondelvicho.reservas.controller;

import com.elbajondelvicho.reservas.dto.ReservaRequest;
import com.elbajondelvicho.reservas.dto.ReservaResponse;
import com.elbajondelvicho.reservas.entity.Reserva;
import com.elbajondelvicho.reservas.service.ReservaService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // 1. CREAR RESERVA
    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(
            @Valid @RequestBody ReservaRequest request,
            JwtAuthenticationToken authentication
    ) {

        // Identidad obtenida del JWT validado
        Long usuarioId = obtenerUsuarioId(authentication);
        String correo = authentication.getToken().getSubject();

        Reserva reserva = reservaService.crearReserva(
                usuarioId,
                correo,
                request.personas(),
                request.fecha(),
                request.hora(),
                request.sucursal()
        );

        URI ubicacion = URI.create(
                "/api/v1/reservas/" + reserva.getId()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(ReservaResponse.desdeEntidad(reserva));
    }

    // 2. CONSULTAR MIS RESERVAS
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaResponse>> misReservas(
            JwtAuthenticationToken authentication
    ) {

        Long usuarioId = obtenerUsuarioId(authentication);

        List<ReservaResponse> reservas = reservaService
                .listarMisReservas(usuarioId)
                .stream()
                .map(ReservaResponse::desdeEntidad)
                .toList();

        return ResponseEntity.ok(reservas);
    }

    // 3. CONSULTAR TODAS LAS RESERVAS (ADMIN)
    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodas() {

        List<ReservaResponse> reservas = reservaService
                .listarTodas()
                .stream()
                .map(ReservaResponse::desdeEntidad)
                .toList();

        return ResponseEntity.ok(reservas);
    }

    // 4. CONSULTAR RESERVA POR ID
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(
            @PathVariable Long id,
            JwtAuthenticationToken authentication
    ) {

        Long usuarioId = obtenerUsuarioId(authentication);
        boolean esAdmin = esAdministrador(authentication);

        Reserva reserva = reservaService.buscarReserva(
                id,
                usuarioId,
                esAdmin
        );

        return ResponseEntity.ok(
                ReservaResponse.desdeEntidad(reserva)
        );
    }

    // 5. ACTUALIZAR RESERVA (ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<ReservaResponse> actualizarReserva(
            @PathVariable Long id,
            @Valid @RequestBody ReservaRequest request
    ) {

        Reserva reserva = reservaService.actualizarReserva(
                id,
                request.personas(),
                request.fecha(),
                request.hora(),
                request.sucursal()
        );

        return ResponseEntity.ok(
                ReservaResponse.desdeEntidad(reserva)
        );
    }

    // 6. CANCELAR RESERVA (ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarReserva(
            @PathVariable Long id
    ) {

        reservaService.cancelarReserva(id);

        return ResponseEntity.noContent().build();
    }

    // OBTENER USUARIO AUTENTICADO DESDE JWT
    private Long obtenerUsuarioId(
            JwtAuthenticationToken authentication
    ) {

        Number usuarioId = authentication
                .getToken()
                .getClaim("userId");

        return usuarioId.longValue();
    }

    // VERIFICAR SI EL USUARIO ES ADMINISTRADOR
    private boolean esAdministrador(
            JwtAuthenticationToken authentication
    ) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_ADMIN")
                );
    }
}
