package com.elbajondelvicho.pedidos.controller;

import com.elbajondelvicho.pedidos.dto.LineaPedidoRequest;
import com.elbajondelvicho.pedidos.entity.Pedido;
import com.elbajondelvicho.pedidos.service.PedidoService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Crear un pedido
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public PedidoResponse crear(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PedidoRequest request
    ) {
        Long usuarioId = obtenerUsuarioId(jwt);

        Pedido pedido = pedidoService.crearPedido(
                usuarioId,
                request.items()
        );

        return convertir(pedido);
    }

    // Consultar pedidos del cliente autenticado
    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public List<PedidoResponse> misPedidos(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = obtenerUsuarioId(jwt);

        return pedidoService
                .obtenerPedidosDeUsuario(usuarioId)
                .stream()
                .map(this::convertir)
                .toList();
    }

    // Consultar todos los pedidos: ADMIN y VENDEDOR
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public List<PedidoResponse> listarTodos() {

        return pedidoService
                .obtenerTodosLosPedidos()
                .stream()
                .map(this::convertir)
                .toList();
    }

    // Obtener ID del usuario desde el JWT
    private Long obtenerUsuarioId(Jwt jwt) {
        Object valor = jwt.getClaim("userId");

        if (!(valor instanceof Number numero)
                || numero.longValue() <= 0) {
            throw new IllegalArgumentException(
                    "JWT sin usuario valido"
            );
        }

        return numero.longValue();
    }

    // Convertir entidad Pedido a PedidoResponse
    private PedidoResponse convertir(Pedido pedido) {

        List<DetalleResponse> detalles = pedido
                .getDetalles()
                .stream()
                .map(detalle -> new DetalleResponse(
                        detalle.getProductoId(),
                        detalle.getNombreProducto(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getSubtotal()
                ))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getEstado().name(),
                pedido.getTotal(),
                pedido.getFechaCreacion(),
                detalles
        );
    }

    // DTO para crear pedidos
    public record PedidoRequest(
            List<LineaPedidoRequest> items
    ) {}

    // DTO para los detalles del pedido
    public record DetalleResponse(
            String productoId,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}

    // DTO de respuesta del pedido
    public record PedidoResponse(
            Long id,
            Long usuarioId,
            String estado,
            BigDecimal total,
            LocalDateTime fechaCreacion,
            List<DetalleResponse> detalles
    ) {}
}
