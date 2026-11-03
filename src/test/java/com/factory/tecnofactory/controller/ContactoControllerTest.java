package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.ContactoRequest;
import com.factory.tecnofactory.dto.ContactoResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.ContactoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactoController.class)
@Import(SecurityConfig.class)
class ContactoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactoService contactoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String JSON_VALIDO = """
            {"nombre": "Ana", "correo": "ana@correo.cl", "comentario": "Hola, quiero más información"}
            """;

    private static ContactoResponse respuesta() {
        return new ContactoResponse(1L, "Ana", "ana@correo.cl", "Hola, quiero más información");
    }

    @Test
    void crearSinSesionDevuelve201ConLocation() throws Exception {
        when(contactoService.crear(any(ContactoRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/contactos")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/contactos/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.correo").value("ana@correo.cl"));
    }

    @Test
    void crearConCorreoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/contactos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana\", \"correo\": \"no-es-correo\", \"comentario\": \"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correo").exists());

        verify(contactoService, never()).crear(any());
    }

    @Test
    void crearSinComentarioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/contactos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana\", \"correo\": \"ana@correo.cl\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.comentario").exists());

        verify(contactoService, never()).crear(any());
    }

    @Test
    void crearConComentarioDemasiadoLargoDevuelve400() throws Exception {
        String largo = "x".repeat(501);
        mockMvc.perform(post("/api/contactos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana\", \"correo\": \"ana@correo.cl\", \"comentario\": \"" + largo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.comentario").exists());

        verify(contactoService, never()).crear(any());
    }

    @Test
    void listarComoAdministradorDevuelve200() throws Exception {
        when(contactoService.listar(any()))
                .thenReturn(new PageResponse<>(List.of(respuesta()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/contactos").with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listarSinSesionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/contactos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarComoClienteDevuelve403() throws Exception {
        mockMvc.perform(get("/api/contactos").with(user("cliente@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarComoAdministradorDevuelve200() throws Exception {
        when(contactoService.buscarPorId(1L)).thenReturn(respuesta());

        mockMvc.perform(get("/api/contactos/1").with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentario").value("Hola, quiero más información"));
    }

    @Test
    void buscarInexistenteDevuelve404() throws Exception {
        when(contactoService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("Contacto", 99L));

        mockMvc.perform(get("/api/contactos/99").with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarComoVendedorDevuelve403() throws Exception {
        mockMvc.perform(get("/api/contactos/1").with(user("vendedor@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }
}
