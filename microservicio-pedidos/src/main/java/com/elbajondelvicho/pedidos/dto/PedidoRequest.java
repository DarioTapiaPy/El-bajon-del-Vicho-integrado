package com.elbajondelvicho.pedidos.dto;

import java.util.List;

public record PedidoRequest(
        List<LineaPedidoRequest> items
) {
}