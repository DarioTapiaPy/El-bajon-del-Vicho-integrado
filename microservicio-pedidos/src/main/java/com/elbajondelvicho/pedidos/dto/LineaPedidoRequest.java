package com.elbajondelvicho.pedidos.dto;

public record LineaPedidoRequest(
        String productoId,
        Integer cantidad
) {
}
