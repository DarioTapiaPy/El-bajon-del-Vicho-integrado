
const API_URL = '/api/v1/auth'

// Evita dos renovaciones simultáneas.
// Es especialmente útil con React StrictMode.
let renovacionEnCurso = null

export async function iniciarSesion(email, password) {
    const respuesta = await fetch(`${API_URL}/login`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
    })

    if (!respuesta.ok) {
        throw new Error(
            respuesta.status === 401
                ? 'Correo o contraseña incorrectos.'
                : 'No se pudo iniciar sesión.'
        )
    }

    return respuesta.json()
}

export async function obtenerPerfil(token) {
    const respuesta = await fetch(`${API_URL}/me`, {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    })

    if (!respuesta.ok) {
        throw new Error('Sesión inválida o expirada.')
    }

    return respuesta.json()
}

export async function registrarCliente(datos) {
    const respuesta = await fetch(`${API_URL}/registro`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(datos),
    })

    if (!respuesta.ok) {
        throw new Error(
            respuesta.status === 409
                ? 'Este correo ya está registrado.'
                : 'No se pudo registrar la cuenta.'
        )
    }

    return respuesta.json()
}

// Recuperar sesión utilizando la cookie HttpOnly
async function ejecutarRenovacion() {
    const respuesta = await fetch(`${API_URL}/refresh`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            'X-Session-Request': '1',
        },
    })

    // No existe una sesión renovable
    if (respuesta.status === 401) {
        return null
    }

    if (!respuesta.ok) {
        throw new Error(
            `No se pudo renovar la sesión (HTTP ${respuesta.status}).`
        )
    }

    return respuesta.json()
}

export function renovarSesion() {
    if (!renovacionEnCurso) {
        renovacionEnCurso = ejecutarRenovacion()
            .finally(() => {
                renovacionEnCurso = null
            })
    }

    return renovacionEnCurso
}

// Revocar la sesión en MySQL y borrar la cookie
export async function cerrarSesion() {
    const respuesta = await fetch(`${API_URL}/logout`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            'X-Session-Request': '1',
        },
    })

    if (!respuesta.ok) {
        throw new Error(
            `No se pudo cerrar la sesión (HTTP ${respuesta.status}).`
        )
    }

    return true
}
