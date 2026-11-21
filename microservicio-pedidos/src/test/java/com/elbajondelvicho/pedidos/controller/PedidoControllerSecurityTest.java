package com.elbajondelvicho.pedidos.controller;

import com.elbajondelvicho.pedidos.dto.LineaPedidoRequest;
import com.elbajondelvicho.pedidos.entity.DetallePedido;
import com.elbajondelvicho.pedidos.entity.EstadoPedido;
import com.elbajondelvicho.pedidos.entity.Pedido;
import com.elbajondelvicho.pedidos.security.SecurityConfig;
import com.elbajondelvicho.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas web de autorizacion de pedidos con JWT simulados.
 * No necesitan MySQL ni JWT_SECRET: no se generan tokens reales.
 */
@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private PedidoService pedidoService;

    // Necesario para construir la cadena de seguridad OAuth2.
    // Las pruebas usan jwt() en vez de decodificar un token real.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void cliente_noPuedeConsultarTodosLosPedidos_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cliente@ejemplo.test")
                                        .claim("userId", 10L)
                                        .claim("roles", List.of("CLIENTE")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pedidoService);
    }

    @Test
    void vendedor_puedeConsultarTodosLosPedidos_devuelve200() throws Exception {
        when(pedidoService.obtenerTodosLosPedidos()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pedidos")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("vendedor@ejemplo.test")
                                        .claim("userId", 20L)
                                        .claim("roles", List.of("VENDEDOR")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(pedidoService).obtenerTodosLosPedidos();
    }

    @Test
    void cliente_puedeConsultarSusPedidos_devuelve200() throws Exception {
        // El usuario del JWT es el 10. Solo se solicitan sus pedidos.
        Pedido pedidoPropio = new Pedido();
        pedidoPropio.setUsuarioId(10L);
        pedidoPropio.setEstado(EstadoPedido.CONFIRMADO);

        when(pedidoService.obtenerPedidosDeUsuario(10L))
                .thenReturn(List.of(pedidoPropio));

        mockMvc.perform(get("/api/v1/pedidos/mis-pedidos")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cliente@ejemplo.test")
                                        .claim("userId", 10L)
                                        .claim("roles", List.of("CLIENTE")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuarioId").value(10))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADO"));

        // El controlador toma el ID del JWT, no de un parametro del cliente.
        verify(pedidoService).obtenerPedidosDeUsuario(10L);
        verify(pedidoService, never()).obtenerTodosLosPedidos();
    }

    @Test
    void cliente_noPuedeActualizarEstado_devuelve403() throws Exception {
        mockMvc.perform(patch("/api/v1/pedidos/2/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADO\"}")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cliente@ejemplo.test")
                                        .claim("userId", 10L)
                                        .claim("roles", List.of("CLIENTE")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isForbidden());

        // Si no tiene permisos, el controlador no debe ejecutar el servicio.
        verifyNoInteractions(pedidoService);
    }

    @Test
    void vendedor_puedeActualizarEstado_devuelve200() throws Exception {
        // El servicio esta simulado. Solo verificamos seguridad, ruta y respuesta HTTP.
        Pedido pedidoActualizado = new Pedido();
        pedidoActualizado.setUsuarioId(10L);
        pedidoActualizado.setEstado(EstadoPedido.CONFIRMADO);

        when(pedidoService.actualizarEstado(2L, EstadoPedido.CONFIRMADO))
                .thenReturn(pedidoActualizado);

        mockMvc.perform(patch("/api/v1/pedidos/2/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADO\"}")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("vendedor@ejemplo.test")
                                        .claim("userId", 20L)
                                        .claim("roles", List.of("VENDEDOR")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        // Un VENDEDOR autorizado debe alcanzar el servicio con los datos correctos.
        verify(pedidoService).actualizarEstado(2L, EstadoPedido.CONFIRMADO);
    }

    @Test
    void vendedor_noPuedeConsultarMisPedidos_devuelve403() throws Exception {
        // VENDEDOR puede consultar todos los pedidos, pero no la ruta personal.
        mockMvc.perform(get("/api/v1/pedidos/mis-pedidos")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("vendedor@ejemplo.test")
                                        .claim("userId", 20L)
                                        .claim("roles", List.of("VENDEDOR")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isForbidden());

        // La peticion debe bloquearse antes de acceder al servicio.
        verifyNoInteractions(pedidoService);
    }

    @Test
    void sinJwt_noPuedeConsultarTodosLosPedidos_devuelve401() throws Exception {
        // Ninguna persona sin autenticar debe consultar todos los pedidos.
        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isUnauthorized());

        // La peticion debe detenerse antes de entrar en el servicio.
        verifyNoInteractions(pedidoService);
    }

    @Test
    void vendedor_noPuedeCrearPedidos_devuelve403() throws Exception {
        // VENDEDOR puede consultar y actualizar estados, pero no crear pedidos.
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":\"prod-1\",\"cantidad\":1}]}")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("vendedor@ejemplo.test")
                                        .claim("userId", 20L)
                                        .claim("roles", List.of("VENDEDOR")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isForbidden());

        // Una peticion rechazada por Spring Security nunca llega a PedidoService.
        verifyNoInteractions(pedidoService);
    }

    @Test
    void cliente_puedeCrearPedido_devuelve201() throws Exception {
        // Pedido simulado: prueba autorizacion, datos del JWT y respuesta del controlador.
        Pedido pedidoCreado = new Pedido();
        pedidoCreado.setUsuarioId(10L);
        pedidoCreado.agregarDetalle(new DetallePedido(
                "prod-1", "Producto de prueba", 2, new BigDecimal("2500.00")
        ));
        pedidoCreado.calcularTotal();

        List<LineaPedidoRequest> items = List.of(
                new LineaPedidoRequest("prod-1", 2)
        );
        when(pedidoService.crearPedido(10L, items)).thenReturn(pedidoCreado);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":\"prod-1\",\"cantidad\":2}]}")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cliente@ejemplo.test")
                                        .claim("userId", 10L)
                                        .claim("roles", List.of("CLIENTE")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(10))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].productoId").value("prod-1"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2));

        // El usuario viene del JWT, no del cuerpo enviado por el cliente.
        verify(pedidoService).crearPedido(10L, items);
    }

    @Test
    void cliente_noPuedeConsultarPedidosDeOtroUsuarioManipulandoParametro() throws Exception {
        // El JWT identifica al CLIENTE 10. Aunque intente solicitar los pedidos
        // del usuario 99 mediante un parametro, solo debe consultar los propios.
        Pedido pedidoPropio = new Pedido();
        pedidoPropio.setUsuarioId(10L);
        pedidoPropio.setEstado(EstadoPedido.CREADO);

        when(pedidoService.obtenerPedidosDeUsuario(10L))
                .thenReturn(List.of(pedidoPropio));

        mockMvc.perform(get("/api/v1/pedidos/mis-pedidos")
                        .param("usuarioId", "99")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cliente@ejemplo.test")
                                        .claim("userId", 10L)
                                        .claim("roles", List.of("CLIENTE")))
                                .authorities(token -> new ArrayList<GrantedAuthority>(
                                        jwtAuthenticationConverter.convert(token).getAuthorities()
                                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuarioId").value(10));

        // El parametro externo se ignora: la identidad valida viene del JWT.
        verify(pedidoService).obtenerPedidosDeUsuario(10L);
        verify(pedidoService, never()).obtenerPedidosDeUsuario(99L);
        verify(pedidoService, never()).obtenerTodosLosPedidos();
    }
}
