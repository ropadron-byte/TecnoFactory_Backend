package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.GoogleCompletarRequest;
import com.factory.tecnofactory.dto.GoogleLoginRequest;
import com.factory.tecnofactory.dto.GooglePerfilPendienteResponse;
import com.factory.tecnofactory.dto.LoginRequest;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.exception.TokenGoogleInvalidoException;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.AuthService;
import com.factory.tecnofactory.service.GoogleAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    // SecurityConfig los necesita para construir el filtro JWT y el AuthenticationManager
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String JSON_LOGIN = """
            { "correo": "ana@correo.cl", "clave": "Clave12345" }
            """;

    private static final String JSON_GOOGLE = """
            { "credential": "id-token-de-google" }
            """;

    private static final String JSON_COMPLETAR = """
            {
              "registroToken": "registro-token",
              "run": 12345678, "runDv": "5", "nombre": "Ana", "apellidos": "Pérez",
              "fechaNacimiento": "1990-05-10", "direccion": "Calle 1",
              "telefono": "+56912345678", "idComuna": 13101
            }
            """;

    @Test
    void loginCorrectoDevuelve200ConToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-de-prueba", "Bearer", "ana@correo.cl", "CLIENTE"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("mal"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void loginConCorreoVacioDevuelve400ConDetalle() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"\", \"clave\": \"Clave12345\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correo").value("El correo es obligatorio"));
    }

    @Test
    void googleConCuentaVinculadaDevuelve200ConToken() throws Exception {
        when(googleAuthService.iniciarSesion(any(GoogleLoginRequest.class)))
                .thenReturn(GoogleAuthService.Resultado.completado(
                        new AuthResponse("jwt-google", "Bearer", "ana@correo.cl", "CLIENTE")));

        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(JSON_GOOGLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-google"))
                .andExpect(jsonPath("$.correo").value("ana@correo.cl"));
    }

    @Test
    void googleConCuentaNuevaDevuelve202ConTokenTemporal() throws Exception {
        when(googleAuthService.iniciarSesion(any(GoogleLoginRequest.class)))
                .thenReturn(GoogleAuthService.Resultado.pendiente(
                        new GooglePerfilPendienteResponse(true, "registro-token", "ana@correo.cl", "Ana", "Pérez")));

        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(JSON_GOOGLE))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requiereCompletarPerfil").value(true))
                .andExpect(jsonPath("$.registroToken").value("registro-token"))
                .andExpect(jsonPath("$.correo").value("ana@correo.cl"));
    }

    @Test
    void googleConCuentaExistenteConContrasenaDevuelve409() throws Exception {
        when(googleAuthService.iniciarSesion(any(GoogleLoginRequest.class)))
                .thenThrow(new ReglaNegocioException("Ya existe una cuenta con este correo. Inicia sesión con tu contraseña y vincula Google"));

        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(JSON_GOOGLE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con este correo. Inicia sesión con tu contraseña y vincula Google"));
    }

    @Test
    void googleConTokenInvalidoDevuelve401() throws Exception {
        when(googleAuthService.iniciarSesion(any(GoogleLoginRequest.class)))
                .thenThrow(new TokenGoogleInvalidoException());

        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(JSON_GOOGLE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El token de Google no es válido o expiró"));
    }

    @Test
    void googleSinCredentialDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.credential").value("El token de Google es obligatorio"));
    }

    @Test
    void completarPerfilDevuelve201ConToken() throws Exception {
        when(googleAuthService.completarPerfil(any(GoogleCompletarRequest.class)))
                .thenReturn(new AuthResponse("jwt-nuevo", "Bearer", "ana@correo.cl", "CLIENTE"));

        mockMvc.perform(post("/api/v1/auth/google/completar")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_COMPLETAR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-nuevo"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void vincularGoogleSinSesionDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/vincular")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_GOOGLE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vincularGoogleConSesionDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/vincular")
                        .with(user("ana@correo.cl").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_GOOGLE))
                .andExpect(status().isNoContent());
    }
}
