package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    // SecurityConfig los necesita para construir el filtro JWT y el AuthenticationManager
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String JSON_VALIDO = """
            {
              "run": 12345678, "runDv": "5", "nombre": "Ana", "apellidos": "Pérez",
              "correo": "ana@correo.cl", "clave": "Clave12345",
              "fechaNacimiento": "1990-05-20", "direccion": "Calle Falsa 123",
              "telefono": "+56912345678", "idComuna": 13101
            }
            """;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return user("admin@correo.cl").roles("ADMINISTRADOR");
    }

    private static UsuarioResponse respuesta() {
        return new UsuarioResponse(7L, 12345678, "5", "Ana", "Pérez", "ana@correo.cl",
                LocalDate.of(1990, 5, 20), "Calle Falsa 123", "+56912345678",
                true, "CLIENTE", "Santiago", "Metropolitana");
    }

    @Test
    void registrarDevuelve201ConLocationYSinClave() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/usuarios/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.tipoUsuario").value("CLIENTE"))
                .andExpect(jsonPath("$.region").value("Metropolitana"))
                .andExpect(jsonPath("$.clave").doesNotExist())
                .andExpect(jsonPath("$.contrasenia").doesNotExist());
    }

    @Test
    void registrarConDigitoIncorrectoDevuelve400ConDetallePorCampo() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO.replace("\"runDv\": \"5\"", "\"runDv\": \"9\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos inválidos"))
                .andExpect(jsonPath("$.detalles.runDv").exists());
        verify(usuarioService, never()).registrar(any());
    }

    @Test
    void registrarConJsonRotoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{\"run\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la petición no es un JSON válido"));
    }

    @Test
    void registrarConCorreoRepetidoDevuelve409() throws Exception {
        when(usuarioService.registrar(any(UsuarioRequest.class)))
                .thenThrow(new ReglaNegocioException("El correo ya está registrado"));

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
    }

    @Test
    void buscarDevuelve200() throws Exception {
        when(usuarioService.buscarPorId(7L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/usuarios/7").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.cl"));
    }

    @Test
    void buscarInexistenteDevuelve404() throws Exception {
        when(usuarioService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("Usuario", 99L));

        mockMvc.perform(get("/api/usuarios/99").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarConIdNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/usuarios/abc").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'id' tiene un formato inválido"));
    }

    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        mockMvc.perform(delete("/api/usuarios").with(admin()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void postSinContentTypeJsonDevuelve415() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.mensaje").value("El tipo de contenido no está soportado; usa application/json"));
    }

    @Test
    void buscarSinTokenDevuelve401ConJson() throws Exception {
        mockMvc.perform(get("/api/usuarios/7"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
        verify(usuarioService, never()).buscarPorId(any());
    }

    @Test
    void buscarComoClienteDevuelve403() throws Exception {
        mockMvc.perform(get("/api/usuarios/7").with(user("ana@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permiso para esta acción"));
        verify(usuarioService, never()).buscarPorId(any());
    }

    @Test
    void miPerfilDevuelveElUsuarioAutenticado() throws Exception {
        when(usuarioService.buscarPorCorreo("ana@correo.cl")).thenReturn(respuesta());

        mockMvc.perform(get("/api/usuarios/me").with(user("ana@correo.cl").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.cl"));
    }

    @Test
    void tokenBearerValidoAutenticaAtravesDelFiltro() throws Exception {
        when(jwtService.esValido("token-bueno")).thenReturn(true);
        when(jwtService.obtenerCorreo("token-bueno")).thenReturn("ana@correo.cl");
        when(userDetailsService.loadUserByUsername("ana@correo.cl")).thenReturn(
                User.builder().username("ana@correo.cl").password("hash").roles("CLIENTE").build());
        when(usuarioService.buscarPorCorreo("ana@correo.cl")).thenReturn(respuesta());

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.cl"));
    }

    @Test
    void tokenBearerInvalidoDevuelve401() throws Exception {
        when(jwtService.esValido("token-malo")).thenReturn(false);

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer token-malo"))
                .andExpect(status().isUnauthorized());
    }
}
