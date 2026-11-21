
package com.elbajondelvicho.pedidos.controller;

import com.elbajondelvicho.pedidos.dto.LineaPedidoRequest;
import com.elbajondelvicho.pedidos.entity.EstadoPedido;
import com.elbajondelvicho.pedidos.entity.Pedido;
import com.elbajondelvicho.pedidos.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Tag(name = "Pedidos", description = "Creacion, historial y gestion de estados de pedidos. La autenticacion se realiza mediante JWT Bearer.")
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // CREAR PEDIDO
    @Operation(summary = "Crear pedido", description = "CLIENTE o ADMIN. Obtiene el usuario del JWT y calcula los totales con los datos del catalogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado"),
            @ApiResponse(responseCode = "400", description = "Datos del pedido invalidos"),
            @ApiResponse(responseCode = "401", description = "Sin JWT valido"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado")
    })
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
                request == null ? null : request.items()
        );

        return convertir(pedido);
    }

    // CONSULTAR PEDIDOS DEL CLIENTE AUTENTICADO
    @Operation(summary = "Consultar mis pedidos", description = "CLIENTE o ADMIN. Solo consulta los pedidos del usuario identificado por el JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial del usuario"),
            @ApiResponse(responseCode = "401", description = "Sin JWT valido"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado")
    })
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

    // CONSULTAR TODOS LOS PEDIDOS
    // ADMIN Y VENDEDOR
    @Operation(summary = "Consultar todos los pedidos", description = "VENDEDOR o ADMIN. Lista los pedidos con sus detalles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado general"),
            @ApiResponse(responseCode = "401", description = "Sin JWT valido"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public List<PedidoResponse> listarTodos() {

        return pedidoService
                .obtenerTodosLosPedidos()
                .stream()
                .map(this::convertir)
                .toList();
    }

    // ACTUALIZAR ESTADO DE UN PEDIDO
    // ADMIN Y VENDEDOR
    @Operation(summary = "Actualizar estado de un pedido", description = "VENDEDOR o ADMIN. Valida las transiciones de estado permitidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Identificador o estado invalido"),
            @ApiResponse(responseCode = "401", description = "Sin JWT valido"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente"),
            @ApiResponse(responseCode = "409", description = "Transicion de estado no permitida")
    })
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public PedidoResponse actualizarEstado(
            @Parameter(description = "Identificador numerico del pedido", example = "2")
            @PathVariable Long id,
            @RequestBody ActualizarEstadoRequest request
    ) {
        EstadoPedido nuevoEstado =
                request == null ? null : request.estado();

        Pedido pedido = pedidoService.actualizarEstado(
                id,
                nuevoEstado
        );

        return convertir(pedido);
    }

    // OBTENER ID DEL USUARIO DESDE EL JWT
    private Long obtenerUsuarioId(Jwt jwt) {
        Object valor = jwt.getClaim("userId");

        if (!(valor instanceof Number numero)
                || numero.longValue() <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "JWT sin usuario valido"
            );
        }

        return numero.longValue();
    }

    // CONVERTIR ENTIDAD PEDIDO A PEDIDORESPONSE
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

    // DTO PARA CREAR PEDIDOS
    public record PedidoRequest(
            List<LineaPedidoRequest> items
    ) {}

    // DTO PARA ACTUALIZAR ESTADO
    public record ActualizarEstadoRequest(
            EstadoPedido estado
    ) {}

    // DTO PARA LOS DETALLES DEL PEDIDO
    public record DetalleResponse(
            String productoId,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}

    // DTO DE RESPUESTA DEL PEDIDO
    public record PedidoResponse(
            Long id,
            Long usuarioId,
            String estado,
            BigDecimal total,
            LocalDateTime fechaCreacion,
            List<DetalleResponse> detalles
    ) {}
}
