import { renovarSesion } from './authApi.js'

// Vite redirige /reservas-api hacia el microservicio en localhost:8082.
// La misma URL relativa funciona desde React sin habilitar CORS en desarrollo.
const BASE_URL = '/reservas-api/api/v1/reservas'

async function solicitarReservas(ruta, token, opciones = {}, onSessionRenewed) {
    if (!token) {
        throw new Error('Debes iniciar sesión antes de gestionar reservas.')
    }

    const enviar = (jwt) => fetch(`${BASE_URL}${ruta}`, {
        ...opciones,
        headers: {
            ...opciones.headers,
            Authorization: `Bearer ${jwt}`,
        },
    })

    let respuesta = await enviar(token)

    // El access token dura una hora. Intentamos renovarlo una sola vez
    // con la cookie HttpOnly que ya utiliza el backend principal.
    if (respuesta.status === 401) {
        let sesion
        try {
            sesion = await renovarSesion()
        } catch {
            throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.')
        }

        if (!sesion?.token || !sesion?.usuario?.email) {
            throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.')
        }

        onSessionRenewed?.(sesion)
        respuesta = await enviar(sesion.token)
    }

    if (!respuesta.ok) {
        let detalle = ''
        try {
            const cuerpo = await respuesta.json()
            detalle = typeof cuerpo.detail === 'string' ? cuerpo.detail : ''
        } catch {
            // Algunos errores HTTP no incluyen cuerpo JSON.
        }

        if (respuesta.status === 400) {
            throw new Error(detalle || 'La reserva tiene datos inválidos. Revisa el formulario.')
        }
        if (respuesta.status === 401) {
            throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.')
        }
        if (respuesta.status === 403) {
            throw new Error('Tu cuenta no tiene permisos para esta operación.')
        }
        throw new Error(detalle || `No se pudo conectar con reservas (HTTP ${respuesta.status}).`)
    }

    return respuesta.status === 204 ? null : respuesta.json()
}

export function crearReserva(datos, token, onSessionRenewed) {
    return solicitarReservas('', token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(datos),
    }, onSessionRenewed)
}

export function consultarMisReservas(token, onSessionRenewed) {
    return solicitarReservas('/mis-reservas', token, {
        method: 'GET',
    }, onSessionRenewed)
}

// Administración: requiere rol ADMIN en el backend.
export function consultarTodasReservas(token, onSessionRenewed) {
    return solicitarReservas('', token, { method: 'GET' }, onSessionRenewed)
}

export function cancelarReserva(id, token, onSessionRenewed) {
    const reservaId = Number(id)
    if (!Number.isSafeInteger(reservaId) || reservaId <= 0) {
        throw new Error('ID de reserva inválido.')
    }
    return solicitarReservas(`/${reservaId}`, token, { method: 'DELETE' }, onSessionRenewed)
}
