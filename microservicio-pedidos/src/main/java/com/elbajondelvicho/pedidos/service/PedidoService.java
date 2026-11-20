
package com.elbajondelvicho.pedidos.service;

import com.elbajondelvicho.pedidos.client.CatalogoClient;
import com.elbajondelvicho.pedidos.dto.LineaPedidoRequest;
import com.elbajondelvicho.pedidos.dto.ProductoCatalogo;
import com.elbajondelvicho.pedidos.entity.DetallePedido;
import com.elbajondelvicho.pedidos.entity.EstadoPedido;
import com.elbajondelvicho.pedidos.entity.Pedido;
import com.elbajondelvicho.pedidos.repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PedidoService {

    private static final BigDecimal TOTAL_MAXIMO =
            new BigDecimal("9999999999.99");

    private final PedidoRepository pedidoRepository;
    private final CatalogoClient catalogoClient;

    public PedidoService(
            PedidoRepository pedidoRepository,
            CatalogoClient catalogoClient
    ) {
        this.pedidoRepository = pedidoRepository;
        this.catalogoClient = catalogoClient;
    }

    // CREAR PEDIDO
    @Transactional
    public Pedido crearPedido(
            Long usuarioId,
            List<LineaPedidoRequest> items
    ) {
        validarSolicitud(usuarioId, items);

        // Consultar precios y disponibilidad reales
        ProductoCatalogo[] catalogo =
                catalogoClient.obtenerProductos();

        Map<String, ProductoCatalogo> productos =
                new HashMap<>();

        for (ProductoCatalogo producto : catalogo) {

            if (producto == null
                    || producto.id() == null
                    || producto.id().isBlank()) {

                throw new IllegalStateException(
                        "El catalogo contiene datos invalidos"
                );
            }

            if (productos.putIfAbsent(
                    producto.id(), producto) != null) {

                throw new IllegalStateException(
                        "El catalogo contiene productos duplicados"
                );
            }
        }

        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId);

        for (LineaPedidoRequest item : items) {

            ProductoCatalogo producto =
                    productos.get(item.productoId());

            if (producto == null) {
                throw new IllegalArgumentException(
                        "El producto solicitado no existe"
                );
            }

            if (!Boolean.TRUE.equals(producto.stock())) {
                throw new IllegalArgumentException(
                        "El producto no esta disponible: "
                                + producto.id()
                );
            }

            validarDatosCatalogo(producto);

            DetallePedido detalle = new DetallePedido(
                    producto.id(),
                    producto.name(),
                    item.cantidad(),
                    producto.price()
            );

            pedido.agregarDetalle(detalle);
        }

        pedido.calcularTotal();

        if (pedido.getTotal().compareTo(TOTAL_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "El total del pedido excede el limite permitido"
            );
        }

        // Guarda el pedido y sus detalles
        return pedidoRepository.saveAndFlush(pedido);
    }

    // CONSULTAR PEDIDOS DEL CLIENTE
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosDeUsuario(
            Long usuarioId
    ) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "Usuario invalido"
            );
        }

        return pedidoRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(
                        usuarioId
                );
    }

    // CONSULTAR TODOS LOS PEDIDOS
    // Usado por ADMIN y VENDEDOR
    @Transactional(readOnly = true)
    public List<Pedido> obtenerTodosLosPedidos() {

        return pedidoRepository
                .findAllByOrderByFechaCreacionDesc();
    }

    // ACTUALIZAR ESTADO DE UN PEDIDO
    @Transactional
    public Pedido actualizarEstado(
            Long pedidoId,
            EstadoPedido nuevoEstado
    ) {

        // Validar ID
        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de pedido invalido"
            );
        }

        // Validar nuevo estado
        if (nuevoEstado == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nuevo estado es obligatorio"
            );
        }

        // Buscar pedido junto con sus detalles
        // Evita LazyInitializationException al construir la respuesta
        Pedido pedido = pedidoRepository
                .findConDetallesById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pedido no existe"
                ));

        EstadoPedido estadoActual = pedido.getEstado();

        // Si ya tiene el estado solicitado, no modificar
        if (estadoActual == nuevoEstado) {
            return pedido;
        }

        // Validar las transiciones permitidas
        boolean transicionValida = switch (estadoActual) {

            case CREADO ->
                    nuevoEstado == EstadoPedido.CONFIRMADO
                            || nuevoEstado == EstadoPedido.CANCELADO;

            case CONFIRMADO ->
                    nuevoEstado == EstadoPedido.EN_PREPARACION
                            || nuevoEstado == EstadoPedido.CANCELADO;

            case EN_PREPARACION ->
                    nuevoEstado == EstadoPedido.LISTO
                            || nuevoEstado == EstadoPedido.CANCELADO;

            case LISTO ->
                    nuevoEstado == EstadoPedido.ENTREGADO
                            || nuevoEstado == EstadoPedido.CANCELADO;

            case ENTREGADO, CANCELADO -> false;
        };

        // Rechazar transiciones inválidas
        if (!transicionValida) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se permite cambiar de "
                            + estadoActual + " a " + nuevoEstado
            );
        }

        // Actualizar estado
        pedido.setEstado(nuevoEstado);

        // Guardar el nuevo estado en MySQL
        return pedidoRepository.saveAndFlush(pedido);
    }

    // VALIDAR SOLICITUD DE COMPRA
    private void validarSolicitud(
            Long usuarioId,
            List<LineaPedidoRequest> items
    ) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "Usuario invalido"
            );
        }

        if (items == null
                || items.isEmpty()
                || items.size() > 20) {

            throw new IllegalArgumentException(
                    "El pedido debe tener entre 1 y 20 productos"
            );
        }

        Set<String> ids = new HashSet<>();

        for (LineaPedidoRequest item : items) {

            if (item == null
                    || item.productoId() == null
                    || item.productoId().isBlank()
                    || item.productoId().length() > 40
                    || item.cantidad() == null
                    || item.cantidad() < 1
                    || item.cantidad() > 50) {

                throw new IllegalArgumentException(
                        "Producto o cantidad invalida"
                );
            }

            if (!ids.add(item.productoId())) {
                throw new IllegalArgumentException(
                        "Hay productos duplicados en el pedido"
                );
            }
        }
    }

    // VALIDAR INFORMACION DEL CATALOGO
    private void validarDatosCatalogo(
            ProductoCatalogo producto
    ) {
        if (producto.name() == null
                || producto.name().isBlank()
                || producto.name().length() > 150
                || producto.price() == null
                || producto.price().signum() < 0
                || producto.price().scale() > 2) {

            throw new IllegalStateException(
                    "El catalogo contiene datos de producto invalidos"
            );
        }
    }
}
