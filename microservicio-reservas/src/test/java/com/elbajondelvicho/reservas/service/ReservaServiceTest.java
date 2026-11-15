
package com.elbajondelvicho.reservas.service;

import com.elbajondelvicho.reservas.entity.Reserva;
import com.elbajondelvicho.reservas.repository.ReservaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReservaService reservaService;

    private LocalDate fechaValida() {
        return LocalDate.now(
                ZoneId.of("America/Santiago")
        ).plusDays(2);
    }

    @Test
    void crearReservaValida() {

        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Reserva reserva = reservaService.crearReserva(
                1L,
                "CLIENTE@EJEMPLO.COM",
                4,
                fechaValida(),
                LocalTime.of(20, 30),
                "Sucursal Viña del Mar"
        );

        assertNotNull(reserva);
        assertEquals(1L, reserva.getUsuarioId());
        assertEquals("cliente@ejemplo.com", reserva.getCorreo());
        assertEquals(4, reserva.getPersonas());
        assertEquals("CONFIRMADA", reserva.getEstado());

        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void rechazarReservaConMasDeCincoPersonas() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> reservaService.crearReserva(
                        1L,
                        "cliente@ejemplo.com",
                        6,
                        fechaValida(),
                        LocalTime.of(20, 30),
                        "Sucursal Viña del Mar"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(reservaRepository);
    }

    @Test
    void rechazarReservaConFechaPasada() {

        LocalDate ayer = LocalDate.now(
                ZoneId.of("America/Santiago")
        ).minusDays(1);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> reservaService.crearReserva(
                        1L,
                        "cliente@ejemplo.com",
                        2,
                        ayer,
                        LocalTime.of(20, 30),
                        "Sucursal Viña del Mar"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    @Test
    void rechazarReservaConSucursalInvalida() {

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> reservaService.crearReserva(
                        1L,
                        "cliente@ejemplo.com",
                        2,
                        fechaValida(),
                        LocalTime.of(20, 30),
                        "Sucursal Inventada"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );
    }

    @Test
    void clienteNoPuedeVerReservaDeOtroUsuario() {

        Reserva reserva = new Reserva();
        reserva.setUsuarioId(10L);

        when(reservaRepository.findById(1L))
                .thenReturn(Optional.of(reserva));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> reservaService.buscarReserva(
                        1L,
                        99L,
                        false
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );
    }

    @Test
    void cancelarReservaConservaElRegistro() {

        Reserva reserva = new Reserva();

        reserva.setUsuarioId(1L);
        reserva.setEstado("CONFIRMADA");

        when(reservaRepository.findById(1L))
                .thenReturn(Optional.of(reserva));

        reservaService.cancelarReserva(1L);

        assertEquals(
                "CANCELADA",
                reserva.getEstado()
        );

        verify(reservaRepository).save(reserva);

        // No se elimina fisicamente la reserva
        verify(reservaRepository, never())
                .deleteById(any());
    }
}
