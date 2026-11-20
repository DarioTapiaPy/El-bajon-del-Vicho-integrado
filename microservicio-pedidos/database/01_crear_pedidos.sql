
-- Instalacion inicial para una base de datos nueva.
-- Ejecutar como administrador en MySQL Workbench.
-- No ejecutar sobre bases con datos sin respaldo.

CREATE DATABASE IF NOT EXISTS el_bajon_del_vicho_pedidos
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE el_bajon_del_vicho_pedidos;

CREATE TABLE IF NOT EXISTS pedido (
                                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                      usuario_id BIGINT NOT NULL,
                                      estado VARCHAR(20) NOT NULL DEFAULT 'CREADO',
    total DECIMAL(12,2) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,

    INDEX idx_pedido_usuario_fecha
(usuario_id, fecha_creacion),

    CONSTRAINT chk_pedido_total
    CHECK (total >= 0),

    CONSTRAINT chk_pedido_estado
    CHECK (estado IN (
           'CREADO',
           'CONFIRMADO',
           'EN_PREPARACION',
           'LISTO',
           'ENTREGADO',
           'CANCELADO'
                     ))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detalle_pedido (
                                              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                              pedido_id BIGINT NOT NULL,
                                              producto_id VARCHAR(40) NOT NULL,
    nombre_producto VARCHAR(150) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

    INDEX idx_detalle_pedido (pedido_id),

    CONSTRAINT fk_detalle_pedido
    FOREIGN KEY (pedido_id)
    REFERENCES pedido(id),

    CONSTRAINT chk_detalle_cantidad
    CHECK (cantidad BETWEEN 1 AND 50),

    CONSTRAINT chk_detalle_precio
    CHECK (precio_unitario >= 0 AND subtotal >= 0)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;
