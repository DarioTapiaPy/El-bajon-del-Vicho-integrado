
package com.elbajondelvicho.reservas.service;

import com.elbajondelvicho.reservas.entity.Reserva;
import com.elbajondelvicho.reservas.repository.ReservaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ReservaService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Santiago");

    private static final LocalTime HORA_APERTURA =
            LocalTime.of(12, 0);

    private static final LocalTime HORA_CIERRE =
            LocalTime.of(23, 0);

    private final ReservaRepository reservaRepository;

    public ReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    // VALIDACIONES DE NEGOCIO
    private void validarReserva(
            Integer personas,
            LocalDate fecha,
            LocalTime hora,
            String sucursal
    ) {
        if (personas == null || personas < 1 || personas > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La reserva debe ser para 1 a 5 personas"
            );
        }

        if (fecha == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha es obligatoria"
            );
        }

        ZonedDateTime ahora = ZonedDateTime.now(ZONA_HORARIA);

        LocalDate hoy = ahora.toLocalDate();
        LocalDate fechaMaxima = hoy.plusDays(30);

        if (fecha.isBefore(hoy) || fecha.isAfter(fechaMaxima)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha debe estar dentro de los proximos 30 dias"
            );
        }

        if (hora == null ||
                hora.isBefore(HORA_APERTURA) ||
                hora.isAfter(HORA_CIERRE)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El horario permitido es de 12:00 a 23:00"
            );
        }

        // No permitir reservar un horario que ya paso
        if (fecha.equals(hoy) &&
                !hora.isAfter(ahora.toLocalTime())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No puedes reservar en un horario pasado"
            );
        }

        if (!"Sucursal Viña del Mar".equals(sucursal) &&
                !"Sucursal Puente Alto".equals(sucursal)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La sucursal seleccionada no es valida"
            );
        }
    }

    // CREAR RESERVA
    @Transactional
    public Reserva crearReserva(
            Long usuarioId,
            String correo,
            Integer personas,
            LocalDate fecha,
            LocalTime hora,
            String sucursal
    ) {
        if (usuarioId == null || usuarioId <= 0 ||
                correo == null || correo.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Identidad de usuario invalida"
            );
        }

        validarReserva(personas, fecha, hora, sucursal);

        Reserva reserva = new Reserva();

        reserva.setUsuarioId(usuarioId);
        reserva.setCorreo(
                correo.trim().toLowerCase(Locale.ROOT)
        );
        reserva.setPersonas(personas);
        reserva.setFecha(fecha);
        reserva.setHora(hora);
        reserva.setSucursal(sucursal);
        reserva.setEstado("CONFIRMADA");

        return reservaRepository.save(reserva);
    }

    // CONSULTAR RESERVAS DEL CLIENTE
    @Transactional(readOnly = true)
    public List<Reserva> listarMisReservas(Long usuarioId) {

        return reservaRepository
                .findByUsuarioIdOrderByFechaAscHoraAsc(usuarioId);
    }

    // CONSULTAR TODAS LAS RESERVAS
    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {

        return reservaRepository
                .findAllByOrderByFechaAscHoraAsc();
    }

    // BUSCAR RESERVA CON CONTROL DE PROPIEDAD
    @Transactional(readOnly = true)
    public Reserva buscarReserva(
            Long reservaId,
            Long usuarioId,
            boolean esAdmin
    ) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reserva no encontrada"
                ));

        if (!esAdmin &&
                !reserva.getUsuarioId().equals(usuarioId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reserva no encontrada"
            );
        }

        return reserva;
    }

    // ACTUALIZAR RESERVA
    @Transactional
    public Reserva actualizarReserva(
            Long reservaId,
            Integer personas,
            LocalDate fecha,
            LocalTime hora,
            String sucursal
    ) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reserva no encontrada"
                ));

        if ("CANCELADA".equals(reserva.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede modificar una reserva cancelada"
            );
        }

        validarReserva(personas, fecha, hora, sucursal);

        reserva.setPersonas(personas);
        reserva.setFecha(fecha);
        reserva.setHora(hora);
        reserva.setSucursal(sucursal);

        return reservaRepository.save(reserva);
    }

    // CANCELAR RESERVA SIN BORRAR SU HISTORIAL
    @Transactional
    public void cancelarReserva(Long reservaId) {

        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reserva no encontrada"
                ));

        reserva.setEstado("CANCELADA");

        reservaRepository.save(reserva);
    }
}
