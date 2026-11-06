package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.BlogPublicoResponse;
import com.factory.tecnofactory.dto.BlogRequest;
import com.factory.tecnofactory.dto.BlogResponse;
import com.factory.tecnofactory.dto.BlogResumenResponse;
import com.factory.tecnofactory.dto.PageResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository blogRepository;
    @Mock
    private ImagenUrlRepository imagenUrlRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private BlogService service;

    @BeforeEach
    void crearService() {
        service = new BlogService(blogRepository, imagenUrlRepository, usuarioRepository,
                new BlogMapperImpl());
    }

    private static Usuario autor() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@correo.cl");
        return usuario;
    }

    private static Authentication sesion(String correo, String rol) {
        return new UsernamePasswordAuthenticationToken(correo, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private static BlogRequest request(Long idImagenPrincipal) {
        return new BlogRequest("Mi blog", "Contenido del blog", null, idImagenPrincipal);
    }

    private static ImagenUrl imagen(Long id, String url) {
        ImagenUrl imagen = new ImagenUrl();
        imagen.setId(id);
        imagen.setUrl(url);
        return imagen;
    }

    private static Blog blogVisible() {
        Blog blog = new Blog();
        blog.setId(1L);
        blog.setTitulo("Mi blog");
        blog.setContenido("Contenido");
        blog.setVisible(true);
        blog.setFechaCreacion(LocalDateTime.now());
        blog.setUsuario(autor());
        return blog;
    }

    @Test
    void creaBlogVisibleConFechaYAutor() {
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(autor()));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> {
            Blog guardado = inv.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });

        BlogResponse respuesta = service.crear(request(null), "ana@correo.cl");

        assertEquals(1L, respuesta.id());
        assertTrue(respuesta.visible());
        assertNotNull(respuesta.fechaCreacion());
        assertEquals(3L, respuesta.autorId());
        assertEquals("Ana", respuesta.autorNombre());
        assertEquals("ana@correo.cl", respuesta.autorCorreo());
        assertNull(respuesta.imagenPrincipal());
        assertTrue(respuesta.imagenes().isEmpty());
    }

    @Test
    void creaBlogConPortadaYLaAgregaALaGaleria() {
        ImagenUrl portada = imagen(7L, "/uploads/blog/a.jpg");
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(autor()));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(portada));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> {
            Blog guardado = inv.getArgument(0);
            guardado.setId(1L);
            return guardado;
        });

        BlogResponse respuesta = service.crear(request(7L), "ana@correo.cl");

        assertEquals(7L, respuesta.imagenPrincipal().id());
        assertEquals("/uploads/blog/a.jpg", respuesta.imagenPrincipal().url());
        assertEquals(1, respuesta.imagenes().size());
        assertEquals(7L, respuesta.imagenes().get(0).id());
    }

    @Test
    void crearConUsuarioInexistenteDevuelve404() {
        when(usuarioRepository.findByCorreo("nadie@correo.cl")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear(request(null), "nadie@correo.cl"));
    }

    @Test
    void crearConImagenInexistenteDevuelve404() {
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(autor()));
        when(imagenUrlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear(request(99L), "ana@correo.cl"));
    }

    @Test
    void blogInvisibleNoSeMuestraAlPublico() {
        Blog blog = blogVisible();
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.buscarPorId(1L, null));
    }

    @Test
    void administradorPuedeVerBlogInvisible() {
        Blog blog = blogVisible();
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        BlogPublicoResponse respuesta = service.buscarPorId(1L, sesion("admin@correo.cl", "ADMINISTRADOR"));

        assertEquals(1L, respuesta.id());
    }

    @Test
    void vendedorPuedeVerUnBlogInvisible() {
        Blog blog = blogVisible();
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        BlogPublicoResponse respuesta = service.buscarPorId(1L, sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals(1L, respuesta.id());
    }

    @Test
    void clienteNoVeUnBlogInvisible() {
        Blog blog = blogVisible();
        blog.setVisible(false);
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.buscarPorId(1L, sesion("cliente@correo.cl", "CLIENTE")));
    }

    @Test
    void vendedorPuedeCambiarLaPortada() {
        Blog blog = blogVisible();
        ImagenUrl portada = imagen(7L, "/uploads/blog/a.jpg");
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(portada));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogResponse respuesta = service.asignarImagenPrincipal(1L, 7L,
                sesion("ana@correo.cl", "VENDEDOR"));

        assertEquals(7L, respuesta.imagenPrincipal().id());
        assertEquals(1, respuesta.imagenes().size());
    }

    @Test
    void clienteNoPuedeCambiarLaPortada() {
        assertThrows(AccessDeniedException.class, () -> service.asignarImagenPrincipal(1L, 7L,
                sesion("cliente@correo.cl", "CLIENTE")));
    }

    @Test
    void cualquierVendedorPuedeCambiarLaPortadaDeBlogsAjenos() {
        Blog blog = blogVisible();
        ImagenUrl portada = imagen(7L, "/uploads/blog/a.jpg");
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blog));
        when(imagenUrlRepository.findById(7L)).thenReturn(Optional.of(portada));
        when(blogRepository.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogResponse respuesta = service.asignarImagenPrincipal(1L, 7L,
                sesion("otro@correo.cl", "VENDEDOR"));

        assertEquals(7L, respuesta.imagenPrincipal().id());
    }

    @Test
    void listarDevuelveSoloBlogsVisibles() {
        when(blogRepository.findByVisibleTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blogVisible())));

        PageResponse<BlogResumenResponse> respuesta = service.listar(PageRequest.of(0, 20));

        assertEquals(1, respuesta.contenido().size());
        assertEquals("Mi blog", respuesta.contenido().get(0).titulo());
    }

    @Test
    void buscarGestionDevuelveDatosDelAutor() {
        when(blogRepository.findById(1L)).thenReturn(Optional.of(blogVisible()));

        BlogResponse respuesta = service.buscarGestionPorId(1L, sesion("admin@correo.cl", "ADMINISTRADOR"));

        assertEquals(3L, respuesta.autorId());
        assertEquals("Ana", respuesta.autorNombre());
        assertEquals("ana@correo.cl", respuesta.autorCorreo());
    }

    @Test
    void clienteNoPuedeUsarElDetalleDeGestion() {
        assertThrows(AccessDeniedException.class, () -> service.buscarGestionPorId(1L,
                sesion("cliente@correo.cl", "CLIENTE")));
    }
}
