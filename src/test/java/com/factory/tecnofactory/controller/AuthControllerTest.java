package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.LoginRequest;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.AuthService;
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

    // SecurityConfig los necesita para construir el filtro JWT y el AuthenticationManager
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String JSON_LOGIN = """
            { "correo": "ana@correo.cl", "clave": "Clave12345" }
            """;

    @Test
    void loginCorrectoDevuelve200ConToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-de-prueba", "Bearer", "ana@correo.cl", "CLIENTE"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("mal"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void loginConCorreoVacioDevuelve400ConDetalle() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"\", \"clave\": \"Clave12345\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correo").value("El correo es obligatorio"));
    }
}
