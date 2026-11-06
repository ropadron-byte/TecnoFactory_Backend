package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.DuenoResponse;
import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.exception.ArchivoNoValidoException;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.ImagenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImagenController.class)
@Import(SecurityConfig.class)
class ImagenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImagenService imagenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static ImagenResponse respuestaImagen() {
        return new ImagenResponse(7L, "/uploads/blog/a.jpg",
                List.of(new DuenoResponse("BLOG", 1L)));
    }

    @Test
    void listarExigePersonal() throws Exception {
        mockMvc.perform(get("/api/imagenes"))
                .andExpect(status().isUnauthorized());

        when(imagenService.listar(any()))
                .thenReturn(new PageResponse<>(List.of(respuestaImagen()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/imagenes").with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(7))
                .andExpect(jsonPath("$.contenido[0].duenos[0].tipo").value("BLOG"));
    }

    @Test
    void listarComoClienteDevuelve403() throws Exception {
        mockMvc.perform(get("/api/imagenes").with(user("cliente@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarPorIdExigePersonal() throws Exception {
        mockMvc.perform(get("/api/imagenes/7"))
                .andExpect(status().isUnauthorized());

        when(imagenService.buscarPorId(7L)).thenReturn(respuestaImagen());

        mockMvc.perform(get("/api/imagenes/7").with(user("vendedor@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/blog/a.jpg"))
                .andExpect(jsonPath("$.duenos[0].id").value(1));
    }

    @Test
    void buscarInexistenteDevuelve404() throws Exception {
        when(imagenService.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Imagen", 99L));

        mockMvc.perform(get("/api/imagenes/99").with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void subirProductoComoAdministradorDevuelve201ConId() throws Exception {
        when(imagenService.subir(any(MultipartFile.class), eq("productos")))
                .thenReturn(new ImagenResponse(7L, "/uploads/productos/a.jpg", List.of()));

        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.jpg",
                "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes/productos").file(archivo)
                        .with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.url").value("/uploads/productos/a.jpg"));
    }

    @Test
    void subirImagenSinDuenoComoVendedorDevuelve201() throws Exception {
        when(imagenService.subir(any(MultipartFile.class), eq("general")))
                .thenReturn(new ImagenResponse(8L, "/uploads/general/b.png", List.of()));

        MockMultipartFile archivo = new MockMultipartFile("archivo", "b.png",
                "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes").file(archivo)
                        .with(user("vendedor@correo.cl").roles("VENDEDOR")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/imagenes/8")))
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.duenos").isEmpty());
    }

    @Test
    void subirImagenSinTokenDevuelve401() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "b.png",
                "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes").file(archivo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void subirImagenComoClienteDevuelve403() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "b.png",
                "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes").file(archivo)
                        .with(user("cliente@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void subirProductoComoClienteDevuelve403() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.jpg",
                "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes/productos").file(archivo)
                        .with(user("cliente@correo.cl").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordenarPorUnCampoInexistenteDevuelve400() throws Exception {
        when(imagenService.listar(any()))
                .thenThrow(new PropertyReferenceException("string", TypeInformation.of(ImagenUrl.class), List.of()));

        mockMvc.perform(get("/api/imagenes").param("sort", "string")
                        .with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede ordenar por 'string'"));
    }

    @Test
    void subirSinElCampoArchivoDevuelve400() throws Exception {
        mockMvc.perform(multipart("/api/imagenes/productos")
                        .with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subirUnArchivoInvalidoDevuelve400() throws Exception {
        when(imagenService.subir(any(MultipartFile.class), eq("productos")))
                .thenThrow(new ArchivoNoValidoException("Solo se permiten imágenes JPG, PNG o WEBP"));

        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.gif", "image/gif", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes/productos").file(archivo)
                        .with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subirUnArchivoMuyGrandeDevuelve413() throws Exception {
        when(imagenService.subir(any(MultipartFile.class), eq("productos")))
                .thenThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/imagenes/productos").file(archivo)
                        .with(user("admin@correo.cl").roles("ADMINISTRADOR")))
                .andExpect(status().isPayloadTooLarge());
    }
}
