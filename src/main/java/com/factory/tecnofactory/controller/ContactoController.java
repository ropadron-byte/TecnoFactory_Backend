package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.ContactoRequest;
import com.factory.tecnofactory.dto.ContactoResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.service.ContactoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/contactos")
@RequiredArgsConstructor
@Tag(name = "Contacto", description = "Formulario de contacto")
public class ContactoController {

    private final ContactoService contactoService;

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Enviar un contacto (público, sin sesión)")
    public ResponseEntity<ContactoResponse> crear(@Valid @RequestBody ContactoRequest request) {
        ContactoResponse creado = contactoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar contactos recibidos (ADMINISTRADOR o VENDEDOR)")
    public PageResponse<ContactoResponse> listar(@ParameterObject Pageable pageable) {
        return contactoService.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un contacto (ADMINISTRADOR o VENDEDOR)")
    public ContactoResponse buscarPorId(@PathVariable Long id) {
        return contactoService.buscarPorId(id);
    }
}
