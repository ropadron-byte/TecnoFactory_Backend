package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.DuenoResponse;
import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.ImagenResumen;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.Blog;
import com.factory.tecnofactory.entity.Categoria;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.entity.Producto;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.repository.BlogRepository;
import com.factory.tecnofactory.repository.ImagenUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenServiceTest {

    @Mock
    private ImagenUrlRepository imagenUrlRepository;
    @Mock
    private BlogRepository blogRepository;
    @Mock
    private AlmacenamientoService almacenamiento;

    private ImagenService service;

    @BeforeEach
    void crearService() {
        service = new ImagenService(imagenUrlRepository, blogRepository, almacenamiento);
    }

    private static Authentication sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private static Usuario autor() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@correo.cl");
        return usuario;
    }

    private static Blog blog(Long id) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setTitulo("Blog " + id);
        blog.setVisible(true);
        blog.setFechaCreacion(LocalDateTime.now());
        blog.setUsuario(autor());
        return blog;
    }

    private static ImagenUrl imagen(Long id, String url) {
        ImagenUrl imagen = new ImagenUrl();
        imagen.setId(id);
        imagen.setUrl(url);
        return imagen;
    }

    private static Producto producto(Long id) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Producto " + id);
        return producto;
    }

    private static Categoria categoria(Long id) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre("Categoría " + id);
        return categoria;
    }

    @Test
    void subirRegistraLaImagenSinDueno() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", new byte[]{1});
        when(almacenamiento.guardar(any(MultipartFile.class), eq("general"))).thenReturn("/uploads/general/x.png");
        when(imagenUrlRepository.saveAndFlush(any(ImagenUrl.class))).thenAnswer(inv -> {
            ImagenUrl guardada = inv.getArgument(0);
            guardada.setId(9L);
            return guardada;
        });

        ImagenResponse respuesta = service.subir(archivo, "general");

        assertEquals(9L, respuesta.id());
        assertEquals("/uploads/general/x.png", respuesta.url());
        assertTrue(respuesta.duenos().isEmpty());
    }

    @Test
    void subirBorraElArchivoSiLaBaseFalla() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", new byte[]{1});
        when(almacenamiento.guardar(any(MultipartFile.class), eq("productos"))).thenReturn("/uploads/productos/x.png");
        when(imagenUrlRepository.saveAndFlush(any(ImagenUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicada"));

        assertThrows(DataIntegrityViolationException.class, () -> service.subir(archivo, "productos"));

        verify(almacenamiento).eliminar("/uploads/productos/x.png");
    }

    @Test
    void buscarPorIdDevuelveTodosLosDuenos() {
        ImagenUrl imagen = imagen(7L, "/uploads/blog/a.jpg");
        imagen.getBlogs().add(blog(1L));
        imagen.getBlogs().add(blog(4L));
        imagen.getProductos().add(producto(12L));
        imagen.getCategorias().add(categoria(3L));

        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(imagen));

        ImagenResponse respuesta = service.buscarPorId(7L);

        assertEquals(7L, respuesta.id());
        assertEquals("/uploads/blog/a.jpg", respuesta.url());
        assertEquals(List.of(
                new DuenoResponse("BLOG", 1L),
                new DuenoResponse("BLOG", 4L),
                new DuenoResponse("CATEGORIA", 3L),
                new DuenoResponse("PRODUCTO", 12L)), respuesta.duenos());
    }

    @Test
    void buscarPorIdInexistenteDevuelve404() {
        when(imagenUrlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    @Test
    void listarDevuelvePaginaDeImagenes() {
        when(imagenUrlRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(imagen(7L, "/uploads/blog/a.jpg"))));

        PageResponse<ImagenResponse> respuesta = service.listar(PageRequest.of(0, 20));

        assertEquals(1, respuesta.contenido().size());
        assertEquals(7L, respuesta.contenido().get(0).id());
    }

    @Test
    void listarPorBlogOrdenaPorId() {
        Blog blog = blog(1L);
        blog.getImagenes().add(imagen(7L, "/uploads/blog/a.jpg"));
        blog.getImagenes().add(imagen(2L, "/uploads/blog/b.jpg"));
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        List<ImagenResumen> imagenes = service.listarPorBlog(1L, null);

        assertEquals(List.of(2L, 7L), imagenes.stream().map(ImagenResumen::id).toList());
    }

    @Test
    void blogInvisibleNoExponeSusImagenesAlPublico() {
        Blog blog = blog(1L);
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        assertThrows(RecursoNoEncontradoException.class, () -> service.listarPorBlog(1L, null));
    }

    @Test
    void obtenerPortadaDevuelveNullCuandoNoTiene() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L)));

        assertNull(service.obtenerPortada(1L, null));
    }

    @Test
    void obtenerPortadaDevuelveLaImagenPrincipal() {
        Blog blog = blog(1L);
        blog.setImagenPrincipal(imagen(7L, "/uploads/blog/a.jpg"));
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        ImagenResumen portada = service.obtenerPortada(1L, null);

        assertEquals(7L, portada.id());
        assertEquals("/uploads/blog/a.jpg", portada.url());
    }

    @Test
    void subirImagenLaGuardaYLaAgregaALaGaleria() {
        Blog blog = blog(1L);
        MultipartFile archivo = new MockMultipartFile("archivo", "a.jpg", "image/jpeg", new byte[]{1});

        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(almacenamiento.guardar(any(MultipartFile.class), eq("blog")))
                .thenReturn("/uploads/blog/a.jpg");
        when(imagenUrlRepository.saveAndFlush(any(ImagenUrl.class))).thenAnswer(inv -> {
            ImagenUrl imagen = inv.getArgument(0);
            imagen.setId(7L);
            return imagen;
        });
        when(blogRepository.saveAndFlush(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        ImagenResponse respuesta = service.subirYAgregarABlog(1L, archivo,
                sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals(7L, respuesta.id());
        assertEquals("/uploads/blog/a.jpg", respuesta.url());
        assertEquals(List.of(new DuenoResponse("BLOG", 1L)), respuesta.duenos());
        assertTrue(blog.getImagenes().stream().anyMatch(i -> i.getId().equals(7L)));
    }

    @Test
    void siFallaLaBaseSeBorraElArchivoRecienSubido() {
        Blog blog = blog(1L);
        MultipartFile archivo = new MockMultipartFile("archivo", "a.jpg", "image/jpeg", new byte[]{1});

        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(almacenamiento.guardar(any(MultipartFile.class), eq("blog")))
                .thenReturn("/uploads/blog/a.jpg");
        when(imagenUrlRepository.saveAndFlush(any(ImagenUrl.class)))
                .thenThrow(new DataIntegrityViolationException("boom"));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.subirYAgregarABlog(1L, archivo, sesion("ana@correo.cl", "VENDEDOR")));

        verify(almacenamiento).eliminar("/uploads/blog/a.jpg");
    }

    @Test
    void agregarImagenExistenteLaVinculaAlBlog() {
        Blog blog = blog(1L);
        ImagenUrl imagen = imagen(7L, "/uploads/blog/a.jpg");

        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(imagen));
        when(blogRepository.saveAndFlush(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        ImagenResponse respuesta = service.agregarExistenteABlog(1L, 7L,
                sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals(7L, respuesta.id());
        assertEquals(List.of(new DuenoResponse("BLOG", 1L)), respuesta.duenos());
    }

    @Test
    void agregarImagenDuplicadaDevuelve409() {
        Blog blog = blog(1L);
        ImagenUrl imagen = imagen(7L, "/uploads/blog/a.jpg");
        blog.getImagenes().add(imagen);

        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(imagen));

        assertThrows(ReglaNegocioException.class, () -> service.agregarExistenteABlog(1L, 7L,
                sesion("ana@correo.cl", "VENDEDOR")));
    }

    @Test
    void clienteNoPuedeAgregarImagenes() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L)));

        assertThrows(AccessDeniedException.class, () -> service.agregarExistenteABlog(1L, 7L,
                sesion("cliente@correo.cl", "CLIENTE")));
    }

    @Test
    void blogInexistenteDevuelve404() {
        when(blogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.agregarExistenteABlog(99L, 7L,
                sesion("ana@correo.cl", "VENDEDOR")));
    }

    @Test
    void imagenInexistenteDevuelve404() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L)));
        when(imagenUrlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.agregarExistenteABlog(1L, 99L,
                sesion("ana@correo.cl", "VENDEDOR")));
    }
}
