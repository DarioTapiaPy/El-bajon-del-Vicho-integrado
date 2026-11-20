package com.elbajondelvicho.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.CREADO;

    @Column(name = "total", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "fecha_creacion", nullable = false,
            updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public void agregarDetalle(DetallePedido detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El detalle no puede ser nulo"
            );
        }

        detalle.setPedido(this);
        detalles.add(detalle);
    }

    public void calcularTotal() {
        this.total = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<DetallePedido> getDetalles() {
        return List.copyOf(detalles);
    }
}
