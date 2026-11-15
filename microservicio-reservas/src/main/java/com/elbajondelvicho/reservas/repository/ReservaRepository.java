
package com.elbajondelvicho.reservas.repository;

import com.elbajondelvicho.reservas.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    // Consultar reservas de un usuario
    List<Reserva> findByUsuarioIdOrderByFechaAscHoraAsc(
            Long usuarioId
    );

    // Consultar todas las reservas ordenadas
    List<Reserva> findAllByOrderByFechaAscHoraAsc();

}
