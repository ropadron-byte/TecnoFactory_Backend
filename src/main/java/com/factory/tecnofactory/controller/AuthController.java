package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.LoginRequest;
import com.factory.tecnofactory.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT")
public class AuthController {

    private final AuthService authService;

    // @SecurityRequirements vacío: quita el candado de Swagger en esta ruta pública
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión y obtener un token JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
