package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.CantidadRequest;
import com.factory.tecnofactory.dto.CarritoItemRequest;
import com.factory.tecnofactory.dto.CarritoResponse;
import com.factory.tecnofactory.service.CarritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Todas las rutas son del carrito de quien tiene la sesión iniciada. */
@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@Tag(name = "Carrito", description = "Carrito de compras del usuario con sesión")
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    @Operation(summary = "Ver mi carrito")
    public CarritoResponse ver(Authentication autenticacion) {
        return carritoService.obtener(autenticacion.getName());
    }

    @PostMapping("/items")
    @Operation(summary = "Agregar un producto (si ya está, suma la cantidad)")
    public CarritoResponse agregar(@Valid @RequestBody CarritoItemRequest request,
                                   Authentication autenticacion) {
        return carritoService.agregar(autenticacion.getName(), request);
    }

    @PutMapping("/items/{idProducto}")
    @Operation(summary = "Cambiar la cantidad de un producto del carrito")
    public CarritoResponse cambiarCantidad(@PathVariable Long idProducto,
                                           @Valid @RequestBody CantidadRequest request,
                                           Authentication autenticacion) {
        return carritoService.cambiarCantidad(autenticacion.getName(), idProducto, request.cantidad());
    }

    @DeleteMapping("/items/{idProducto}")
    @Operation(summary = "Quitar un producto del carrito")
    public CarritoResponse quitar(@PathVariable Long idProducto, Authentication autenticacion) {
        return carritoService.quitar(autenticacion.getName(), idProducto);
    }

    @DeleteMapping
    @Operation(summary = "Vaciar el carrito")
    public ResponseEntity<Void> vaciar(Authentication autenticacion) {
        carritoService.vaciar(autenticacion.getName());
        return ResponseEntity.noContent().build();
    }
}
