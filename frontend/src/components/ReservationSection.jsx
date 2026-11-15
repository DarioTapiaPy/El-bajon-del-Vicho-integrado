import { useEffect, useRef, useState } from 'react'
import { crearReserva, consultarMisReservas } from '../services/reservasApi.js'

function getDateRange() {
    const today = new Date()
    const limit = new Date()
    limit.setDate(today.getDate() + 30)

    const formatDate = (date) => {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        return `${year}-${month}-${day}`
    }

    return { minDate: formatDate(today), maxDate: formatDate(limit) }
}

function ReservationSection({
    userEmail,
    authToken,
    onRequireLogin,
    onSessionRenewed,
}) {
    const [people, setPeople] = useState('')
    const [date, setDate] = useState('')
    const [time, setTime] = useState('')
    const [branch, setBranch] = useState('')
    const [successMessage, setSuccessMessage] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [reservas, setReservas] = useState(null)
    const [loadingList, setLoadingList] = useState(false)
    const [listError, setListError] = useState('')
    const consultaActual = useRef(0)

    // No mostrar el historial de una cuenta después de cerrar sesión
    // o iniciar sesión con otra cuenta en el mismo navegador.
    useEffect(() => {
        consultaActual.current += 1
        setReservas(null)
        setListError('')
        setSuccessMessage('')
    }, [userEmail])

    const { minDate, maxDate } = getDateRange()
    const autenticado = Boolean(authToken && userEmail && userEmail !== 'Invitado')

    const handleSubmit = async (event) => {
        event.preventDefault()
        if (submitting) return

        setSuccessMessage('')
        setErrorMessage('')

        if (!autenticado) {
            onRequireLogin?.()
            return
        }

        const personas = Number(people)
        if (!Number.isInteger(personas) || personas < 1 || personas > 5 ||
            !date || date < minDate || date > maxDate || !time ||
            time < '12:00' || time > '23:00' || !branch) {
            setErrorMessage('Completa correctamente los datos de la reserva.')
            return
        }

        setSubmitting(true)
        try {
            // No enviamos correo, usuarioId ni estado: se obtienen del JWT.
            const creada = await crearReserva({
                personas,
                fecha: date,
                hora: time,
                sucursal: branch,
            }, authToken, onSessionRenewed)

            setSuccessMessage(`¡Reserva #${creada.id} confirmada para ${creada.correo}!`)
            setPeople('')
            setDate('')
            setTime('')
            setBranch('')

            // Si el historial estaba abierto, actualizamos la lista sin recargar.
            setReservas((actuales) => Array.isArray(actuales)
                ? [...actuales, creada].sort((a, b) =>
                    `${a.fecha} ${a.hora}`.localeCompare(`${b.fecha} ${b.hora}`))
                : null)
        } catch (error) {
            setErrorMessage(error.message || 'No fue posible guardar la reserva.')
        } finally {
            setSubmitting(false)
        }
    }

    const handleConsultar = async () => {
        setListError('')
        if (!autenticado) {
            onRequireLogin?.()
            return
        }

        const numeroConsulta = ++consultaActual.current
        setLoadingList(true)
        try {
            const resultado = await consultarMisReservas(authToken, onSessionRenewed)
            if (!Array.isArray(resultado)) {
                throw new Error('La API no devolvió una lista de reservas.')
            }
            if (numeroConsulta === consultaActual.current) {
                setReservas(resultado)
            }
        } catch (error) {
            if (numeroConsulta === consultaActual.current) {
                setListError(error.message || 'No se pudieron consultar las reservas.')
            }
        } finally {
            if (numeroConsulta === consultaActual.current) {
                setLoadingList(false)
            }
        }
    }

    return (
        <section id="reservas" className="py-5 bg-dark">
            <div className="container py-4">
                <div className="row align-items-center g-5">
                    <div className="col-lg-5">
                        <p className="text-warning fw-bold mb-2">¿VAS A VENIR?</p>
                        <h2 className="display-5 fw-bold text-light">RESERVA TU MESA</h2>
                        <p className="text-secondary fs-5">
                            Junta a los tuyos y ven a pegarte el bajón en nuestras sucursales.
                        </p>
                        <p className="text-secondary small">
                            Las reservas se guardan en nuestro sistema y puedes consultarlas desde aquí.
                        </p>
                    </div>

                    <div className="col-lg-7">
                        <form
                            className="bg-black border border-secondary rounded p-4"
                            onSubmit={handleSubmit}
                        >
                            <div className="row g-3">
                                <div className="col-md-6">
                                    <label htmlFor="people" className="form-label text-light">Cantidad de personas</label>
                                    <input type="number" id="people" className="form-control"
                                        min="1" max="5" placeholder="Ej: 4" value={people}
                                        onChange={(event) => setPeople(event.target.value)} required />
                                </div>

                                <div className="col-md-6">
                                    <label htmlFor="date" className="form-label text-light">Fecha</label>
                                    <input type="date" id="date" className="form-control"
                                        min={minDate} max={maxDate} value={date}
                                        onChange={(event) => setDate(event.target.value)} required />
                                </div>

                                <div className="col-md-6">
                                    <label htmlFor="time" className="form-label text-light">Hora</label>
                                    <input type="time" id="time" className="form-control"
                                        min="12:00" max="23:00" value={time}
                                        onChange={(event) => setTime(event.target.value)} required />
                                </div>

                                <div className="col-md-6">
                                    <label htmlFor="branch" className="form-label text-light">Sucursal</label>
                                    <select id="branch" className="form-select" value={branch}
                                        onChange={(event) => setBranch(event.target.value)} required>
                                        <option value="">Seleccionar sucursal</option>
                                        <option value="Sucursal Viña del Mar">Sucursal Viña del Mar</option>
                                        <option value="Sucursal Puente Alto">Sucursal Puente Alto</option>
                                    </select>
                                </div>

                                <div className="col-12">
                                    <button type="submit" disabled={submitting}
                                        className="btn btn-warning fw-bold w-100 py-3 mt-2">
                                        {submitting ? 'Guardando reserva...' : 'Reservar mesa'}
                                    </button>
                                </div>
                            </div>

                            {successMessage && (
                                <div className="alert alert-success mt-3 mb-0" role="status">
                                    {successMessage}
                                </div>
                            )}
                            {errorMessage && (
                                <div className="alert alert-danger mt-3 mb-0" role="alert">
                                    {errorMessage}
                                </div>
                            )}
                        </form>

                        <div className="mt-3">
                            <button type="button" onClick={handleConsultar} disabled={loadingList}
                                className="btn btn-outline-warning">
                                {loadingList ? 'Consultando...' : 'Ver mis reservas'}
                            </button>
                            {listError && (
                                <p className="text-warning mt-2" role="alert">{listError}</p>
                            )}
                            {Array.isArray(reservas) && (
                                <div className="bg-black text-light border border-secondary rounded p-3 mt-3">
                                    <h3 className="h5 mb-3">Mis reservas</h3>
                                    {reservas.length === 0 ? (
                                        <p className="text-secondary mb-0">Todavía no tienes reservas.</p>
                                    ) : (
                                        <ul className="list-group">
                                            {reservas.map((reserva) => (
                                                <li key={reserva.id}
                                                    className="list-group-item bg-dark text-light border-secondary">
                                                    <strong>#{reserva.id}</strong> — {reserva.sucursal}
                                                    <div className="small">
                                                        {reserva.fecha} a las {String(reserva.hora).slice(0, 5)}
                                                        {' · '}{reserva.personas} persona(s)
                                                        {' · '}<span>{reserva.estado}</span>
                                                    </div>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default ReservationSection
