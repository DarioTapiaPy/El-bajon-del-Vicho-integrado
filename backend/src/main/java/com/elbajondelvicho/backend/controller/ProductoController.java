
package com.elbajondelvicho.backend.controller;

import com.elbajondelvicho.backend.dto.CrearProductoRequest;
import com.elbajondelvicho.backend.dto.ProductoDTO;
import com.elbajondelvicho.backend.dto.ProductoRequest;
import com.elbajondelvicho.backend.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // GET: LISTAR PRODUCTOS
    @Operation(summary = "Listar productos del catálogo")
    @GetMapping
    public List<ProductoDTO> listarProductos(
            @RequestParam(required = false) String categoria
    ) {
        if (categoria != null && !categoria.isBlank()) {
            return productoService.listarPorCategoria(categoria);
        }

        return productoService.listarTodos();
    }

    // GET: BUSCAR POR ID
    @Operation(summary = "Consultar producto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> buscarProducto(
            @PathVariable String id
    ) {
        return ResponseEntity.of(
                productoService.buscarPorId(id)
        );
    }

    // POST: CREAR PRODUCTO
    @Operation(
            summary = "Crear un producto",
            description = "Operación exclusiva del rol ADMIN"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(
            @Valid @RequestBody CrearProductoRequest request
    ) {
        ProductoDTO creado = productoService.crear(
                request.id(),
                request.datos()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(creado);
    }

    // PUT: ACTUALIZAR PRODUCTO
    @Operation(
            summary = "Actualizar un producto",
            description = "Operación exclusiva del rol ADMIN"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizarProducto(
            @PathVariable String id,
            @Valid @RequestBody ProductoRequest request
    ) {
        ProductoDTO actualizado =
                productoService.actualizar(id, request);

        return ResponseEntity.ok(actualizado);
    }

    // DELETE: ELIMINAR PRODUCTO
    @Operation(
            summary = "Eliminar un producto",
            description = "Operación exclusiva del rol ADMIN"
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable String id
    ) {
        productoService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
