
package com.elbajondelvicho.reservas.dto;

import com.elbajondelvicho.reservas.entity.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservaResponse(
        Long id,
        Long usuarioId,
        String correo,
        Integer personas,
        LocalDate fecha,
        LocalTime hora,
        String sucursal,
        String estado,
        LocalDateTime fechaCreacion
) {

    public static ReservaResponse desdeEntidad(
            Reserva reserva
    ) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuarioId(),
                reserva.getCorreo(),
                reserva.getPersonas(),
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getSucursal(),
                reserva.getEstado(),
                reserva.getFechaCreacion()
        );
    }
}
