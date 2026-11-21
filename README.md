El Bajón del Vicho — EP3 · Guía de instalación e integración
Proyecto académico con React 19/Vite 8, Java 21/Spring Boot 4.1.2, Spring Security/JWT y MySQL. Integración realizada a partir del ZIP entregado el 21-11-2026; NO sustituye una ejecución real en Windows/MySQL.
Arquitectura
Servicio	Puerto	Base de datos	Responsabilidad
frontend/	5173	—	Catálogo, carrito, cliente, pedidos, reservas y paneles
backend/	8081	el_bajon_del_vicho_desarrollo	Autenticación JWT, refresh/logout, usuarios y catálogo
microservicio-reservas/	8082	el_bajon_del_vicho_reservas	Reservas independientes
microservicio-pedidos/	8083	el_bajon_del_vicho_pedidos	Pedidos, detalles, totales, historial y estados


Los servicios no comparten tablas. El backend principal emite JWT con userId, roles e iss; reservas y pedidos verifican la firma y emisor mediante el mismo JWT_SECRET (Base64 de al menos 32 bytes). Nunca subir secretos, tokens ni contraseñas al repositorio.

Vite redirige /api/ → 8081, /reservas-api/ → 8082 y /pedidos-api/ → 8083 (quitando los prefijos propios para los microservicios).
Instalación inicial (Windows + MySQL 8.x)
1. Instalar Java 21, Node.js compatible con Vite 8 (recomendado Node 26), npm, Git, MySQL 8.x. Abrir IntelliJ para los tres servicios Java.
2. Hacer respaldo antes de modificar esquemas existentes. En una instalación NUEVA, abrir database/01_instalacion_ep3_bases_y_tablas.sql en MySQL Workbench como administrador y revisarlo antes de ejecutarlo. Crea tres bases y siete tablas, inserta las tres categorías y 13 productos iniciales con INSERT IGNORE. CREATE TABLE IF NOT EXISTS no migra estructuras existentes: para bases con tablas previas, revisar columna por columna y migrar según corresponda.
3. Revisar database/02_usuarios_y_permisos_EJEMPLO.sql. Es una plantilla comentada, no ejecutable sin editar. Con claves robustas elegidas localmente, crear vicho_app, reservas_app y pedidos_app, y darles únicamente SELECT/INSERT/UPDATE/DELETE en sus bases respectivas. Si ya existen y funcionan, no cambiar sus claves.
4. Crear en IntelliJ las configuraciones de ejecución y establecer variables de entorno locales:
   Módulo	Variables necesarias
   backend/	DB_PASSWORD, JWT_SECRET
   microservicio-reservas/	RESERVAS_DB_PASSWORD, JWT_SECRET
   microservicio-pedidos/	PEDIDOS_DB_PASSWORD, JWT_SECRET


   Las tres aplicaciones usan el mismo JWT_SECRET. Opcionales: COOKIE_SECURE=false solo en HTTP local, CATALOGO_BASE_URL=http://localhost:8081 para pedidos. Los archivos application.properties no contienen contraseñas.
5. Levantar en orden el backend principal, reservas y pedidos (desde la carpeta respectiva), por IntelliJ o mediante ./mvnw.cmd spring-boot:run en PowerShell con las variables correctamente cargadas.
6. Levantar React desde frontend/:
   npm.cmd ci
   npm.cmd run dev
   Abrir http://localhost:5173.
   Verificación de servicios
- http://localhost:8081/api/v1/productos → catálogo JSON.
- http://localhost:8081/swagger-ui/index.html → OpenAPI principal.
- http://localhost:8082/swagger-ui/index.html → OpenAPI reservas.
- Pedidos http://localhost:8083/api/v1/pedidos: 401 sin Bearer, 403 para CLIENTE en GET global, 200 para VENDEDOR con JWT.
- Registro de CLIENTE → login → reserva → carrito/confirmación → «Mis pedidos» → VENDEDOR actualiza el estado → CLIENTE ve el cambio después de F5.
  Seguridad y permisos de pedidos
  Método	Ruta del servicio pedidos (8083)	Roles
  POST	/api/v1/pedidos	CLIENTE, ADMIN
  GET	/api/v1/pedidos/mis-pedidos	CLIENTE, ADMIN; usa userId del JWT
  GET	/api/v1/pedidos	VENDEDOR, ADMIN
  PATCH	/api/v1/pedidos/{id}/estado	VENDEDOR, ADMIN


VENDEDOR puede actualizar estados de pedidos, según la decisión posterior del equipo. Esta guía no cambia la gestión ADMIN ni le agrega funcionalidades. Estados: CREADO → CONFIRMADO → EN_PREPARACION → LISTO → ENTREGADO; se permite CANCELADO desde cualquier estado no terminal. Todos los saltos fuera de reglas devuelven 409.

La orden registra usuarioId, líneas, precios del catálogo, total, fecha y estado. Limitación actual: dirección de despacho y sucursal de retiro se muestran en React, pero no se guardan en el pedido; no debe afirmarse lo contrario.
Pruebas automatizadas
En PowerShell (cada comando dentro de su módulo):
cd microservicio-pedidos
.\mvnw.cmd clean test
cd ..\microservicio-reservas
.\mvnw.cmd clean test
cd ..\backend
.\mvnw.cmd clean test
cd ..\frontend
npm.cmd ci
$env:CHROME_BIN = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
npm.cmd test -- --single-run
npm.cmd run build
npm.cmd run lint
El test de contexto de pedidos ahora usa H2 solo en perfil test; producción sigue configurada con MySQL/`ddl-auto=none`. Los tests web de JWT en PedidoControllerSecurityTest utilizan tokens simulados: no prueban la criptografía real. Una suite verde en un módulo no garantiza que todos los módulos pasen.

También se incluye scripts/validar_ep3.cmd, ejecutable desde CMD o PowerShell con .\scripts\validar_ep3.cmd, que detiene la secuencia si alguna verificación falla.

Consultar docs/PLAN_VALIDACION_EP3.md para el guion de evidencias y docs/MATRIZ_ENDPOINTS.md para reservas y pedidos.
Cambios de esta propuesta sobre el ZIP
- Manejo localizado HTTP 400 para validaciones de creación de pedidos, con campo JSON detail consumido por React; 401 si userId JWT no es válido.
- Nuevos casos de prueba de carrito, catálogo, totales, permisos y errores; aislamiento de prueba de contexto con H2.
- Scripts SQL iniciales para tres bases y cuentas en plantilla comentada.
- Documentación EP3 y plan de pruebas; no se modificó AdminSection.jsx ni las reglas de autorización ADMIN.
  Importante antes de integrar al proyecto del equipo
  Esta carpeta proviene del ZIP entregado, no de un git pull sobre la rama actual. Comparar antes de reemplazar archivos. Crear una rama, revisar diferencias, ejecutar pruebas y recién entonces abrir Pull Request. No ejecutar DROP TABLE, no guardar secretos en Git y no sobrescribir cambios posteriores del equipo.
  Para completar la evaluación: contrastar con la rúbrica oficial, ejecutar los tests en el computador del equipo, probar la instalación limpia y documentar los resultados reales. No se incluye una boleta/factura fiscal ni gestión completa del despacho por falta de especificaciones funcio
