
package com.elbajondelvicho.pedidos.repository;

import com.elbajondelvicho.pedidos.entity.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

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
}
