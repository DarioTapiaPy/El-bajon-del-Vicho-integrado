
import { useEffect, useState } from 'react'
import { consultarTodasReservas, cancelarReserva } from '../services/reservasApi.js'

function AdminSection({
                          products,
                          onSaveProduct,
                          onToggleStock,
                          onLogout,
                          authToken,
                          onSessionRenewed,
                      }) {
    const [reservations, setReservations] = useState([])
    const [loadingReservations, setLoadingReservations] = useState(false)
    const [reservationError, setReservationError] = useState('')
    const [cancelingId, setCancelingId] = useState(null)
    const [reservationsRefresh, setReservationsRefresh] = useState(0)
    const [drafts, setDrafts] = useState({})
    const [savingId, setSavingId] = useState(null)

    // La lista viene exclusivamente del microservicio, no de localStorage.
    useEffect(() => {
        let active = true

        if (!authToken) {
            setReservations([])
            setReservationError('')
            return () => { active = false }
        }

        setLoadingReservations(true)
        setReservationError('')

        consultarTodasReservas(authToken, onSessionRenewed)
            .then((data) => {
                if (!active) return
                if (!Array.isArray(data)) {
                    throw new Error('La API no devolvió una lista de reservas.')
                }
                setReservations(data)
            })
            .catch((error) => {
                if (!active) return
                setReservationError(error.message || 'No fue posible cargar las reservas.')
            })
            .finally(() => {
                if (active) setLoadingReservations(false)
            })

        return () => { active = false }
    }, [authToken, reservationsRefresh])

    useEffect(() => {
        const newDrafts = {}

        products.forEach((product) => {
            newDrafts[product.id] = {
                description: product.description,
                price: product.price,
            }
        })

        setDrafts(newDrafts)
    }, [products])

    const handleDraftChange = (productId, field, value) => {
        setDrafts((currentDrafts) => ({
            ...currentDrafts,
            [productId]: {
                ...currentDrafts[productId],
                [field]: value,
            },
        }))
    }

    const handleSave = async (productId) => {
        const draft = drafts[productId]
        if (!draft || savingId) return

        const numericPrice = Number(draft.price)

        if (
            !draft.description.trim() ||
            !Number.isFinite(numericPrice) ||
            numericPrice <= 0
        ) {
            window.alert('La descripción y el precio deben ser válidos.')
            return
        }

        setSavingId(productId)

        try {
            await onSaveProduct(
                productId,
                draft.description.trim(),
                numericPrice
            )
            window.alert('Producto actualizado en MySQL.')
        } catch (error) {
            window.alert(
                error.message || 'Error al guardar el producto.'
            )
        } finally {
            setSavingId(null)
        }
    }

    const handleStock = async (productId) => {
        if (savingId) return

        setSavingId(productId)

        try {
            await onToggleStock(productId)
        } catch (error) {
            window.alert(
                error.message || 'Error al actualizar disponibilidad.'
            )
        } finally {
            setSavingId(null)
        }
    }

    const handleCancelReservation = async (reservation) => {
        if (cancelingId !== null || reservation.estado === 'CANCELADA') return
        if (!window.confirm(`¿Cancelar la reserva #${reservation.id}? Se conservará en el historial.`)) return

        setCancelingId(reservation.id)
        setReservationError('')
        try {
            await cancelarReserva(reservation.id, authToken, onSessionRenewed)
            setReservations((current) => current.map((item) =>
                item.id === reservation.id
                    ? { ...item, estado: 'CANCELADA' }
                    : item
            ))
        } catch (error) {
            setReservationError(error.message || 'No fue posible cancelar la reserva.')
        } finally {
            setCancelingId(null)
        }
    }

    return (
        <section id="admin" className="py-5 bg-black">
            <div className="container py-4">
                <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
                    <div>
                        <p className="text-warning fw-bold mb-1">
                            PANEL DE CONTROL
                        </p>

                        <h2 className="text-light fw-bold mb-0">
                            Gestión de Productos
                        </h2>
                    </div>

                    <button
                        type="button"
                        className="btn btn-outline-light"
                        onClick={onLogout}
                    >
                        Cerrar sesión admin
                    </button>
                </div>

                <div className="table-responsive">
                    <table className="table table-dark table-bordered align-middle">
                        <thead>
                        <tr>
                            <th>Producto</th>
                            <th style={{ minWidth: '300px' }}>
                                Descripción
                            </th>
                            <th style={{ minWidth: '130px' }}>
                                Precio ($)
                            </th>
                            <th>Stock</th>
                            <th>Acción</th>
                        </tr>
                        </thead>

                        <tbody>
                        {products.map((product) => (
                            <tr key={product.id}>
                                <td>
                                    <strong className="text-warning">
                                        {product.name}
                                    </strong>
                                </td>

                                <td>
                                        <textarea
                                            className="form-control"
                                            rows="4"
                                            value={
                                                drafts[product.id]
                                                    ?.description ?? ''
                                            }
                                            onChange={(event) =>
                                                handleDraftChange(
                                                    product.id,
                                                    'description',
                                                    event.target.value
                                                )
                                            }
                                            disabled={savingId !== null}
                                        />
                                </td>

                                <td>
                                    <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        className="form-control"
                                        value={
                                            drafts[product.id]
                                                ?.price ?? ''
                                        }
                                        onChange={(event) =>
                                            handleDraftChange(
                                                product.id,
                                                'price',
                                                event.target.value
                                            )
                                        }
                                        disabled={savingId !== null}
                                    />
                                </td>

                                <td>
                                    <button
                                        type="button"
                                        className={
                                            product.stock
                                                ? 'btn btn-success btn-sm'
                                                : 'btn btn-danger btn-sm'
                                        }
                                        onClick={() =>
                                            handleStock(product.id)
                                        }
                                        disabled={savingId !== null}
                                    >
                                        {product.stock
                                            ? 'En Stock (Ocultar)'
                                            : 'Agotado (Mostrar)'}
                                    </button>
                                </td>

                                <td>
                                    <button
                                        type="button"
                                        className="btn btn-warning btn-sm fw-bold"
                                        onClick={() =>
                                            handleSave(product.id)
                                        }
                                        disabled={savingId !== null}
                                    >
                                        {savingId === product.id
                                            ? 'Guardando...'
                                            : 'Guardar'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <div className="mt-5">
                    <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                        <h2 className="text-light fw-bold mb-0">
                            Gestión de Reservas
                        </h2>
                        <button
                            type="button"
                            className="btn btn-outline-warning btn-sm"
                            disabled={!authToken || loadingReservations}
                            onClick={() => setReservationsRefresh((n) => n + 1)}
                        >
                            Actualizar reservas
                        </button>
                    </div>

                    {reservationError && (
                        <p className="alert alert-danger" role="alert">
                            {reservationError}
                        </p>
                    )}
                    {loadingReservations && (
                        <p className="text-light" role="status">
                            Cargando reservas desde MySQL...
                        </p>
                    )}

                    <div className="table-responsive">
                        <table className="table table-dark table-bordered align-middle">
                            <thead>
                            <tr>
                                <th>Cliente</th>
                                <th>Sucursal</th>
                                <th>Fecha</th>
                                <th>Hora</th>
                                <th>Personas</th>
                                <th>Estado</th>
                                <th>Acciones</th>
                            </tr>
                            </thead>

                            <tbody>
                            {reservations.length === 0 ? (
                                <tr>
                                    <td
                                        colSpan="7"
                                        className="text-center text-secondary py-4"
                                    >
                                        No hay reservas registradas.
                                    </td>
                                </tr>
                            ) : (
                                reservations.map((reservation) => (
                                        <tr key={reservation.id}>
                                            <td>
                                                <strong>
                                                    {reservation.correo}
                                                </strong>
                                            </td>

                                            <td>
                                                {reservation.sucursal}
                                            </td>

                                            <td>
                                                {reservation.fecha}
                                            </td>

                                            <td>
                                                {reservation.hora}
                                            </td>

                                            <td>
                                                {reservation.personas}
                                            </td>

                                            <td>
                                                <span className={reservation.estado === 'CANCELADA'
                                                    ? 'badge text-bg-secondary'
                                                    : 'badge text-bg-success'}>
                                                    {reservation.estado}
                                                </span>
                                            </td>

                                            <td>
                                                <div className="d-flex flex-column gap-2">
                                                    <a
                                                        href={`mailto:${reservation.correo}?subject=Sobre tu reserva en El Bajón del Vicho`}
                                                        className="btn btn-warning btn-sm fw-bold"
                                                    >
                                                        Contactar
                                                    </a>

                                                    <button
                                                        type="button"
                                                        className="btn btn-danger btn-sm"
                                                        disabled={cancelingId !== null || reservation.estado === 'CANCELADA'}
                                                        onClick={() => handleCancelReservation(reservation)}
                                                    >
                                                        {reservation.estado === 'CANCELADA'
                                                            ? 'Cancelada'
                                                            : cancelingId === reservation.id
                                                                ? 'Cancelando...'
                                                                : 'Cancelar reserva'}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    )
                                )
                            )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default AdminSection
