package com.elbajondelvicho.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "producto_id", nullable = false,
            length = 40)
    private String productoId;

    @Column(name = "nombre_producto", nullable = false,
            length = 150)
    private String nombreProducto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal subtotal;

    public DetallePedido() {
    }

    public DetallePedido(
            String productoId,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitario
    ) {
        if (productoId == null || productoId.isBlank()
                || nombreProducto == null
                || nombreProducto.isBlank()
                || cantidad == null || cantidad <= 0
                || precioUnitario == null
                || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException(
                    "Datos de detalle invalidos"
            );
        }

        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;

        this.subtotal = precioUnitario.multiply(
                BigDecimal.valueOf(cantidad)
        );
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public String getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
