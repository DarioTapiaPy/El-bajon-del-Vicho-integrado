package com.elbajondelvicho.pedidos.dto;

import java.math.BigDecimal;

public record ProductoCatalogo(
        String id,
        String name,
        BigDecimal price,
        Boolean stock
) {
}
