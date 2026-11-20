
import { renovarSesion } from './authApi.js'

// Vite redirige /pedidos-api hacia localhost:8083.
// Utilizamos URLs relativas para evitar problemas de CORS
// durante el desarrollo local.
const BASE_URL = '/pedidos-api/api/v1/pedidos'

async function solicitarPedidos(
    ruta,
    token,
    opciones = {},
    onSessionRenewed
) {
    if (!token) {
        throw new Error(
            'Debes iniciar sesión antes de gestionar pedidos.'
        )
    }

    const enviar = (jwt) => fetch(`${BASE_URL}${ruta}`, {
        ...opciones,
        headers: {
            ...opciones.headers,
            Authorization: `Bearer ${jwt}`,
        },
    })

    let respuesta = await enviar(token)

    // Renovar el JWT una sola vez si expiró.
    // Reutilizamos la cookie HttpOnly del backend principal.
    if (respuesta.status === 401) {
        let sesion

        try {
            sesion = await renovarSesion()
        } catch {
            throw new Error(
                'Tu sesión expiró. Vuelve a iniciar sesión.'
            )
        }

        if (!sesion?.token || !sesion?.usuario?.email) {
            throw new Error(
                'Tu sesión expiró. Vuelve a iniciar sesión.'
            )
        }

        onSessionRenewed?.(sesion)

        respuesta = await enviar(sesion.token)
    }

    // Manejo de errores HTTP
    if (!respuesta.ok) {
        let detalle = ''

        try {
            const cuerpo = await respuesta.json()

            detalle =
                typeof cuerpo.detail === 'string'
                    ? cuerpo.detail
                    : ''
        } catch {
            // Algunos errores HTTP no incluyen JSON.
        }

        if (respuesta.status === 400) {
            throw new Error(
                detalle ||
                'El pedido tiene datos inválidos.'
            )
        }

        if (respuesta.status === 401) {
            throw new Error(
                'Tu sesión expiró. Vuelve a iniciar sesión.'
            )
        }

        if (respuesta.status === 403) {
            throw new Error(
                'Tu cuenta no tiene permisos para esta operación.'
            )
        }

        if (respuesta.status === 404) {
            throw new Error(
                detalle || 'El pedido solicitado no existe.'
            )
        }

        if (respuesta.status === 409) {
            throw new Error(
                detalle ||
                'No se permite realizar este cambio de estado.'
            )
        }

        throw new Error(
            detalle ||
            `No se pudo conectar con pedidos (HTTP ${respuesta.status}).`
        )
    }

    return respuesta.status === 204
        ? null
        : respuesta.json()
}

// CREAR PEDIDO
// Permitido para CLIENTE y ADMIN.
export function crearPedido(
    datos,
    token,
    onSessionRenewed
) {
    return solicitarPedidos(
        '',
        token,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(datos),
        },
        onSessionRenewed
    )
}

// CONSULTAR PEDIDOS DEL USUARIO AUTENTICADO
// Permitido para CLIENTE y ADMIN.
export function consultarMisPedidos(
    token,
    onSessionRenewed
) {
    return solicitarPedidos(
        '/mis-pedidos',
        token,
        {
            method: 'GET',
        },
        onSessionRenewed
    )
}

// CONSULTAR TODOS LOS PEDIDOS
// Permitido para ADMIN y VENDEDOR.
export function consultarTodosPedidos(
    token,
    onSessionRenewed
) {
    return solicitarPedidos(
        '',
        token,
        {
            method: 'GET',
        },
        onSessionRenewed
    )
}

// ACTUALIZAR ESTADO DE UN PEDIDO
// Permitido para ADMIN y VENDEDOR.
export function actualizarEstadoPedido(
    id,
    estado,
    token,
    onSessionRenewed
) {
    const pedidoId = Number(id)

    if (!Number.isSafeInteger(pedidoId) || pedidoId <= 0) {
        throw new Error('ID de pedido inválido.')
    }

    const estadosPermitidos = [
        'CREADO',
        'CONFIRMADO',
        'EN_PREPARACION',
        'LISTO',
        'ENTREGADO',
        'CANCELADO',
    ]

    if (!estadosPermitidos.includes(estado)) {
        throw new Error('Estado de pedido inválido.')
    }

    return solicitarPedidos(
        `/${pedidoId}/estado`,
        token,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ estado }),
        },
        onSessionRenewed
    )
}
