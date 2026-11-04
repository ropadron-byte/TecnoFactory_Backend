package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.BlogRequest;
import com.factory.tecnofactory.dto.BlogResponse;
import com.factory.tecnofactory.entity.Blog;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.mapper.BlogMapperImpl;
import com.factory.tecnofactory.repository.BlogRepository;
import com.factory.tecnofactory.repository.ImagenUrlRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceGestionTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private ImagenUrlRepository imagenUrlRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private BlogService service;

    @BeforeEach
    void crearService() {
        service = new BlogService(blogRepository, imagenUrlRepository, usuarioRepository, new BlogMapperImpl());
    }

    private static Authentication sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private static Blog blog() {
        Usuario autor = new Usuario();
        autor.setId(3L);
        autor.setNombre("Ana");
        autor.setCorreo("ana@correo.cl");
        Blog blog = new Blog();
        blog.setId(1L);
        blog.setTitulo("Mi blog");
        blog.setContenido("Contenido");
        blog.setVisible(true);
        blog.setFechaCreacion(LocalDateTime.now());
        blog.setUsuario(autor);
        return blog;
    }

    private static ImagenUrl imagen(Long id) {
        ImagenUrl imagen = new ImagenUrl();
        imagen.setId(id);
        imagen.setUrl("/uploads/blog/" + id + ".jpg");
        return imagen;
    }

    @Test
    void eliminarQuitaLaAsociacionConLasImagenesPeroNoLasBorra() {
        Blog blog = blog();
        ImagenUrl img = imagen(7L);
        blog.getImagenes().add(img);
        blog.setImagenPrincipal(img);
        img.getBlogs().add(blog);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        service.eliminar(1L, sesion("ana@correo.cl", "VENDEDOR"));

        assertTrue(blog.getImagenes().isEmpty());
        assertTrue(img.getBlogs().isEmpty());
        verify(blogRepository).delete(blog);
        verify(imagenUrlRepository, never()).delete(any());
    }

    @Test
    void cualquierVendedorPuedeEliminarUnBlogAjeno() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog()));

        service.eliminar(1L, sesion("otro@correo.cl", "VENDEDOR"));

        verify(blogRepository).delete(any(Blog.class));
    }

    @Test
    void clienteNoPuedeEliminar() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog()));

        assertThrows(AccessDeniedException.class,
                () -> service.eliminar(1L, sesion("cliente@correo.cl", "CLIENTE")));
        verify(blogRepository, never()).delete(any());
    }

    @Test
    void administradorPuedeEliminarBlogAjeno() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog()));

        service.eliminar(1L, sesion("admin@correo.cl", "ADMINISTRADOR"));

        verify(blogRepository).delete(any(Blog.class));
    }

    @Test
    void eliminarInexistenteDevuelve404() {
        when(blogRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.eliminar(9L, sesion("ana@correo.cl", "VENDEDOR")));
    }

    @Test
    void desactivarOcultaElBlog() {
        Blog blog = blog();
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogResponse respuesta = service.desactivar(1L, sesion("ana@correo.cl", "VENDEDOR"));

        assertFalse(respuesta.visible());
    }

    @Test
    void activarVuelveAPublicarElBlog() {
        Blog blog = blog();
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.activar(1L, sesion("ana@correo.cl", "VENDEDOR")).visible());
    }

    @Test
    void cualquierVendedorPuedeDesactivarUnBlogAjeno() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog()));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.desactivar(1L, sesion("otro@correo.cl", "VENDEDOR")).visible());
    }

    @Test
    void actualizarCambiaTituloYContenidoYConservaVisibilidad() {
        Blog blog = blog();
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogResponse respuesta = service.actualizar(1L,
                new BlogRequest("Nuevo", "Texto nuevo", null, null), sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals("Nuevo", respuesta.titulo());
        assertEquals("Texto nuevo", respuesta.contenido());
        assertFalse(respuesta.visible());
    }

    @Test
    void actualizarConPortadaLaAgregaALaGaleria() {
        Blog blog = blog();
        ImagenUrl portada = imagen(7L);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(portada));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogResponse respuesta = service.actualizar(1L,
                new BlogRequest("Mi blog", "Contenido", true, 7L), sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals(7L, respuesta.imagenPrincipal().id());
        assertEquals(1, respuesta.imagenes().size());
    }

    @Test
    void clienteNoPuedeActualizar() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog()));

        assertThrows(AccessDeniedException.class, () -> service.actualizar(1L,
                new BlogRequest("x", "y", null, null), sesion("cliente@correo.cl", "CLIENTE")));
    }

    @Test
    void elListadoDeGestionDevuelveTodosLosBlogsIncluidosLosDesactivados() {
        Blog oculto = blog();
        oculto.setId(2L);
        oculto.setVisible(false);
        when(blogRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog(), oculto)));

        var pagina = service.listarGestion(PageRequest.of(0, 20));

        assertEquals(2, pagina.contenido().size());
        assertTrue(pagina.contenido().get(0).visible());
        assertFalse(pagina.contenido().get(1).visible());
    }
}
