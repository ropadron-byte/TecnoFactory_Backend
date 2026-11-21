package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.dto.PerfilRequest;
import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Registro, consulta y perfil de usuarios")
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
                .path("/api/v1/usuarios/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios (administrador o vendedor, solo lectura)")
    public PageResponse<UsuarioResponse> listar(@ParameterObject Pageable pageable) {
        return usuarioService.listar(pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "Ver mi propio perfil (cualquier usuario con sesión)")
    public UsuarioResponse miPerfil(Authentication autenticacion) {
        return usuarioService.buscarPorCorreo(autenticacion.getName());
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar mis datos personales (cualquier usuario con sesión)",
            description = "El correo no se puede cambiar: está ligado a la cuenta de Google o al registro original.")
    public UsuarioResponse actualizarMiPerfil(Authentication autenticacion,
                                              @Valid @RequestBody PerfilRequest request) {
        return usuarioService.actualizarPerfil(autenticacion.getName(), request);
    }

    @PatchMapping("/me/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar mi propia cuenta (solo cliente)")
    public void desactivarMiCuenta(Authentication autenticacion) {
        usuarioService.desactivarMiCuenta(autenticacion.getName());
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar la cuenta de un vendedor (solo administrador)")
    public UsuarioResponse desactivarVendedor(@PathVariable Long id) {
        return usuarioService.desactivarVendedor(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar la cuenta de un vendedor (solo administrador)")
    public void eliminarVendedor(@PathVariable Long id) {
        usuarioService.eliminarVendedor(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un usuario por id (solo administrador)")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }
}
