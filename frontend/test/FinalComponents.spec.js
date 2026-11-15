import React, { act } from 'react'
import { createRoot } from 'react-dom/client'

import OrderSuccessModal
    from '../src/components/OrderSuccessModal.jsx'

import AdminSection
    from '../src/components/AdminSection.jsx'

import ContactSection
    from '../src/components/ContactSection.jsx'


function changeValue(element, value) {
    let prototype = HTMLInputElement.prototype

    if (element instanceof HTMLTextAreaElement) {
        prototype = HTMLTextAreaElement.prototype
    }

    const setter =
        Object.getOwnPropertyDescriptor(
            prototype,
            'value',
        ).set

    act(() => {
        setter.call(element, value)

        element.dispatchEvent(
            new Event('input', {
                bubbles: true,
            }),
        )
    })
}


describe('OrderSuccessModal', () => {
    let container
    let root

    beforeEach(() => {
        globalThis.IS_REACT_ACT_ENVIRONMENT = true

        container = document.createElement('div')
        document.body.appendChild(container)

        root = createRoot(container)
    })

    afterEach(() => {
        act(() => {
            root.unmount()
        })

        container.remove()
    })

    it('no debe mostrarse cuando isOpen es false', () => {
        const onContinue =
            jasmine.createSpy('onContinue')

        act(() => {
            root.render(
                React.createElement(
                    OrderSuccessModal,
                    {
                        isOpen: false,
                        orderNumber: 1234,
                        itemCount: 1,
                        total: 8990,
                        deliveryType: 'pickup',
                        branch: 'Puente Alto',
                        address: '',
                        userEmail: 'cliente@correo.cl',
                        onContinue,
                    },
                ),
            )
        })

        expect(container.innerHTML).toBe('')
    })

    it('debe mostrar correctamente un pedido con retiro', () => {
        const onContinue =
            jasmine.createSpy('onContinue')

        act(() => {
            root.render(
                React.createElement(
                    OrderSuccessModal,
                    {
                        isOpen: true,
                        orderNumber: 4567,
                        itemCount: 1,
                        total: 8990,
                        deliveryType: 'pickup',
                        branch: 'Puente Alto',
                        address: '',
                        userEmail: 'cliente@correo.cl',
                        onContinue,
                    },
                ),
            )
        })

        expect(container.textContent).toContain(
            '4567',
        )

        expect(container.textContent).toContain(
            '1 producto',
        )

        expect(container.textContent).toContain(
            'Puente Alto',
        )

        expect(container.textContent).toContain(
            'cliente@correo.cl',
        )
    })

    it('debe mostrar correctamente un pedido con despacho', () => {
        const onContinue =
            jasmine.createSpy('onContinue')

        act(() => {
            root.render(
                React.createElement(
                    OrderSuccessModal,
                    {
                        isOpen: true,
                        orderNumber: 7890,
                        itemCount: 2,
                        total: 19480,
                        deliveryType: 'delivery',
                        branch: '',
                        address: 'Los Aromos 456',
                        userEmail: 'cliente@correo.cl',
                        onContinue,
                    },
                ),
            )
        })

        expect(container.textContent).toContain(
            '2 productos',
        )

        expect(container.textContent).toContain(
            'Los Aromos 456',
        )

        expect(container.textContent).toContain(
            'cliente@correo.cl',
        )
    })

    it('debe ejecutar onContinue al seguir comprando', () => {
        const onContinue =
            jasmine.createSpy('onContinue')

        act(() => {
            root.render(
                React.createElement(
                    OrderSuccessModal,
                    {
                        isOpen: true,
                        orderNumber: 1111,
                        itemCount: 1,
                        total: 8990,
                        deliveryType: 'pickup',
                        branch: 'Puente Alto',
                        address: '',
                        userEmail: 'cliente@correo.cl',
                        onContinue,
                    },
                ),
            )
        })

        const button =
            container.querySelector('button')

        act(() => {
            button.click()
        })

        expect(onContinue).toHaveBeenCalledTimes(1)
    })
})


