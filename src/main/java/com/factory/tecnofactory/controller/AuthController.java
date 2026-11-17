package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.GoogleCompletarRequest;
import com.factory.tecnofactory.dto.GoogleLoginRequest;
import com.factory.tecnofactory.dto.LoginRequest;
import com.factory.tecnofactory.service.AuthService;
import com.factory.tecnofactory.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT y con Google")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    // @SecurityRequirements vacío: quita el candado de Swagger en esta ruta pública
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión y obtener un token JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión con el ID token de Google",
            description = "Si la cuenta ya existe responde 200 con el JWT. "
                    + "Si es nueva responde 202 y un token temporal: la cuenta todavía no se crea, "
                    + "el usuario debe enviar todos sus datos a /google/completar. "
                    + "Si el correo ya existe con contraseña responde 409: hay que iniciar sesión y vincular Google.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada; devuelve el JWT"),
            @ApiResponse(responseCode = "202", description = "Falta completar el perfil"),
            @ApiResponse(responseCode = "401", description = "Token de Google inválido o cuenta desactivada"),
            @ApiResponse(responseCode = "409", description = "El correo ya existe con contraseña")
    })
    public ResponseEntity<?> loginConGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleAuthService.Resultado resultado = googleAuthService.iniciarSesion(request);
        if (resultado.requierePerfil()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado.perfilPendiente());
        }
        return ResponseEntity.ok(resultado.auth());
    }

    @PostMapping("/google/completar")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Completar el perfil de una cuenta nueva de Google",
            description = "Recibe el token temporal de /google y los datos que Google no entrega.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada; devuelve el JWT"),
            @ApiResponse(responseCode = "401", description = "Token de registro inválido o expirado"),
            @ApiResponse(responseCode = "409", description = "RUN o correo ya registrado")
    })
    public AuthResponse completarPerfil(@Valid @RequestBody GoogleCompletarRequest request) {
        return googleAuthService.completarPerfil(request);
    }

    @PostMapping("/google/vincular")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Vincular Google a la cuenta con sesión iniciada",
            description = "Exige que el correo de Google coincida con el correo de la cuenta autenticada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Google vinculado"),
            @ApiResponse(responseCode = "401", description = "Sin sesión o token de Google inválido"),
            @ApiResponse(responseCode = "409", description = "El correo no coincide o ya está vinculado")
    })
    public void vincular(Authentication autenticacion, @Valid @RequestBody GoogleLoginRequest request) {
        googleAuthService.vincular(autenticacion.getName(), request);
    }
}
