package com.elbajondelvicho.pedidos.service;

import com.elbajondelvicho.pedidos.client.CatalogoClient;
import com.elbajondelvicho.pedidos.entity.EstadoPedido;
import com.elbajondelvicho.pedidos.entity.Pedido;
import com.elbajondelvicho.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private CatalogoClient catalogoClient;

    private PedidoService pedidoService;

    @BeforeEach
    void preparar() {
        pedidoService = new PedidoService(pedidoRepository, catalogoClient);
    }

    @Test
    void actualizarEstado_deCreadoAConfirmado_guardaPedido() {
        // Preparar un pedido que todavía no se ha confirmado.
        Pedido pedido = new Pedido();
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());

        when(pedidoRepository.findConDetallesById(1L))
                .thenReturn(Optional.of(pedido));
        when(pedidoRepository.saveAndFlush(pedido))
                .thenReturn(pedido);

        // Ejecutar la transición válida.
        Pedido resultado = pedidoService.actualizarEstado(
                1L, EstadoPedido.CONFIRMADO
        );

        // Verificar que el estado cambió y se solicitó guardar en el repositorio.
        assertSame(pedido, resultado);
        assertEquals(EstadoPedido.CONFIRMADO, resultado.getEstado());
        verify(pedidoRepository).saveAndFlush(pedido);
    }

    @Test
    void actualizarEstado_deCreadoAEntregado_rechazaCon409YSinGuardar() {
        // No se permite saltar directamente de CREADO a ENTREGADO.
        Pedido pedido = new Pedido();
        when(pedidoRepository.findConDetallesById(2L))
                .thenReturn(Optional.of(pedido));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pedidoService.actualizarEstado(2L, EstadoPedido.ENTREGADO)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoPedido.CREADO, pedido.getEstado());
        verify(pedidoRepository, never()).saveAndFlush(pedido);
    }

    @Test
    void actualizarEstado_deEntregadoACancelado_rechazaCon409YSinGuardar() {
        // ENTREGADO es un estado terminal: no puede pasar a CANCELADO.
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.ENTREGADO);

        when(pedidoRepository.findConDetallesById(3L))
                .thenReturn(Optional.of(pedido));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pedidoService.actualizarEstado(3L, EstadoPedido.CANCELADO)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
        verify(pedidoRepository, never()).saveAndFlush(pedido);
    }

    @Test
    void actualizarEstado_deCanceladoAConfirmado_rechazaCon409YSinGuardar() {
        // CANCELADO es terminal: el pedido no puede reactivarse.
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.CANCELADO);

        when(pedidoRepository.findConDetallesById(4L))
                .thenReturn(Optional.of(pedido));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pedidoService.actualizarEstado(4L, EstadoPedido.CONFIRMADO)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
        verify(pedidoRepository, never()).saveAndFlush(pedido);
    }

    @Test
    void actualizarEstado_pedidoInexistente_rechazaCon404YSinGuardar() {
        // El repositorio no encuentra el ID solicitado.
        when(pedidoRepository.findConDetallesById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pedidoService.actualizarEstado(999L, EstadoPedido.CONFIRMADO)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(pedidoRepository).findConDetallesById(999L);
        verify(pedidoRepository, never()).saveAndFlush(any(Pedido.class));
    }

    @Test
    void actualizarEstado_conIdInvalido_rechazaCon400SinGuardar() {
        // ID cero es invalido. El servicio debe rechazarlo antes de consultar datos.
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pedidoService.actualizarEstado(0L, EstadoPedido.CONFIRMADO)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(pedidoRepository, catalogoClient);
    }

    @Test
    void actualizarEstado_conEstadoNulo_rechazaCon400SinConsultar() {
        // El nuevo estado es obligatorio, por lo que se valida antes de consultar.
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pedidoService.actualizarEstado(1L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(pedidoRepository, catalogoClient);
    }

    @Test
    void actualizarEstado_conMismoEstadoEntregado_retornaSinGuardar() {
        // Solicitar nuevamente un estado existente debe ser una operacion sin cambios.
        // Incluso ENTREGADO (estado terminal) permite repetir el mismo estado.
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.ENTREGADO);

        when(pedidoRepository.findConDetallesById(5L))
                .thenReturn(Optional.of(pedido));

        Pedido resultado = pedidoService.actualizarEstado(5L, EstadoPedido.ENTREGADO);

        assertSame(pedido, resultado);
        assertEquals(EstadoPedido.ENTREGADO, resultado.getEstado());
        verify(pedidoRepository).findConDetallesById(5L);
        verify(pedidoRepository, never()).saveAndFlush(any(Pedido.class));
        verifyNoInteractions(catalogoClient);
    }
}
