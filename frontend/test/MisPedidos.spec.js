
import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import MisPedidos from '../src/components/MisPedidos.jsx'

describe('MisPedidos - integración con API real de pedidos', () => {
    let container
    let root
    let fetchSpy

    function responderJson(cuerpo, status = 200) {
        fetchSpy.and.returnValue(Promise.resolve({
            ok: status >= 200 && status < 300,
            status,
            json: async () => cuerpo,
        }))
    }

    async function renderizar(token = 'token-prueba') {
        await act(async () => {
            root.render(React.createElement(MisPedidos, {
                authToken: token,
                onClose: jasmine.createSpy('onClose'),
            }))
        })
    }

    beforeEach(() => {
        globalThis.IS_REACT_ACT_ENVIRONMENT = true
        container = document.createElement('div')
        document.body.appendChild(container)
        root = createRoot(container)
        fetchSpy = spyOn(globalThis, 'fetch')
    })

    afterEach(() => {
        act(() => root.unmount())
        container.remove()
    })

    it('consulta /mis-pedidos con token y muestra historial vacío', async () => {
        responderJson([])
        await renderizar()

        expect(fetchSpy).toHaveBeenCalledTimes(1)

        const [url, opciones] = fetchSpy.calls.mostRecent().args

        expect(url).toBe('/pedidos-api/api/v1/pedidos/mis-pedidos')
        expect(opciones.headers.Authorization).toBe('Bearer token-prueba')
        expect(container.textContent).toContain(
            'Todavía no tienes pedidos registrados.'
        )
    })

    it('muestra productos y estado devueltos por la API', async () => {
        responderJson([{
            id: 12,
            usuarioId: 10,
            estado: 'LISTO',
            total: 8990,
            detalles: [{
                productoId: 'prod-1',
                nombreProducto: 'Combo clásico',
                cantidad: 1,
                subtotal: 8990,
            }],
        }])

        await renderizar()

        expect(container.textContent).toContain('Pedido #12')
        expect(container.textContent).toContain('Listo para entregar')
        expect(container.textContent).toContain('Combo clásico')
        expect(container.textContent).toContain('Cantidad: 1')
    })

    it('muestra rechazo 403 sin revelar pedidos ajenos', async () => {
        responderJson({ detail: 'Sin permiso' }, 403)

        await renderizar()

        const alerta = container.querySelector('[role="alert"]')

        expect(alerta).not.toBeNull()
        expect(alerta.textContent).toContain('no tiene permisos')
        expect(container.textContent).not.toContain('Pedido #')
    })

    it('rechaza consultas sin token antes de llamar a fetch', async () => {
        await renderizar(null)

        expect(container.querySelector('[role="alert"]').textContent)
            .toContain('Debes iniciar sesión')

        expect(fetchSpy).not.toHaveBeenCalled()
    })
})
