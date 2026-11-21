# El Bajón del Vicho — Validación final EP3

**Fecha de evidencias:** 21 de noviembre de 2026.  
**Rama de trabajo indicada por Git:** `feat/cierre-ep3-jose`.  
**Criterio:** distinguir resultados realmente observados de revisiones pendientes. No modificar ADMIN en esta etapa.

## 1. Resultados comprobados en la máquina de José

| Comprobación | Comando / método | Resultado reportado |
|---|---|---|
| Microservicio de pedidos | `microservicio-pedidos`: `./mvnw.cmd clean test` | **37 tests**, 0 fallos, 0 errores, 0 omitidos; `BUILD SUCCESS` |
| Microservicio de reservas | `microservicio-reservas`: `./mvnw.cmd clean test` | **7 tests**, 0 fallos, 0 errores, 0 omitidos; `BUILD SUCCESS` |
| Backend principal | `backend`: `./mvnw.cmd clean test` | **1 test**, 0 fallos, 0 errores, 0 omitidos; `BUILD SUCCESS` tras configurar H2 en perfil test |
| Frontend React | `frontend`: `npm.cmd test -- --single-run` | **51/51 SUCCESS** en Chrome Headless |
| Build frontend | `npm.cmd run build` | Correcto; Vite **8.3.4**, `built in 213ms` |
| Lint frontend | `npm.cmd run lint` | **0 errores, 8 advertencias**; comando finalizado |
| Red local | `Test-NetConnection` puertos 3306, 8081, 8082, 8083, 5173 | **5/5 True** (puertos abiertos, no prueba funcional por sí sola) |
| Catálogo principal | `Invoke-RestMethod http://localhost:8081/api/v1/productos` | **13 productos** devueltos desde API/MySQL |
| Visualización de frontend | `http://localhost:5173` | Usuario confirmó productos, precios, imágenes y acentos correctos |

**Total de tests automatizados aprobados: 96/96** (37 + 7 + 1 + 51). Estas cifras son tests ejecutados, **no porcentaje de cobertura**.

**Cobertura reportada de React:** statements **41,62 %**, branches **36,08 %**, functions **41,70 %**, lines **42,74 %**.

### Advertencias de lint que no bloquearon build

- `AddressModal.jsx`: 1 (`set-state-in-effect`).
- `MisPedidos.jsx`: 1 (`refs`).
- `ReservationSection.jsx`: 1 (`set-state-in-effect`).
- `StaffCatalog.jsx`: 2 (`refs`, `set-state-in-effect`).
- `AdminSection.jsx`: 3 (`set-state-in-effect` y dependencia de hook).

No se modificó `AdminSection.jsx`. Estas advertencias son tareas de calidad opcionales, no errores de ejecución comprobados.

## 2. Resultado de pruebas funcionales ya realizadas anteriormente

Antes de la última integración se había verificado manualmente: CLIENTE crea y consulta sus pedidos, VENDEDOR consulta pedidos y cambia estados, y el CLIENTE ve el estado actualizado. **No se repitió ese flujo completo tras los últimos cambios**; registrar su validación histórica sin presentarla como una nueva prueba de extremo a extremo.

## 3. Pendientes antes de declarar EP3 totalmente validado

- [ ] Confirmar en un entorno de instalación limpia que los scripts SQL crean las **tres bases** y sus tablas, índices, cuentas/permisos sin modificar datos existentes. **No ejecutar scripts de instalación sobre bases con datos sin respaldo y revisión**.
- [ ] Verificar una solicitud inválida real en pedidos (por ejemplo cantidad cero): respuesta HTTP **400**, sin añadir un pedido; los tests automatizados ya cubren casos relacionados.
- [ ] Confirmar con evidencia de MySQL los datos de `pedido`, `detalle_pedido`, `reserva` y `sesion` si la rúbrica lo exige.
- [ ] Revisar la rúbrica para saber si se requiere boleta/factura, persistir dirección/sucursal de despacho/retiro, Swagger de pedidos, despliegue público y documentación institucional adicional.
- [ ] Revisar `git diff --cached` y `git diff`, comprobar que ADMIN sigue intacto; hacer commit/PR solo después de revisar los archivos definitivos.
- [ ] Si se modifica código tras estas evidencias, volver a ejecutar las suites afectadas.

## 4. Comandos para revalidar (PowerShell)

Ejecutar cada uno desde la carpeta indicada, sin incluir contraseñas ni JWT en los registros compartidos.

```powershell
cd C:\Users\josed\IdeaProjects\El-bajon-del-Vicho-integrado\microservicio-pedidos
.\mvnw.cmd clean test

cd ..\microservicio-reservas
.\mvnw.cmd clean test

cd ..\backend
.\mvnw.cmd clean test

cd ..\frontend
npm.cmd test -- --single-run
npm.cmd run build
npm.cmd run lint

cd ..
git branch --show-current
git status --short --untracked-files=all
git diff --stat
git diff --cached --stat
```

El perfil H2 en tests del backend y pedidos **no es la base real** utilizada por la aplicación en ejecución. No compartir `JWT_SECRET`, contraseñas MySQL, cookies ni tokens de sesión.

## 5. Evidencia recomendada para entrega

Guardar capturas de los resúmenes Maven (37, 7, 1), Karma (51/51 y porcentajes), Vite, lint (0 errores / 8 advertencias), catálogo real y `git status` final. Registrar también la fecha y el commit donde se hayan integrado estas modificaciones. La ausencia de capturas no invalida los resultados reportados, pero puede dificultar su demostración durante la evaluación.
