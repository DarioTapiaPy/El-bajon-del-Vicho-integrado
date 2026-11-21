# El Bajón del Vicho — Matriz de endpoints EP3

**Fecha de revisión:** 21 de noviembre de 2026.  
**Fuente:** controladores y configuración de Spring Security del ZIP `El-bajon-del-Vicho-integrado(5).zip`, con la integración posterior de tests y manejo de errores en pedidos.  
**Alcance:** describe los contratos y permisos existentes; **no modifica código de ADMIN**.

## Backend principal — puerto 8081

| Método | Endpoint | Acceso / propósito |
|---|---|---|
| GET | `/api/v1/health` | Público; comprobar disponibilidad |
| GET | `/api/v1/productos` | Público; listar catálogo |
| GET | `/api/v1/productos/{id}` | Público; consultar producto |
| POST | `/api/v1/auth/registro` | Público; registrar CLIENTE |
| POST | `/api/v1/auth/login` | Público; iniciar sesión |
| POST | `/api/v1/auth/refresh` | Público a nivel de regla HTTP; requiere cookie y cabecera de sesión válidas según el controlador |
| POST | `/api/v1/auth/logout` | Público a nivel de regla HTTP; requiere sesión válida según el controlador |
| GET | `/api/v1/auth/me` | Usuario autenticado; consultar perfil |
| POST | `/api/v1/productos` | ADMIN |
| PUT | `/api/v1/productos/{id}` | ADMIN |
| DELETE | `/api/v1/productos/{id}` | ADMIN |
| GET | `/api/v1/vendedor/verificar` | VENDEDOR o ADMIN |

Las rutas bajo `/api/v1/admin/**` están reservadas a ADMIN. Su funcionalidad específica no se modificó ni se volvió a validar en esta etapa. Swagger: `http://localhost:8081/swagger-ui/index.html`.

## Microservicio de pedidos — puerto 8083

| Método | Endpoint | CLIENTE | VENDEDOR | ADMIN | Respuesta esperada |
|---|---|---|---|---|---|
| POST | `/api/v1/pedidos` | Sí | No | Sí | 201 al crear; 400 ante datos inválidos |
| GET | `/api/v1/pedidos/mis-pedidos` | Sí | No | Sí | 200, historial del `userId` del JWT |
| GET | `/api/v1/pedidos` | No | Sí | Sí | 200, listado global |
| PATCH | `/api/v1/pedidos/{id}/estado` | No | Sí | Sí | 200; 400, 404 o 409 según el caso |

- Un usuario no autenticado debe recibir **401** en endpoints protegidos; uno autenticado sin permiso, **403**.
- La identidad del cliente se obtiene del **JWT**, no de un `usuarioId` del cuerpo o la URL.
- El servicio calcula precios y totales con información del catálogo, no con importes enviados por React.
- Estados permitidos: `CREADO → CONFIRMADO / CANCELADO`; `CONFIRMADO → EN_PREPARACION / CANCELADO`; `EN_PREPARACION → LISTO / CANCELADO`; `LISTO → ENTREGADO / CANCELADO`. `ENTREGADO` y `CANCELADO` son terminales.
- Por decisión de integración posterior al traspaso inicial, **VENDEDOR sí puede cambiar estados de pedidos**. No implica permiso para editar productos ni ADMIN.

## Microservicio de reservas — puerto 8082

| Método | Endpoint | Acceso / alcance | Respuesta esperada |
|---|---|---|---|
| POST | `/api/v1/reservas` | CLIENTE o ADMIN | 201 |
| GET | `/api/v1/reservas/mis-reservas` | CLIENTE o ADMIN; identidad del token | 200 |
| GET | `/api/v1/reservas` | ADMIN | 200 |
| GET | `/api/v1/reservas/{id}` | CLIENTE o ADMIN; el servicio controla propiedad | 200 / 404 |
| PUT | `/api/v1/reservas/{id}` | ADMIN | 200 |
| DELETE | `/api/v1/reservas/{id}` | ADMIN; cancelación lógica | 204 |

Swagger: `http://localhost:8082/swagger-ui/index.html`. El servicio usa su propia base de datos y comparte el secreto de verificación JWT en un mismo ambiente.

## Nota de alcance

La matriz documenta reglas actuales de los controladores; las respuestas de éxito y error dependen también de los DTO, validaciones y servicio. Las pruebas automatizadas pasaron el 21/11/2026, pero **no sustituyen una validación completa de extremo a extremo con MySQL**. La operación de despacho/retiro visual del frontend no equivale por sí sola a persistir la dirección o sucursal en la tabla `pedido`.
