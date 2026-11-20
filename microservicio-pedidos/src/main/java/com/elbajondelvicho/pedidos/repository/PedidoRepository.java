
package com.elbajondelvicho.pedidos.repository;

import com.elbajondelvicho.pedidos.entity.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    // Pedidos del cliente autenticado
    @EntityGraph(attributePaths = "detalles")
    List<Pedido> findByUsuarioIdOrderByFechaCreacionDesc(
            Long usuarioId
    );

    // Todos los pedidos para ADMIN y VENDEDOR
    @EntityGraph(attributePaths = "detalles")
    List<Pedido> findAllByOrderByFechaCreacionDesc();

    // NUEVO: buscar un pedido cargando sus detalles
    @EntityGraph(attributePaths = "detalles")
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findConDetallesById(@Param("id") Long id);
}