describe('AdminSection', () => {
    let container
    let root

    const products = [
        {
            id: 'prod-1',
            name: 'COMBO ADMIN',
            description: 'Descripcion original',
            price: 8990,
            stock: true,
        },
    ]

    function renderAdmin(
        onSaveProduct,
        onToggleStock,
        onLogout = () => {},
    ) {
        act(() => {
            root.render(
                React.createElement(
                    AdminSection,
                    {
                        products,
                        onSaveProduct,
                        onToggleStock,
                        onLogout,
                    },
                ),
            )
        })
    }

    beforeEach(() => {
        globalThis.IS_REACT_ACT_ENVIRONMENT = true

        localStorage.clear()
        sessionStorage.clear()

        container = document.createElement('div')
        document.body.appendChild(container)

        root = createRoot(container)
    })

    afterEach(() => {
        act(() => {
            root.unmount()
        })

        container.remove()

        localStorage.clear()
        sessionStorage.clear()
    })

    it('debe mostrar la gestion de productos (la sesion se controla en App)', () => {
        const onSaveProduct = jasmine.createSpy('onSaveProduct')
        const onToggleStock = jasmine.createSpy('onToggleStock')

        renderAdmin(onSaveProduct, onToggleStock)

        expect(container.textContent).toContain('Gestión de Productos')
        expect(container.textContent).toContain('COMBO ADMIN')
        expect(container.querySelector('#adminEmail')).toBeNull()
        expect(container.querySelector('#adminPassword')).toBeNull()
    })

    it('debe notificar el cierre de sesion administrativa', () => {
        const onSaveProduct = jasmine.createSpy('onSaveProduct')
        const onToggleStock = jasmine.createSpy('onToggleStock')
        const onLogout = jasmine.createSpy('onLogout')

        renderAdmin(onSaveProduct, onToggleStock, onLogout)

        const logoutButton = Array.from(
            container.querySelectorAll('button'),
        ).find((button) =>
            button.textContent.includes('Cerrar sesión admin'),
        )

        expect(logoutButton).not.toBeUndefined()

        act(() => {
            logoutButton.click()
        })

        expect(onLogout).toHaveBeenCalledTimes(1)
    })

    it('debe ejecutar onToggleStock con el producto correcto', () => {
        sessionStorage.setItem(
            'adminAuth',
            'true',
        )

        const onSaveProduct =
            jasmine.createSpy('onSaveProduct')

        const onToggleStock =
            jasmine.createSpy('onToggleStock')

        renderAdmin(
            onSaveProduct,
            onToggleStock,
        )

        const buttons =
            Array.from(
                container.querySelectorAll('button'),
            )

        const stockButton =
            buttons.find(
                (button) =>
                    button.textContent.includes('Stock'),
            )

        act(() => {
            stockButton.click()
        })

        expect(
            onToggleStock,
        ).toHaveBeenCalledTimes(1)

        expect(
            onToggleStock,
        ).toHaveBeenCalledWith('prod-1')
    })

    it('debe guardar una descripción y precio válidos', async () => {
        sessionStorage.setItem(
            'adminAuth',
            'true',
        )

        const onSaveProduct =
            jasmine.createSpy('onSaveProduct')

        const onToggleStock =
            jasmine.createSpy('onToggleStock')

        const alertSpy =
            spyOn(window, 'alert')

        renderAdmin(
            onSaveProduct,
            onToggleStock,
        )

        const description =
            container.querySelector('textarea')

        const price =
            container.querySelector(
                'input[type="number"]',
            )

        changeValue(
            description,
            '  Nueva descripcion del producto  ',
        )

        changeValue(
            price,
            '9990',
        )

        const buttons =
            Array.from(
                container.querySelectorAll('button'),
            )

        const saveButton =
            buttons.find(
                (button) =>
                    button.textContent.trim() === 'Guardar',
            )

        // onSaveProduct es asincrono: esperar a que termine antes
        // de verificar el mensaje de exito.
        await act(async () => {
            saveButton.click()
        })

        expect(
            onSaveProduct,
        ).toHaveBeenCalledTimes(1)

        expect(
            onSaveProduct,
        ).toHaveBeenCalledWith(
            'prod-1',
            'Nueva descripcion del producto',
            9990,
        )

        expect(alertSpy).toHaveBeenCalled()
    })

    it('debe consultar y cancelar una reserva desde la API', async () => {
        const reserva = {
            id: 7,
            correo: 'cliente@correo.cl',
            sucursal: 'Sucursal Puente Alto',
            fecha: '2026-11-20',
            hora: '20:00:00',
            personas: 2,
            estado: 'CONFIRMADA',
        }

        const peticiones = spyOn(globalThis, 'fetch').and.callFake(
            (url, options = {}) => {
                if (url.endsWith('/api/v1/reservas') && options.method === 'GET') {
                    return Promise.resolve({
                        ok: true,
                        status: 200,
                        json: async () => [reserva],
                    })
                }
                if (url.endsWith('/api/v1/reservas/7') && options.method === 'DELETE') {
                    return Promise.resolve({ ok: true, status: 204 })
                }
                throw new Error(`Petición inesperada: ${options.method} ${url}`)
            },
        )

        spyOn(window, 'confirm').and.returnValue(true)

        await act(async () => {
            root.render(React.createElement(AdminSection, {
                products,
                onSaveProduct: jasmine.createSpy('onSaveProduct'),
                onToggleStock: jasmine.createSpy('onToggleStock'),
                onLogout: () => {},
                authToken: 'token-de-prueba',
            }))
        })

        expect(container.textContent).toContain('cliente@correo.cl')
        expect(container.textContent).toContain('CONFIRMADA')

        const boton = Array.from(container.querySelectorAll('button'))
            .find((b) => b.textContent.includes('Cancelar reserva'))
        expect(boton).toBeDefined()

        await act(async () => { boton.click() })

        expect(peticiones.calls.allArgs().some(([url, options]) =>
            url.endsWith('/api/v1/reservas/7') && options.method === 'DELETE'
        )).toBeTrue()
        expect(container.textContent).toContain('CANCELADA')
        expect(localStorage.getItem('bajon_db_reservas')).toBeNull()
    })

})


describe('ContactSection', () => {
    let container
    let root

    beforeEach(() => {
        globalThis.IS_REACT_ACT_ENVIRONMENT = true

        container = document.createElement('div')
        document.body.appendChild(container)

        root = createRoot(container)
    })

    afterEach(() => {
        act(() => {
            root.unmount()
        })

        container.remove()
    })

    it('debe actualizar los datos del formulario de contacto', () => {
        act(() => {
            root.render(
                React.createElement(ContactSection),
            )
        })

        const name =
            container.querySelector('#contactName')

        const email =
            container.querySelector('#contactEmail')

        const message =
            container.querySelector('#contactMessage')

        changeValue(name, 'Jose')
        changeValue(email, 'jose@correo.cl')
        changeValue(
            message,
            'Necesito ayuda con mi pedido',
        )

        expect(name.value).toBe('Jose')

        expect(email.value).toBe(
            'jose@correo.cl',
        )

        expect(message.value).toBe(
            'Necesito ayuda con mi pedido',
        )
    })
})