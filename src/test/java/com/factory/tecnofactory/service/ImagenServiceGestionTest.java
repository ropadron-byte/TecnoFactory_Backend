package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.TipoDueno;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenServiceGestionTest {

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

    private static Blog blog(Long id, String correoAutor) {
        Usuario autor = new Usuario();
        autor.setCorreo(correoAutor);
        Blog blog = new Blog();
        blog.setId(id);
        blog.setUsuario(autor);
        return blog;
    }

    private static ImagenUrl imagen(Long id) {
        ImagenUrl imagen = new ImagenUrl();
        imagen.setId(id);
        imagen.setUrl("/uploads/blog/" + id + ".jpg");
        return imagen;
    }

    private static void relacionar(Blog blog, ImagenUrl imagen) {
        blog.getImagenes().add(imagen);
        imagen.getBlogs().add(blog);
    }

    private static Producto producto(Long id, ImagenUrl imagen) {
        Producto p = new Producto();
        p.setId(id);
        p.getImagenesProducto().add(imagen);
        imagen.getProductos().add(p);
        return p;
    }

    private static Categoria categoria(Long id, ImagenUrl imagen) {
        Categoria c = new Categoria();
        c.setId(id);
        c.getImagenesCategoria().add(imagen);
        imagen.getCategorias().add(c);
        return c;
    }

    // ---------- borrar ----------

    @Test
    void borraUnaImagenSinRelacionesYSuArchivo() {
        ImagenUrl img = imagen(7L);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        service.eliminar(7L);

        verify(imagenUrlRepository).delete(img);
        verify(almacenamiento).eliminar("/uploads/blog/7.jpg");
    }

    @Test
    void noBorraUnaImagenQueEstaEnUnBlog() {
        ImagenUrl img = imagen(7L);
        relacionar(blog(1L, "ana@correo.cl"), img);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(7L));
        verify(imagenUrlRepository, never()).delete(any());
        verify(almacenamiento, never()).eliminar(any());
    }

    @Test
    void noBorraUnaImagenQueEsPortadaDeUnBlog() {
        ImagenUrl img = imagen(7L);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));
        when(blogRepository.findByImagenPrincipalId(7L)).thenReturn(List.of(blog(1L, "ana@correo.cl")));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(7L));
    }

    @Test
    void noBorraUnaImagenDeProductoOCategoria() {
        ImagenUrl img = imagen(7L);
        producto(2L, img);
        categoria(3L, img);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        assertThrows(ReglaNegocioException.class, () -> service.eliminar(7L));
    }

    @Test
    void borrarInexistenteDevuelve404() {
        when(imagenUrlRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(9L));
    }

    // ---------- desacoplar de todos ----------

    @Test
    void desacoplarDeTodosLimpiaBlogsProductosYCategorias() {
        ImagenUrl img = imagen(7L);
        Blog b = blog(1L, "ana@correo.cl");
        relacionar(b, img);
        b.setImagenPrincipal(img);
        Producto p = producto(2L, img);
        Categoria c = categoria(3L, img);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));
        when(blogRepository.findByImagenPrincipalId(7L)).thenReturn(List.of(b));

        ImagenResponse respuesta = service.desacoplarDeTodos(7L, sesion("admin@correo.cl", "ADMINISTRADOR"));

        assertTrue(respuesta.duenos().isEmpty());
        assertTrue(b.getImagenes().isEmpty());
        assertNull(b.getImagenPrincipal());
        assertTrue(p.getImagenesProducto().isEmpty());
        assertTrue(c.getImagenesCategoria().isEmpty());
        verify(imagenUrlRepository, never()).delete(any());
    }

    @Test
    void vendedorDesacoplaDeTodosSiTodosLosBlogsSonSuyos() {
        ImagenUrl img = imagen(7L);
        relacionar(blog(1L, "ana@correo.cl"), img);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        ImagenResponse respuesta = service.desacoplarDeTodos(7L, sesion("ana@correo.cl", "VENDEDOR"));

        assertTrue(respuesta.duenos().isEmpty());
    }

    @Test
    void vendedorDesacoplaDeTodosAunqueLosDuenosNoSeanSuyos() {
        ImagenUrl img = imagen(7L);
        Blog ajeno = blog(1L, "otro@correo.cl");
        relacionar(ajeno, img);
        Producto p = producto(2L, img);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        ImagenResponse respuesta = service.desacoplarDeTodos(7L, sesion("ana@correo.cl", "VENDEDOR"));

        assertTrue(respuesta.duenos().isEmpty());
        assertTrue(ajeno.getImagenes().isEmpty());
        assertTrue(p.getImagenesProducto().isEmpty());
    }

    @Test
    void clienteNoPuedeDesacoplar() {
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(imagen(7L)));

        assertThrows(AccessDeniedException.class,
                () -> service.desacoplarDeTodos(7L, sesion("cliente@correo.cl", "CLIENTE")));
    }

    // ---------- desacoplar individual ----------

    @Test
    void desacoplaDeUnSoloBlogYDejaLosDemas() {
        ImagenUrl img = imagen(7L);
        Blog b1 = blog(1L, "ana@correo.cl");
        Blog b2 = blog(2L, "ana@correo.cl");
        relacionar(b1, img);
        relacionar(b2, img);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(b1));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        ImagenResponse respuesta = service.desacoplarDe(7L, TipoDueno.BLOG, 1L, sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals(1, respuesta.duenos().size());
        assertEquals(2L, respuesta.duenos().get(0).id());
        assertTrue(b1.getImagenes().isEmpty());
    }

    @Test
    void desacoplarLaPortadaDejaElBlogSinPortada() {
        ImagenUrl img = imagen(7L);
        Blog b = blog(1L, "ana@correo.cl");
        relacionar(b, img);
        b.setImagenPrincipal(img);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(b));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        service.desacoplarDeBlog(1L, 7L, sesion("ana@correo.cl", "VENDEDOR"));

        assertNull(b.getImagenPrincipal());
    }

    @Test
    void desacoplarImagenQueNoEstaEnElBlogDevuelve404() {
        Blog b = blog(1L, "ana@correo.cl");
        when(blogRepository.findById(1L)).thenReturn(Optional.of(b));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(imagen(7L)));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.desacoplarDeBlog(1L, 7L, sesion("ana@correo.cl", "VENDEDOR")));
    }

    @Test
    void clienteNoDesacoplaDeBlog() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog(1L, "ana@correo.cl")));

        assertThrows(AccessDeniedException.class,
                () -> service.desacoplarDeBlog(1L, 7L, sesion("cliente@correo.cl", "CLIENTE")));
    }

    @Test
    void vendedorDesacoplaDeProducto() {
        ImagenUrl img = imagen(7L);
        Producto p = producto(2L, img);
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));

        ImagenResponse respuesta = service.desacoplarDe(7L, TipoDueno.PRODUCTO, 2L,
                sesion("ana@correo.cl", "VENDEDOR"));
        assertTrue(respuesta.duenos().isEmpty());
        assertTrue(p.getImagenesProducto().isEmpty());
    }

    @Test
    void desacoplarDeCategoriaInexistenteDevuelve404() {
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(imagen(7L)));

        assertThrows(RecursoNoEncontradoException.class, () -> service.desacoplarDe(7L, TipoDueno.CATEGORIA, 3L,
                sesion("admin@correo.cl", "ADMINISTRADOR")));
    }

    // ---------- reemplazar ----------

    @Test
    void reemplazaElArchivoConservandoLaCarpetaYBorraElAnterior() {
        ImagenUrl img = imagen(7L);
        MockMultipartFile archivo = new MockMultipartFile("archivo", "n.png", "image/png", new byte[]{1});
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));
        when(almacenamiento.guardar(any(MultipartFile.class), eq("blog"))).thenReturn("/uploads/blog/nueva.png");

        ImagenResponse respuesta = service.reemplazar(7L, archivo, sesion("admin@correo.cl", "ADMINISTRADOR"));

        assertEquals("/uploads/blog/nueva.png", respuesta.url());
        verify(almacenamiento).eliminar("/uploads/blog/7.jpg");
    }

    @Test
    void siFallaLaBaseSeBorraElArchivoNuevo() {
        ImagenUrl img = imagen(7L);
        MockMultipartFile archivo = new MockMultipartFile("archivo", "n.png", "image/png", new byte[]{1});
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(img));
        when(almacenamiento.guardar(any(MultipartFile.class), eq("blog"))).thenReturn("/uploads/blog/nueva.png");
        when(imagenUrlRepository.saveAndFlush(any(ImagenUrl.class))).thenThrow(new IllegalStateException("fallo"));

        assertThrows(IllegalStateException.class,
                () -> service.reemplazar(7L, archivo, sesion("admin@correo.cl", "ADMINISTRADOR")));
        verify(almacenamiento).eliminar("/uploads/blog/nueva.png");
        verify(almacenamiento, never()).eliminar("/uploads/blog/7.jpg");
    }
}
