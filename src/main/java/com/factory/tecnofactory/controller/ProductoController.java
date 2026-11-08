package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.dto.ProductoRequest;
import com.factory.tecnofactory.dto.ProductoResponse;
import com.factory.tecnofactory.service.ProductoService;
import com.factory.tecnofactory.util.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo de productos")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Catálogo de productos activos, paginado (público). Filtro opcional por categoría")
    public PageResponse<ProductoResponse> listar(@RequestParam(required = false) Long categoriaId,
                                                 Pageable pageable) {
        return productoService.listarActivos(categoriaId, pageable);
    }

    @GetMapping("/ofertas")
    @SecurityRequirements
    @Operation(summary = "Productos con descuento (público)")
    public PageResponse<ProductoResponse> ofertas(Pageable pageable) {
        return productoService.listarOfertas(pageable);
    }

    @GetMapping("/admin")
    @Operation(summary = "Todos los productos, incluidos los desactivados (administrador y vendedor)")
    public PageResponse<ProductoResponse> listarTodos(Pageable pageable) {
        return productoService.listarTodos(pageable);
    }

    @GetMapping("/criticos")
    @Operation(summary = "Productos con stock crítico (administrador y vendedor)")
    public List<ProductoResponse> criticos() {
        return productoService.listarCriticos();
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Detalle de un producto (público; los desactivados solo los ve el personal)")
    public ProductoResponse buscarPorId(@PathVariable Long id, Authentication autenticacion) {
        return productoService.buscarPorId(id, Roles.esPersonal(autenticacion));
    }

    @PostMapping
    @Operation(summary = "Crear un producto (solo administrador)")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un producto (solo administrador)")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un producto: deja de mostrarse, pero no se borra (solo administrador)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Volver a activar un producto (solo administrador)")
    public ProductoResponse activar(@PathVariable Long id) {
        return productoService.activar(id);
    }
}
