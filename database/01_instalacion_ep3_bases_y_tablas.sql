-- EP3 El Bajon del Vicho | Instalacion de tablas para un ENTORNO NUEVO
-- Fecha: 2026-11-21. Ejecutar como administrador MySQL 8.4, previo respaldo.
-- NO contiene DROP ni reemplaza/migra tablas ya existentes.
-- NO crea las cuentas SQL: ver 02_usuarios_y_permisos_EJEMPLO.sql.
-- No insertar contrasenas ni secretos en Git.
-- Para una base con estructura previa, evaluar migracion manual antes de usar.

CREATE DATABASE IF NOT EXISTS el_bajon_del_vicho_desarrollo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS el_bajon_del_vicho_reservas
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS el_bajon_del_vicho_pedidos
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1. Backend principal: catalogo, usuarios y sesiones.
USE el_bajon_del_vicho_desarrollo;

CREATE TABLE IF NOT EXISTS `categoria` (
                                           `id` bigint NOT NULL AUTO_INCREMENT,
                                           `codigo` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
    `nombre` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `codigo` (`codigo`)
    ) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `producto` (
                                          `id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
    `nombre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
    `descripcion` text COLLATE utf8mb4_unicode_ci NOT NULL,
    `precio` decimal(10,2) NOT NULL,
    `imagen` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `stock` tinyint(1) NOT NULL DEFAULT '1',
    `categoria_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_producto_categoria` (`categoria_id`),
    CONSTRAINT `fk_producto_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categoria` (`id`),
    CONSTRAINT `chk_producto_precio` CHECK ((`precio` >= 0))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `categoria` VALUES (1,'hamburguesas','Hamburguesas'),(2,'acompanamientos','Acompañamientos'),(3,'bebidas','Bebidas');

INSERT IGNORE INTO `producto` VALUES ('prod-1','COMBO CLASICO','La opción tradicional que nunca falla. Hamburguesa de carne de res con queso cheddar, lechuga fresca, tomate, cebolla morada y aderezos clásicos. Incluye porción de papas fritas y una lata de Coca-Cola.',8990.00,'/assets/combo_clasico.webp',1,1),('prod-10','Papas Fritas Vichonas','Papas fritas de corte ondulado cubiertas con salsa de queso fundido, carne desmechada, tocino, crema ácida y un toque de ciboulette fresco. Una opción contundente.',7990.00,'/assets/papas_clasicas.webp',1,2),('prod-11','Agua Individual','Botella de agua purificada sin gas por unidad. La opción más ligera, clásica y saludable para mantenerte hidratado.',1200.00,'/assets/agua.webp',1,3),('prod-12','Jugo Individual','Jugo néctar en formato de 400 ml por unidad. Una alternativa dulce y refrescante para acompañar tus comidas. Sabores a elección según disponibilidad.',1500.00,'/assets/jugo_400ml.webp',1,3),('prod-13','Bebida en Lata','Bebida en lata de 350 cc por unidad, servida bien fría. Elige tu sabor favorito entre nuestras distintas opciones clásicas, saborizadas o en su versión sin azúcar.',1500.00,'/assets/bebidas_350cc.webp',1,3),('prod-2','COMBO GORILLA GLUE','Una opción intensa y crujiente. Hamburguesa de carne de res con queso cheddar derretido, tiras de tocino, pepinillos, aros de cebolla empanizados y salsa BBQ. Incluye porción de papas fritas y una lata de Coca-Cola.',11990.00,'/assets/combo_gorilla_glue.webp',1,1),('prod-3','COMBO HAZE','Una combinación de sabores dulces y salados. Hamburguesa de carne de res con queso cheddar, cebolla caramelizada, pepinillos y una cubierta de mermelada de tocino o relish. Incluye porción de papas fritas y una lata de Coca-Cola.',11990.00,'/assets/combo_haze.webp',1,1),('prod-4','COMBO PURPLE HAZE','El combo más completo. Hamburguesa de carne desmechada con abundante guacamole, rodajas de tomate y queso. Acompañada de papas fritas, crujientes bocados de pollo frito, salsa para untar y una lata de Coca-Cola.',13990.00,'/assets/combo_purple_haze.webp',1,1),('prod-5','Papas Fritas Individuales','Porción individual de papas fritas de corte ondulado, presentadas en bolsa de papel. El tamaño justo para calmar el antojo.',1990.00,'/assets/papas_individuales.webp',1,2),('prod-6','Nuggets de Pollo','Clásicos bocados de pollo con un exterior crujiente y dorado. Ideales para compartir o como el acompañamiento perfecto. Porción de 5 unidades.',2990.00,'/assets/nuggets_pollo.webp',1,2),('prod-7','Aros de Cebolla','Aros de cebolla gruesos con un empanizado crujiente y dorado. Un acompañamiento clásico con un suave contraste dulce y salado.',3490.00,'/assets/aros_cebolla.webp',1,2),('prod-8','Mozarella Fingers','Bastones de queso mozzarella empanizados y fritos hasta alcanzar un dorado crujiente por fuera, con queso suave y fundido por dentro. Porción de 6 unidades.',4490.00,'/assets/mozarella_fingers.webp',1,2),('prod-9','Papas Fritas Grandes','Porción familiar de papas fritas de corte ondulado, servidas calientes y con el punto exacto de sal. Especial para compartir.',4990.00,'/assets/papas_grandes.webp',1,2);


-- Usuarios registrados mediante endpoint de registro: passwords bcrypt, NUNCA plaintext.
CREATE TABLE IF NOT EXISTS usuario (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       nombre VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
    activo TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_email (email),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN','VENDEDOR','CLIENTE'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sesion (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      usuario_id BIGINT NOT NULL,
                                      token_hash VARCHAR(64) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_expiracion DATETIME(6) NOT NULL,
    fecha_revocacion DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sesion_token_hash (token_hash),
    KEY idx_sesion_usuario_id (usuario_id),
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario(id) ON UPDATE RESTRICT ON DELETE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Reservas en base independiente (sin FK de usuario entre esquemas).
USE el_bajon_del_vicho_reservas;
CREATE TABLE IF NOT EXISTS reserva (
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


-- 3. Pedidos y detalle en base independiente (sin FK de usuario al principal).
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


-- 4. Verificacion NO destructiva.
SHOW TABLES FROM el_bajon_del_vicho_desarrollo;
SHOW TABLES FROM el_bajon_del_vicho_reservas;
SHOW TABLES FROM el_bajon_del_vicho_pedidos;
SELECT COUNT(*) AS categorias FROM el_bajon_del_vicho_desarrollo.categoria;
SELECT COUNT(*) AS productos FROM el_bajon_del_vicho_desarrollo.producto;
-- En un entorno NUEVO se esperan 3 categorias y 13 productos.
