import { useEffect, useRef, useState } from 'react'
import {
    consultarTodosPedidos,
    actualizarEstadoPedido,
} from '../services/pedidosApi.js'

const SIGUIENTES_ESTADOS = {
    CREADO: ['CONFIRMADO', 'CANCELADO'],
    CONFIRMADO: ['EN_PREPARACION', 'CANCELADO'],
    EN_PREPARACION: ['LISTO', 'CANCELADO'],
    LISTO: ['ENTREGADO', 'CANCELADO'],
    ENTREGADO: [],
    CANCELADO: [],
}

const ETIQUETAS_ESTADOS = {
    CREADO: 'Creado',
    CONFIRMADO: 'Confirmado',
    EN_PREPARACION: 'En preparación',
    LISTO: 'Listo',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado',
}

function formatearPrecio(valor) {
    const numero = Number(valor)
    return Number.isFinite(numero)
        ? `$${numero.toLocaleString('es-CL')}`
        : 'Sin información'
}

function StaffCatalog({
                          products,
                          status,
                          onRetry,
                          onLogout,
                          authToken,
                          onSessionRenewed,
                      }) {
    const [selectedId, setSelectedId] = useState(null)
    const [pedidos, setPedidos] = useState([])
    const [pedidosStatus, setPedidosStatus] = useState('loading')
    const [pedidosError, setPedidosError] = useState('')
    const [pedidosRetry, setPedidosRetry] = useState(0)
    const [actualizandoId, setActualizandoId] = useState(null)
    const [estadoElegido, setEstadoElegido] = useState({})
    const [accionError, setAccionError] = useState('')
    // App.jsx entrega un callback nuevo en cada render; no debe recargar pedidos por eso.
    const renovarSesionRef = useRef(onSessionRenewed)
    renovarSesionRef.current = onSessionRenewed

    const selectedProduct =
        products.find((p) => p.id === selectedId) ||
        products[0] ||
        null

    useEffect(() => {
        let activo = true

        async function cargarPedidos() {
            setPedidosStatus('loading')
            setPedidosError('')

            try {
                const resultado = await consultarTodosPedidos(
                    authToken,
                    renovarSesionRef.current
                )

                if (!activo) return
                if (!Array.isArray(resultado)) {
                    throw new Error('El servidor no devolvió una lista de pedidos.')
                }

                setPedidos(resultado)
                setPedidosStatus('success')
            } catch (error) {
                if (!activo) return
                setPedidosError(error.message || 'No se pudieron cargar los pedidos.')
                setPedidosStatus('error')
            }
        }

        if (authToken) {
            cargarPedidos()
        } else {
            setPedidos([])
            setPedidosStatus('error')
            setPedidosError('Debes iniciar sesión para consultar pedidos.')
        }

        return () => { activo = false }
    }, [authToken, pedidosRetry])

    async function cambiarEstado(pedido) {
        const nuevoEstado = estadoElegido[pedido.id]
        const opciones = SIGUIENTES_ESTADOS[pedido.estado] || []

        if (!opciones.includes(nuevoEstado) || actualizandoId !== null) return

        setActualizandoId(pedido.id)
        setAccionError('')

        try {
            const actualizado = await actualizarEstadoPedido(
                pedido.id,
                nuevoEstado,
                authToken,
                onSessionRenewed
            )

            setPedidos((actuales) =>
                actuales.map((actual) =>
                    actual.id === pedido.id ? actualizado : actual
                )
            )
            setEstadoElegido((actual) => ({ ...actual, [pedido.id]: '' }))
        } catch (error) {
            setAccionError(
                `Pedido #${pedido.id}: ${error.message || 'No se pudo actualizar el estado.'}`
            )
        } finally {
            setActualizandoId(null)
        }
    }

    return (
        <main className="min-vh-100 bg-black text-light">
            <header className="border-bottom border-secondary py-3">
                <div className="container d-flex justify-content-between align-items-center">
                    <div className="d-flex align-items-center gap-3">
                        <img src="/assets/logo.png" alt="El Bajón del Vicho" width="65" />
                        <span className="fw-bold text-warning">Panel VENDEDOR</span>
                    </div>
                    <button type="button" className="btn btn-outline-light" onClick={onLogout}>
                        Cerrar sesión
                    </button>
                </div>
            </header>

            <div className="container py-5">
                <h1 className="h2 text-warning mb-3">Catálogo de productos</h1>
                <p className="text-secondary mb-4">
                    Consulta los productos y sus detalles.
                    Este perfil no tiene permisos de edición del catálogo.
                </p>

                {status === 'loading' && <p>Cargando productos...</p>}

                {status === 'error' && (
                    <div role="alert">
                        <p>No se pudo cargar el catálogo.</p>
                        <button type="button" className="btn btn-warning" onClick={onRetry}>
                            Reintentar
                        </button>
                    </div>
                )}

                {status === 'success' && (
                    <div className="row g-4">
                        <div className="col-md-5">
                            <h2 className="h5 mb-3">Lista de productos</h2>
                            <div className="list-group">
                                {products.map((product) => (
                                    <button
                                        type="button"
                                        key={product.id}
                                        className={
                                            'list-group-item list-group-item-action ' +
                                            (selectedProduct?.id === product.id ? 'active' : '')
                                        }
                                        onClick={() => setSelectedId(product.id)}
                                    >
                                        {product.name}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="col-md-7">
                            {selectedProduct && (
                                <article className="card bg-dark text-light border-secondary">
                                    <img
                                        src={selectedProduct.image}
                                        alt={selectedProduct.name}
                                        className="card-img-top"
                                        style={{ maxHeight: '280px', objectFit: 'contain' }}
                                    />
                                    <div className="card-body">
                                        <h2 className="h4 text-warning">{selectedProduct.name}</h2>
                                        <p>{selectedProduct.description}</p>
                                        <p><strong>Precio: </strong>{formatearPrecio(selectedProduct.price)}</p>
                                        <p><strong>Categoría: </strong>{selectedProduct.category}</p>
                                        <p>
                                            <strong>Disponibilidad: </strong>
                                            {selectedProduct.stock ? 'Disponible' : 'Agotado'}
                                        </p>
                                    </div>
                                </article>
                            )}
                        </div>
                    </div>
                )}

                <section className="mt-5 border-top border-secondary pt-4" aria-labelledby="titulo-pedidos">
                    <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
                        <h2 className="h4 text-warning mb-0" id="titulo-pedidos">Órdenes</h2>
                        <button
                            type="button"
                            className="btn btn-outline-warning btn-sm"
                            onClick={() => setPedidosRetry((valor) => valor + 1)}
                            disabled={pedidosStatus === 'loading' || actualizandoId !== null}
                        >
                            Actualizar lista
                        </button>
                    </div>

                    <p className="text-secondary">
                        Consulta todos los pedidos y avanza sus estados según las reglas del sistema.
                    </p>

                    {pedidosStatus === 'loading' && <p role="status">Cargando pedidos...</p>}

                    {pedidosStatus === 'error' && (
                        <div className="alert alert-danger" role="alert">{pedidosError}</div>
                    )}

                    {accionError && (
                        <div className="alert alert-danger" role="alert">{accionError}</div>
                    )}

                    {pedidosStatus === 'success' && pedidos.length === 0 && (
                        <p>No hay pedidos registrados todavía.</p>
                    )}

                    {pedidosStatus === 'success' && pedidos.length > 0 && (
                        <div className="row g-3">
                            {[...pedidos].sort((a, b) => Number(b.id) - Number(a.id)).map((pedido) => {
                                const siguientes = SIGUIENTES_ESTADOS[pedido.estado] || []
                                const bloqueado = actualizandoId !== null
                                const estadoSeleccionado = estadoElegido[pedido.id] || ''

                                return (
                                    <div className="col-12" key={pedido.id}>
                                        <article className="card bg-dark text-light border-secondary">
                                            <div className="card-body">
                                                <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                                                    <h3 className="h5 text-warning mb-1">Pedido #{pedido.id}</h3>
                                                    <span className="badge bg-secondary">
                                                        {ETIQUETAS_ESTADOS[pedido.estado] || pedido.estado}
                                                    </span>
                                                </div>
                                                <p className="mb-1"><strong>Cliente ID:</strong> {pedido.usuarioId ?? 'No disponible'}</p>
                                                <p className="mb-3"><strong>Total:</strong> {formatearPrecio(pedido.total)}</p>

                                                {Array.isArray(pedido.detalles) && pedido.detalles.length > 0 && (
                                                    <div className="mb-3">
                                                        <h4 className="h6">Productos</h4>
                                                        <ul className="mb-0">
                                                            {pedido.detalles.map((detalle, indice) => (
                                                                <li key={detalle.id ?? `${detalle.productoId}-${indice}`}>
                                                                    {detalle.nombreProducto || detalle.productoNombre || detalle.productoId || 'Producto'}
                                                                    {' — '}Cantidad: {detalle.cantidad}
                                                                </li>
                                                            ))}
                                                        </ul>
                                                    </div>
                                                )}

                                                {siguientes.length > 0 ? (
                                                    <div className="d-flex flex-wrap gap-2 align-items-center">
                                                        <label className="visually-hidden" htmlFor={`estado-pedido-${pedido.id}`}>
                                                            Nuevo estado para pedido #{pedido.id}
                                                        </label>
                                                        <select
                                                            id={`estado-pedido-${pedido.id}`}
                                                            className="form-select form-select-sm w-auto"
                                                            value={estadoSeleccionado}
                                                            onChange={(event) => setEstadoElegido((actual) => ({
                                                                ...actual,
                                                                [pedido.id]: event.target.value,
                                                            }))}
                                                            disabled={bloqueado}
                                                        >
                                                            <option value="">Seleccionar nuevo estado</option>
                                                            {siguientes.map((estado) => (
                                                                <option value={estado} key={estado}>
                                                                    {ETIQUETAS_ESTADOS[estado]}
                                                                </option>
                                                            ))}
                                                        </select>
                                                        <button
                                                            type="button"
                                                            className="btn btn-warning btn-sm"
                                                            disabled={bloqueado || !estadoSeleccionado}
                                                            onClick={() => cambiarEstado(pedido)}
                                                        >
                                                            {actualizandoId === pedido.id ? 'Guardando...' : 'Guardar estado'}
                                                        </button>
                                                    </div>
                                                ) : (
                                                    <p className="text-secondary mb-0">
                                                        Pedido finalizado. No admite más cambios.
                                                    </p>
                                                )}
                                            </div>
                                        </article>
                                    </div>
                                )
                            })}
                        </div>
                    )}
                </section>
            </div>
        </main>
    )
}

export default StaffCatalog
