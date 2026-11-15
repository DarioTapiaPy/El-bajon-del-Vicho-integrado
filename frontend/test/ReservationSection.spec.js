import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import ReservationSection from '../src/components/ReservationSection.jsx'

describe('ReservationSection - API de reservas', () => {
    let container
    let root
    let fetchSpy

    const cliente = {
        userEmail: 'cliente@correo.cl',
        authToken: 'token-jwt-de-prueba',
    }

    function fechaDentroDelPlazo() {
        const fecha = new Date()
        fecha.setDate(fecha.getDate() + 2)
        const anio = fecha.getFullYear()
        const mes = String(fecha.getMonth() + 1).padStart(2, '0')
        const dia = String(fecha.getDate()).padStart(2, '0')
        return `${anio}-${mes}-${dia}`
    }

    function cambiarValor(elemento, valor) {
        const prototipo = elemento instanceof HTMLSelectElement
            ? HTMLSelectElement.prototype
            : HTMLInputElement.prototype
        const setter = Object.getOwnPropertyDescriptor(prototipo, 'value').set

        act(() => {
            setter.call(elemento, valor)
            elemento.dispatchEvent(new Event('input', { bubbles: true }))
            elemento.dispatchEvent(new Event('change', { bubbles: true }))
        })
    }

    function completarFormulario() {
        cambiarValor(container.querySelector('#people'), '4')
        cambiarValor(container.querySelector('#date'), fechaDentroDelPlazo())
        cambiarValor(container.querySelector('#time'), '20:30')
        cambiarValor(container.querySelector('#branch'), 'Sucursal Viña del Mar')
    }

    async function enviarFormulario() {
        await act(async () => {
            container.querySelector('form').dispatchEvent(
                new Event('submit', { bubbles: true, cancelable: true }),
            )
        })
    }

    async function pulsarMisReservas() {
        const boton = Array.from(container.querySelectorAll('button'))
            .find((elemento) => elemento.textContent.includes('Ver mis reservas'))
        await act(async () => boton.click())
    }

    function responderJson(cuerpo, status = 200) {
        fetchSpy.and.returnValue(Promise.resolve({
            ok: status >= 200 && status < 300,
            status,
            json: async () => cuerpo,
        }))
    }

    function renderizar(props = cliente) {
        act(() => {
            root.render(React.createElement(ReservationSection, props))
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

    it('solicita inicio de sesión antes de crear una reserva', async () => {
        const onRequireLogin = jasmine.createSpy('onRequireLogin')
        renderizar({ userEmail: null, authToken: null, onRequireLogin })

        await enviarFormulario()

        expect(onRequireLogin).toHaveBeenCalledTimes(1)
        expect(fetchSpy).not.toHaveBeenCalled()
    })

    it('rechaza datos incompletos de un usuario autenticado', async () => {
        renderizar()

        await enviarFormulario()

        expect(container.querySelector('[role="alert"]').textContent)
            .toContain('Completa correctamente los datos')
        expect(fetchSpy).not.toHaveBeenCalled()
    })

    it('crea una reserva mediante POST y muestra la confirmación', async () => {
        responderJson({
            id: 12,
            usuarioId: 7,
            correo: 'cliente@correo.cl',
            personas: 4,
            fecha: fechaDentroDelPlazo(),
            hora: '20:30:00',
            sucursal: 'Sucursal Viña del Mar',
            estado: 'CONFIRMADA',
        }, 201)

        renderizar()
        completarFormulario()
        await enviarFormulario()

        expect(fetchSpy).toHaveBeenCalledTimes(1)
        const [url, opciones] = fetchSpy.calls.mostRecent().args
        expect(url).toBe('/reservas-api/api/v1/reservas')
        expect(opciones.method).toBe('POST')
        expect(opciones.headers.Authorization).toBe('Bearer token-jwt-de-prueba')
        const body = JSON.parse(opciones.body)
        expect(body.personas).toBe(4)
        expect(body.sucursal).toBe('Sucursal Viña del Mar')
        expect(body.usuarioId).toBeUndefined()
        expect(body.correo).toBeUndefined()
        expect(container.querySelector('[role="status"]').textContent)
            .toContain('Reserva #12 confirmada')
    })

    it('muestra el error devuelto por el servidor al crear una reserva', async () => {
        responderJson({ detail: 'No se puede guardar la reserva.' }, 400)
        renderizar()
        completarFormulario()

        await enviarFormulario()

        expect(container.querySelector('[role="alert"]').textContent)
            .toContain('No se puede guardar la reserva.')
        expect(container.querySelector('[role="status"]')).toBeNull()
    })

    it('solicita inicio de sesión antes de consultar el historial', async () => {
        const onRequireLogin = jasmine.createSpy('onRequireLogin')
        renderizar({ userEmail: null, authToken: null, onRequireLogin })

        await pulsarMisReservas()

        expect(onRequireLogin).toHaveBeenCalledTimes(1)
        expect(fetchSpy).not.toHaveBeenCalled()
    })

    it('consulta y presenta las reservas del usuario autenticado', async () => {
        responderJson([{
            id: 3,
            correo: 'cliente@correo.cl',
            personas: 2,
            fecha: fechaDentroDelPlazo(),
            hora: '19:00:00',
            sucursal: 'Sucursal Puente Alto',
            estado: 'CONFIRMADA',
        }])
        renderizar()

        await pulsarMisReservas()

        expect(fetchSpy).toHaveBeenCalledTimes(1)
        const [url, opciones] = fetchSpy.calls.mostRecent().args
        expect(url).toBe('/reservas-api/api/v1/reservas/mis-reservas')
        expect(opciones.method).toBe('GET')
        expect(opciones.headers.Authorization).toBe('Bearer token-jwt-de-prueba')
        expect(container.textContent).toContain('#3')
        expect(container.textContent).toContain('Sucursal Puente Alto')
        expect(container.textContent).toContain('CONFIRMADA')
    })

    it('indica cuando el cliente no tiene reservas', async () => {
        responderJson([])
        renderizar()

        await pulsarMisReservas()

        expect(container.textContent).toContain('Todavía no tienes reservas.')
    })

    it('no muestra reservas antiguas de otra cuenta al cambiar de usuario', async () => {
        responderJson([{
            id: 9,
            personas: 2,
            fecha: fechaDentroDelPlazo(),
            hora: '19:00:00',
            sucursal: 'Sucursal Puente Alto',
            estado: 'CONFIRMADA',
        }])
        renderizar()
        await pulsarMisReservas()
        expect(container.textContent).toContain('#9')

        renderizar({
            userEmail: 'otro@correo.cl',
            authToken: 'otro-jwt-de-prueba',
        })

        expect(container.textContent).not.toContain('#9')
    })
})
