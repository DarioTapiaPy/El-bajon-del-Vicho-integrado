
-- EL BAJON DEL VICHO
-- Microservicio independiente de reservas
-- Migracion inicial: crear tabla reserva

USE el_bajon_del_vicho_reservas;

CREATE TABLE reserva (
                         id BIGINT NOT NULL AUTO_INCREMENT,

                         usuario_id BIGINT NOT NULL,

                         correo VARCHAR(150) NOT NULL,

                         personas TINYINT UNSIGNED NOT NULL,

                         fecha DATE NOT NULL,

                         hora TIME NOT NULL,

                         sucursal VARCHAR(40) NOT NULL,

                         estado VARCHAR(20) NOT NULL
                             DEFAULT 'CONFIRMADA',

                         fecha_creacion DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                         PRIMARY KEY (id),

                         INDEX idx_reserva_usuario_fecha (
        usuario_id,
        fecha
    ),

                         INDEX idx_reserva_sucursal_fecha (
        sucursal,
        fecha
    ),

                         CONSTRAINT chk_reserva_personas
                             CHECK (personas BETWEEN 1 AND 5),

                         CONSTRAINT chk_reserva_hora
                             CHECK (
                                 hora BETWEEN '12:00:00' AND '23:00:00'
                                 ),

                         CONSTRAINT chk_reserva_sucursal
                             CHECK (
                                 sucursal IN (
                                              'Sucursal Viña del Mar',
                                              'Sucursal Puente Alto'
                                     )
                                 ),

                         CONSTRAINT chk_reserva_estado
                             CHECK (
                                 estado IN (
                                            'CONFIRMADA',
                                            'CANCELADA'
                                     )
                                 )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
