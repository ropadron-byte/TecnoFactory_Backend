package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.BlogPublicoResponse;
import com.factory.tecnofactory.dto.BlogRequest;
import com.factory.tecnofactory.dto.BlogResponse;
import com.factory.tecnofactory.dto.BlogResumenResponse;
import com.factory.tecnofactory.dto.ImagenResumen;
import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.BlogService;
import com.factory.tecnofactory.service.ImagenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlogController.class)
@Import(SecurityConfig.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlogService blogService;

    @MockitoBean
    private ImagenService imagenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final String JSON_VALIDO = """
            {"titulo": "Mi blog", "contenido": "Contenido del blog", "idImagenPrincipal": 7}
            """;

    private static RequestPostProcessor admin() {
        return user("admin@correo.cl").roles("ADMINISTRADOR");
    }

    private static RequestPostProcessor vendedor() {
        return user("ana@correo.cl").roles("VENDEDOR");
    }

    private static ImagenResumen imagenResumen() {
        return new ImagenResumen(7L, "/uploads/blog/a.jpg");
    }

    private static BlogResponse respuestaBlog() {
        return new BlogResponse(1L, "Mi blog", "Contenido del blog", true,
                LocalDateTime.of(2026, 11, 3, 12, 0), 3L, "Ana", "ana@correo.cl",
                imagenResumen(), List.of(imagenResumen()));
    }

    private static BlogPublicoResponse respuestaBlogPublico() {
        return new BlogPublicoResponse(1L, "Mi blog", "Contenido del blog",
                LocalDateTime.of(2026, 11, 3, 12, 0), imagenResumen(), List.of(imagenResumen()));
    }

    private static ImagenResponse respuestaImagen() {
        return new ImagenResponse(7L, "/uploads/blog/a.jpg",
                List.of(new com.factory.tecnofactory.dto.DuenoResponse("BLOG", 1L)));
    }

    @Test
    void crearDevuelve201ConLocation() throws Exception {
        when(blogService.crear(any(BlogRequest.class), eq("admin@correo.cl")))
                .thenReturn(respuestaBlog());

        mockMvc.perform(post("/api/blogs").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/blogs/1"))
                .andExpect(jsonPath("$.titulo").value("Mi blog"))
                .andExpect(jsonPath("$.autorCorreo").value("ana@correo.cl"))
                .andExpect(jsonPath("$.imagenPrincipal.id").value(7))
                .andExpect(jsonPath("$.imagenes[0].url").value("/uploads/blog/a.jpg"));
    }

    @Test
    void crearComoClienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/blogs")
                        .with(user("cliente@correo.cl").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden());

        verify(blogService, never()).crear(any(), any());
    }

    @Test
    void crearSinTituloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/blogs").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\": \"Contenido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.titulo").exists());

        verify(blogService, never()).crear(any(), any());
    }

    @Test
    void listarEsPublico() throws Exception {
        when(blogService.listar(any()))
                .thenReturn(new PageResponse<>(List.of(new BlogResumenResponse(1L, "Mi blog",
                        LocalDateTime.of(2026, 11, 3, 12, 0), imagenResumen())),
                        0, 20, 1, 1));

        mockMvc.perform(get("/api/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].titulo").value("Mi blog"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscarEsPublicoSinDatosDelAutor() throws Exception {
        when(blogService.buscarPorId(eq(1L), any())).thenReturn(respuestaBlogPublico());

        mockMvc.perform(get("/api/blogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.autorCorreo").doesNotExist())
                .andExpect(jsonPath("$.autorNombre").doesNotExist())
                .andExpect(jsonPath("$.autorId").doesNotExist());
    }

    @Test
    void buscarGestionDevuelveDatosDelAutorParaPersonal() throws Exception {
        when(blogService.buscarGestionPorId(eq(1L), any())).thenReturn(respuestaBlog());

        mockMvc.perform(get("/api/blogs/gestion/1").with(vendedor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorCorreo").value("ana@correo.cl"))
                .andExpect(jsonPath("$.autorNombre").value("Ana"));
    }

    @Test
    void buscarGestionSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/blogs/gestion/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buscarInexistenteDevuelve404() throws Exception {
        when(blogService.buscarPorId(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Blog", 99L));

        mockMvc.perform(get("/api/blogs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void portadaDevuelveLaImagen() throws Exception {
        when(imagenService.obtenerPortada(eq(1L), any())).thenReturn(imagenResumen());

        mockMvc.perform(get("/api/blogs/1/imagen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/blog/a.jpg"));
    }

    @Test
    void portadaSinImagenDevuelve204() throws Exception {
        when(imagenService.obtenerPortada(eq(1L), any())).thenReturn(null);

        mockMvc.perform(get("/api/blogs/1/imagen"))
                .andExpect(status().isNoContent());
    }

    @Test
    void galeriaEsPublica() throws Exception {
        when(imagenService.listarPorBlog(eq(1L), any())).thenReturn(List.of(imagenResumen()));

        mockMvc.perform(get("/api/blogs/1/imagenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void subirImagenComoVendedorDevuelve201() throws Exception {
        when(imagenService.subirYAgregarABlog(eq(1L), any(MultipartFile.class), any()))
                .thenReturn(respuestaImagen());

        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.jpg",
                "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/blogs/1/imagenes").file(archivo).with(vendedor()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/blogs/1/imagenes/7"))
                .andExpect(jsonPath("$.url").value("/uploads/blog/a.jpg"));
    }

    @Test
    void subirImagenSinTokenDevuelve401() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.jpg",
                "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/blogs/1/imagenes").file(archivo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agregarImagenExistenteDevuelve201() throws Exception {
        when(imagenService.agregarExistenteABlog(eq(1L), eq(7L), any()))
                .thenReturn(respuestaImagen());

        mockMvc.perform(post("/api/blogs/1/imagenes/7").with(vendedor()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/blogs/1/imagenes"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void cambiarPortadaDevuelve200() throws Exception {
        when(blogService.asignarImagenPrincipal(eq(1L), eq(7L), any()))
                .thenReturn(respuestaBlog());

        mockMvc.perform(put("/api/blogs/1/imagen-principal/7").with(vendedor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenPrincipal.id").value(7));
    }
}
