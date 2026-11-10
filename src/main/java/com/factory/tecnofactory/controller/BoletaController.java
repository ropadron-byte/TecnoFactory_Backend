package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.BoletaResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.service.BoletaService;
import com.factory.tecnofactory.util.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/boletas")
@RequiredArgsConstructor
@Tag(name = "Boletas", description = "Compras realizadas")
public class BoletaController {

    private final BoletaService boletaService;

    @PostMapping("/checkout")
    @Operation(summary = "Comprar: convierte mi carrito en una boleta, descuenta el stock y vacía el carrito")
    public ResponseEntity<BoletaResponse> checkout(Authentication autenticacion) {
        BoletaResponse creada = boletaService.generarDesdeCarrito(autenticacion.getName());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/boletas/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/mias")
    @Operation(summary = "Mis boletas, de la más nueva a la más antigua")
    public PageResponse<BoletaResponse> mias(Authentication autenticacion, Pageable pageable) {
        return boletaService.listarMias(autenticacion.getName(), pageable);
    }

    @GetMapping
    @Operation(summary = "Todas las boletas (administrador y vendedor)")
    public PageResponse<BoletaResponse> todas(Pageable pageable) {
        return boletaService.listarTodas(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver una boleta (el personal ve cualquiera; un cliente solo las suyas)")
    public BoletaResponse buscarPorId(@PathVariable Long id, Authentication autenticacion) {
        return boletaService.buscarPorId(id, autenticacion.getName(), Roles.esPersonal(autenticacion));
    }

    @PatchMapping("/{id}/anular")
    @Operation(summary = "Anular una boleta y devolver el stock (solo administrador)")
    public BoletaResponse anular(@PathVariable Long id) {
        return boletaService.anular(id);
    }
}
