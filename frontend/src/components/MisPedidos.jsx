import { useEffect, useRef, useState } from 'react'
import { consultarMisPedidos } from '../services/pedidosApi.js'

const ETIQUETAS_ESTADO = {
    CREADO: 'Creado',
    CONFIRMADO: 'Confirmado',
    EN_PREPARACION: 'En preparación',
    LISTO: 'Listo para entregar',
    ENTREGADO: 'Entregado',
    CANCELADO: 'Cancelado',
}

const FORMATO_CLP = new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
})

function mostrarPrecio(valor) {
    if (valor === null || valor === undefined || valor === '') {
        return 'No disponible'
    }

    const numero = Number(valor)
    return Number.isFinite(numero) ? FORMATO_CLP.format(numero) : 'No disponible'
}

function mostrarFecha(valor) {
    if (!valor) return null

    const fecha = new Date(valor)
    if (Number.isNaN(fecha.getTime())) return null

    return new Intl.DateTimeFormat('es-CL', {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(fecha)
}

function MisPedidos({ authToken, onSessionRenewed, onClose }) {
    const [pedidos, setPedidos] = useState([])
    const [estadoConsulta, setEstadoConsulta] = useState('cargando')
    const [errorConsulta, setErrorConsulta] = useState('')
    const [numeroConsulta, setNumeroConsulta] = useState(0)
    const renovarCallback = useRef(onSessionRenewed)

    // Evita volver a consultar por cada renderizado de App.jsx.
    renovarCallback.current = onSessionRenewed

    useEffect(() => {
        let activo = true

        async function cargarMisPedidos() {
            setEstadoConsulta('cargando')
            setErrorConsulta('')

            try {
                const respuesta = await consultarMisPedidos(
                    authToken,
                    (sesion) => renovarCallback.current?.(sesion)
                )

                if (!Array.isArray(respuesta)) {
                    throw new Error('El servidor no devolvió una lista de pedidos válida.')
                }

                const ordenados = [...respuesta].sort(
                    (a, b) => Number(b.id || 0) - Number(a.id || 0)
                )

                if (!activo) return
                setPedidos(ordenados)
                setEstadoConsulta('listo')
            } catch (error) {
                if (!activo) return
                setPedidos([])
                setErrorConsulta(error?.message || 'No se pudieron consultar tus pedidos.')
                setEstadoConsulta('error')
            }
        }

        cargarMisPedidos()
        return () => { activo = false }
    }, [authToken, numeroConsulta])

    return (
        <section id="mis-pedidos" className="bg-black text-light py-5" aria-labelledby="mis-pedidos-titulo">
            <div className="container py-3">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                    <div>
                        <h2 id="mis-pedidos-titulo" className="text-warning fw-bold mb-1">
                            Mis pedidos
                        </h2>
                        <p className="text-light-emphasis mb-0">
                            Consulta el estado y detalle de tus compras.
                        </p>
                    </div>
                    <div className="d-flex gap-2">
                        <button
                            type="button"
                            className="btn btn-outline-warning"
                            onClick={() => setNumeroConsulta((actual) => actual + 1)}
                            disabled={estadoConsulta === 'cargando'}
                        >
                            Actualizar
                        </button>
                        <button type="button" className="btn btn-outline-light" onClick={onClose}>
                            Cerrar
                        </button>
                    </div>
                </div>

                {estadoConsulta === 'cargando' && (
                    <p role="status" className="text-warning">Cargando tus pedidos...</p>
                )}

                {estadoConsulta === 'error' && (
                    <div role="alert" className="alert alert-danger">
                        {errorConsulta}
                    </div>
                )}

                {estadoConsulta === 'listo' && pedidos.length === 0 && (
                    <div className="border border-secondary rounded p-4">
                        Todavía no tienes pedidos registrados.
                    </div>
                )}

                {estadoConsulta === 'listo' && pedidos.length > 0 && (
                    <div className="d-grid gap-3">
                        {pedidos.map((pedido, indice) => {
                            const detalles = Array.isArray(pedido.detalles) ? pedido.detalles : []
                            const fecha = mostrarFecha(
                                pedido.fechaCreacion || pedido.fecha || pedido.createdAt
                            )
                            const estado = pedido.estado || 'SIN_ESTADO'

                            return (
                                <article
                                    key={pedido.id ?? indice}
                                    className="card bg-dark text-light border-secondary"
                                >
                                    <div className="card-body">
                                        <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                                            <div>
                                                <h3 className="h5 text-warning mb-1">
                                                    Pedido #{pedido.id ?? '—'}
                                                </h3>
                                                {fecha && (
                                                    <span className="small text-light-emphasis">{fecha}</span>
                                                )}
                                            </div>
                                            <span className="badge bg-warning text-dark fs-6">
                                                {ETIQUETAS_ESTADO[estado] || estado.replaceAll('_', ' ')}
                                            </span>
                                        </div>

                                        {detalles.length > 0 ? (
                                            <ul className="list-group list-group-flush mb-3">
                                                {detalles.map((detalle, detalleIndice) => {
                                                    const nombre = detalle.nombreProducto ||
                                                        detalle.productoNombre ||
                                                        detalle.nombre ||
                                                        detalle.producto?.name ||
                                                        detalle.productoId ||
                                                        'Producto'
                                                    return (
                                                        <li
                                                            key={detalle.id ?? detalleIndice}
                                                            className="list-group-item bg-dark text-light border-secondary px-0 d-flex flex-wrap justify-content-between gap-2"
                                                        >
                                                            <span>
                                                                {nombre} — Cantidad: {detalle.cantidad ?? '—'}
                                                            </span>
                                                            {detalle.subtotal !== undefined && (
                                                                <span>{mostrarPrecio(detalle.subtotal)}</span>
                                                            )}
                                                        </li>
                                                    )
                                                })}
                                            </ul>
                                        ) : (
                                            <p className="text-light-emphasis small">
                                                El servidor no entregó el detalle de los productos.
                                            </p>
                                        )}

                                        <div className="text-end fw-bold">
                                            Total: <span className="text-warning">{mostrarPrecio(pedido.total)}</span>
                                        </div>
                                    </div>
                                </article>
                            )
                        })}
                    </div>
                )}
            </div>
        </section>
    )
}

export default MisPedidos
