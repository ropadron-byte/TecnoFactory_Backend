package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.BlogService;
import com.factory.tecnofactory.service.ImagenService;
import com.factory.tecnofactory.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reglas de acceso y códigos HTTP de los endpoints de gestión de blogs, imágenes y vendedores. */
@WebMvcTest({BlogController.class, ImagenController.class, UsuarioController.class})
@Import(SecurityConfig.class)
class GestionSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlogService blogService;
    @MockitoBean
    private ImagenService imagenService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String JSON_VENDEDOR = """
            {
              "run": 12345678, "runDv": "5", "nombre": "Luis", "apellidos": "Soto",
              "correo": "luis@correo.cl", "clave": "Clave12345",
              "fechaNacimiento": "1990-05-20", "direccion": "Calle 1",
              "telefono": "+56912345678", "idComuna": 13101
            }
            """;

    @Test
    void adminCreaVendedorYRecibe201() throws Exception {
        when(usuarioService.crearVendedor(any(UsuarioRequest.class))).thenReturn(new UsuarioResponse(
                8L, 12345678, "5", "Luis", "Soto", "luis@correo.cl", LocalDate.of(1990, 5, 20),
                "Calle 1", "+56912345678", true, "VENDEDOR", "Santiago", "Metropolitana"));

        mockMvc.perform(post("/api/usuarios/vendedores").with(user("admin@correo.cl").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VENDEDOR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/usuarios/8"))
                .andExpect(jsonPath("$.tipoUsuario").value("VENDEDOR"))
                .andExpect(jsonPath("$.clave").doesNotExist());
    }

    @Test
    void vendedorNoPuedeCrearVendedores() throws Exception {
        mockMvc.perform(post("/api/usuarios/vendedores").with(user("ana@correo.cl").roles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VENDEDOR))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonimoNoPuedeCrearVendedores() throws Exception {
        mockMvc.perform(post("/api/usuarios/vendedores")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VENDEDOR))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorBorraSuBlogYRecibe204() throws Exception {
        mockMvc.perform(delete("/api/blogs/1").with(user("ana@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isNoContent());
        verify(blogService).eliminar(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    void clienteNoPuedeBorrarNiDesactivarBlogs() throws Exception {
        mockMvc.perform(delete("/api/blogs/1").with(user("c@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/blogs/1/desactivar").with(user("c@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonimoNoPuedeBorrarBlogs() throws Exception {
        mockMvc.perform(delete("/api/blogs/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void vendedorDesactivaYActivaBlogs() throws Exception {
        mockMvc.perform(patch("/api/blogs/1/desactivar").with(user("ana@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/blogs/1/activar").with(user("ana@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarBlogValidaElCuerpo() throws Exception {
        mockMvc.perform(put("/api/blogs/1").with(user("ana@correo.cl").roles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": \"\", \"contenido\": \"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gestionDeBlogsExigeSesionDeVendedorOAdmin() throws Exception {
        mockMvc.perform(get("/api/blogs/gestion")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/blogs/gestion").with(user("c@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendedorBorraUnaImagenLibreYRecibe204() throws Exception {
        mockMvc.perform(delete("/api/imagenes/7").with(user("ana@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isNoContent());
        verify(imagenService).eliminar(7L);
    }

    @Test
    void desacoplarExigeSesion() throws Exception {
        mockMvc.perform(delete("/api/imagenes/7/duenos")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/imagenes/7/duenos/BLOG/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void tipoDeDuenoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(delete("/api/imagenes/7/duenos/MARCIANO/1").with(user("ana@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isBadRequest());
    }
}
