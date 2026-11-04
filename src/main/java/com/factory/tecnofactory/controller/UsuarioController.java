package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Registro y consulta de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Registrar un usuario con rol CLIENTE (público)")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.registrar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PostMapping("/vendedores")
    @Operation(summary = "Crear un usuario con rol VENDEDOR (solo administrador)")
    public ResponseEntity<UsuarioResponse> crearVendedor(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crearVendedor(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/usuarios/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/me")
    @Operation(summary = "Ver mi propio perfil (cualquier usuario con sesión)")
    public UsuarioResponse miPerfil(Authentication autenticacion) {
        return usuarioService.buscarPorCorreo(autenticacion.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un usuario por id (solo administrador)")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }
}
